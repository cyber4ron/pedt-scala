package com.wandoujia.n4c.pedt

import java.net.{URLDecoder, URLEncoder}

import akka.actor.{Actor, Props}
import akka.io.IO
import akka.util.Timeout
import com.wandoujia.n4c.pedt.util.Marshalling._
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.json._
import spray.routing.{HttpService, Route}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source

object MockN4CService {
  private val log = LoggerFactory.getLogger(MockN4CService.getClass)

  class BiMultiMap[K, V](kvs: (K, V)*) {
    private val map = new mutable.HashMap[K, mutable.Set[V]] with mutable.MultiMap[K, V]
    private val inverseMap = new mutable.HashMap[V, mutable.Set[K]] with mutable.MultiMap[V, K]

    kvs foreach { kv =>
      map.addBinding(kv._1, kv._2)
      inverseMap.addBinding(kv._2, kv._1)
    }

    def get = map
    def inverse = inverseMap

    def +(kv: (K, V)) = {
      map.addBinding(kv._1, kv._2)
      inverseMap.addBinding(kv._2, kv._1)
      this
    }

    def -(key: K) = {
      map.get(key) foreach { set =>
        val copy = set.map(x => x)
        copy foreach { value =>
          map.removeBinding(key, value)
          inverseMap.removeBinding(value, key)
        }
      }
      this
    }
  }

  trait ServerRoute extends HttpService {
    var scopes = new BiMultiMap[String, String]("http://127.0.0.1:8083/" -> "n4c:/a/b/c/sink",
      "http://127.0.0.1:8083/" -> "n4c:/a/b/c/map",
      "http://127.0.0.1:8084/" -> "n4c:/a/b/c/map",
      "http://127.0.0.1:8085/" -> "n4c:/a/b/c/map") // resource <-> scope

    val subscribes = new BiMultiMap[String, String]() // scope <-> url

    var tasks = Map[String, String]("30d9f7c5c9eb1a52c41af0bd4e2d835b" -> Source.fromFile("pedt/src/test/resources/init_connection_count.json").mkString,
      "2543693a4c27f3c97e2c2f9cca4000ea" -> Source.fromFile("pedt/src/test/resources/get_current_connection_count.json").mkString,
      "3c4b2ad93a3b98279c95e33a18eb994b" -> Source.fromFile("pedt/src/test/resources/update_current_connection_count.json").mkString,
      "893d7569b4c01d8c8b6d3e053ebafb66" -> Source.fromFile("pedt/src/test/resources/word_count_map.json").mkString,
      "132ecd77dc5a8ecfd36ce6bb149d208f" -> Source.fromFile("pedt/src/test/resources/word_count_reduce.json").mkString,
      "d71634053f6eeaa6b21eb2fe82506af4" -> Source.fromFile("pedt/src/test/resources/call_from_js.json").mkString,
      "30bf98606c585eef4ba24537231df3fb" -> Source.fromFile("pedt/src/test/resources/test_task.json").mkString)

    def route: Route = {
      path("ping") {
        get {
          complete("pong!")
        }
      } ~ path("download_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
        get {
          complete {
            tasks.getOrElse(taskId, "task not found.").asInstanceOf[String]
          }
        }
      } ~ path("register_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
        post {
          entity(as[String]) { taskDef =>
            complete {
              tasks = tasks.updated(taskId, taskDef)
              """{"status": "ok"}"""
            }
          }
        }
      } ~ path("query:" ~ "(.+)".r) { scope =>
        get {
          complete {
            scopes.inverse.get(URLDecoder.decode(scope, "UTF-8")).map(set => s"""["${set.mkString("""", """")}"]""").getOrElse[String]("scope not found.")
          } // ToResponseMarshaller(resultString)
        } ~ post {
          entity(as[String]) { json =>
            complete {
              log.info(json)
              "to be done."
            }
          }
        }
      } ~ path("subscribe:" ~ "(.+)".r) { scope =>
        post {
          entity(as[String]) { json =>
            log.info(json)
            val jsonObj = json.parseJson.asJsObject
            complete {
              subscribes + (scope -> jsonObj.fields.get("receive").get.asInstanceOf[JsString].value)
              """{"status": "ok"}"""
            }
          }
        }
      } ~ path("lost_worker:" ~ "^(.+)$".r) { host_port => // e.g. 127.0.0.1%3A8084
        get {
          complete {
            // 没有保证数据一致性，但用于测试没有问题
            val affectedScopes = scopes.get(s"""http://${URLDecoder.decode(host_port, "UTF-8")}/""").map(x => x)
            scopes - s"""http://$host_port/"""
            import com.wandoujia.n4c.pedt.context.HttpContext._
            affectedScopes.foreach(scope => subscribes.get.get(scope).get.foreach(notifyUrl => request(s"$notifyUrl${URLEncoder.encode(scope, "UTF-8")}")))
            """{"status": "ok"}"""
          }
        }
      } ~ path("add_worker:" ~ "^(.+)$".r) { host_port =>
        complete {
          "to be done"
        }
      }
    }
  }

  class ServiceActor extends Actor with ServerRoute {
    implicit val system = context.system
    def actorRefFactory = context
    def receive = runRoute(route)
  }

  object HttpServer {
    private val log = LoggerFactory.getLogger(HttpServer.getClass)
  }

  class HttpServer {
    import com.wandoujia.n4c.pedt.context.HttpContext.system
    implicit val timeout = Timeout(5.seconds)

    def start() {
      val service = system.actorOf(Props(classOf[ServiceActor]), "n4c-service")
      IO(Http) ! Http.Bind(service, interface = "127.0.0.1", port = 8089)
      HttpServer.log.info(s"http server[127.0.0.1:8089] started")
    }

    def stop() {
      IO(Http) ! Http.Unbind
    }
  }

}
