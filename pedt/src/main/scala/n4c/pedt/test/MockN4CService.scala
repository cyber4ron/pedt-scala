package n4c.pedt.test

import java.net.URLDecoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import n4c.pedt.http.MarshallingSupport

import scala.io.Source

object MockN4CService extends App {
  class ServerRoute extends Directives {
    def route: Route = {
      path("ping") {
        get {
          complete("pong!")
        }
      } ~ path("download_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
        get {
          complete {
            import MarshallingSupport._
            val tasks = Map[String, String](
              "30d9f7c5c9eb1a52c41af0bd4e2d835b" -> Source.fromFile("pedt/src/test/resources/task1.json").mkString,
              "297bcc44afe6d4c42751d6682312e2e4" -> Source.fromFile("pedt/src/test/resources/task2.json").mkString,
              "50428e7e8797cbab92c39cefbf0c8f88" -> Source.fromFile("pedt/src/test/resources/task3.json").mkString,
              "b6dc1169078fc4da7913fa153e47e1a5" -> Source.fromFile("pedt/src/test/resources/task4.json").mkString,
              "132ecd77dc5a8ecfd36ce6bb149d208f" -> Source.fromFile("pedt/src/test/resources/task5.json").mkString,
              "bfef82a97bd7af9103ced97ca570c8e3" -> Source.fromFile("pedt/src/test/resources/task6.json").mkString)
            Some(tasks.getOrElse(taskId, "task not found."))
          }
        }
      } ~ path("query") {
        get {
          import MarshallingSupport._
          parameter('scope.as[String])((scope: String) => {
            complete {
              val scopes = Map[String, String]("n4c:/a/b/c/sink" -> """["http://127.0.0.1:8083/"]""",
                "n4c:/a/b/c/map" -> """["http://127.0.0.1:8083/", "http://127.0.0.1:8084/", "http://127.0.0.1:8085/"]""")
              Some(scopes.getOrElse(URLDecoder.decode(scope, "UTF-8"), "scope not found.")) // ToResponseMarshaller("scope not found.")
            }
          })
        }
      }
    }
  }

  class HttpServer {
    def start() {
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      val route = new MockN4CService.ServerRoute().route
      val serverRoute = Directives.respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*"))(route)

      val source = Http().bind("127.0.0.1", 8089)
      source.runForeach(_.handleWith(serverRoute))
    }
  }

}
