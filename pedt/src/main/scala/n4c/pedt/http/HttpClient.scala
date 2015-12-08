package n4c.pedt.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpResponse, HttpRequest }
import akka.stream.ActorMaterializer

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._

/**
 * has to be thread safe
 *
 */
class HttpClient {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val x = 1000 * 1000L //

  def request(uri: String): Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri))

  def requestBlock(uri: String)(implicit timeoutMs: Long): HttpResponse = {
    val responseFuture = Http().singleRequest(HttpRequest(uri = uri))
    Await.result(responseFuture, timeoutMs milliseconds)
  }
}
