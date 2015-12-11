package n4c.pedt.test

import java.net.URLDecoder

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import n4c.pedt.util.Marshalling
import Marshalling._
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.{HttpService, Route}

import scala.concurrent.duration._
import scala.io.Source

object MockN4CService {
  private val log = LoggerFactory.getLogger(MockN4CService.getClass)

  trait ServerRoute extends HttpService {
    def route: Route = {
      path("ping") {
        get {
          complete("pong!")
        }
      } ~ path("download_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
        get {
          complete {
            val tasks = Map[String, String](
              "30d9f7c5c9eb1a52c41af0bd4e2d835b" -> Source.fromFile("pedt/src/test/resources/task1.json").mkString,
              "297bcc44afe6d4c42751d6682312e2e4" -> Source.fromFile("pedt/src/test/resources/task2.json").mkString,
              "50428e7e8797cbab92c39cefbf0c8f88" -> Source.fromFile("pedt/src/test/resources/task3.json").mkString,
              "b6dc1169078fc4da7913fa153e47e1a5" -> Source.fromFile("pedt/src/test/resources/task4.json").mkString,
              "132ecd77dc5a8ecfd36ce6bb149d208f" -> Source.fromFile("pedt/src/test/resources/task5.json").mkString,
              "bfef82a97bd7af9103ced97ca570c8e3" -> Source.fromFile("pedt/src/test/resources/task6.json").mkString)
            tasks.getOrElse(taskId, "task not found.").asInstanceOf[String]
          }
        }
      } ~ path("query:" ~ "(.+)".r) { scope =>
        get {
          complete {
            val scopes = Map[String, String]("n4c:/a/b/c/sink" -> """["http://127.0.0.1:8083/"]""",
              "n4c:/a/b/c/map" -> """["http://127.0.0.1:8083/", "http://127.0.0.1:8084/", "http://127.0.0.1:8085/"]""")
            scopes.getOrElse(URLDecoder.decode(scope, "UTF-8"), "scope not found.").asInstanceOf[String] // ToResponseMarshaller("scope not found.")
          }
        } ~ post {
            entity(as[String]) { json =>
              complete {
              log.info(json)
              ""
            }
          }
        }
      }
    }
  }

  class ServiceActor extends Actor with ServerRoute {
    implicit val system = context.system
    def actorRefFactory = context
    def receive = runRoute(route)
  }

  class HttpServer {
    def start() {
      implicit val system = ActorSystem("x")
      val service = system.actorOf(Props(classOf[ServiceActor]), "n4c-service")
      implicit val timeout = Timeout(5.seconds)
      IO(Http) ! Http.Bind(service, interface = "127.0.0.1", port = 8089)
    }
  }

}
