package n4c.pedt.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * has to be thread safe
 *
 */
class HttpClient(config: Config) {
  private val log = LoggerFactory.getLogger(classOf[HttpClient])

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val x = 1000 * 1000L //

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  def request(uri: String,
              method: HttpMethod = HttpMethods.GET,
              entity: RequestEntity = HttpEntity.empty(ContentType(MediaTypes.`application/json`))): Future[HttpResponse] = method match {
    case HttpMethods.GET =>
      log.info(s"requesting: $uri")
      Http().singleRequest(HttpRequest(uri = uri))
    case HttpMethods.POST =>
      Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity))
  }

  def post(uri: String): Future[HttpResponse] = {

    val json = s"""{
                 |	"key": "$x",
                 |	"type": "scope",
                 |	"version": "1.1",
                 |	"receive": "http://$host:$port/notify"
                 |}"""

    val ent = Requesten(ContentType(MediaTypes.`application/json`), """{"id":"1"}""")

    Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = ent))
  }

  def requestBlock(uri: String)(implicit timeoutMs: Long): HttpResponse = {
    val responseFuture = Http().singleRequest(HttpRequest(uri = uri))
    Await.result(responseFuture, timeoutMs milliseconds)
  }
}
