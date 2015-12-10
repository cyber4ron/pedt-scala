package n4c.pedt.context

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import n4c.pedt.http.{HttpClient, MarshallingSupport}
import org.slf4j.LoggerFactory
import spray.json.JsValue

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties._

object HttpContext {
  private val log = LoggerFactory.getLogger(HttpContext.getClass)

  import MarshallingSupport._

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load()

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  implicit val httpClient = new HttpClient(config)
  implicit val requestTimeOutMs = envOrElse("http.request.timeoutMs",
    config.getString("http.request.timeoutMs")).toLong

  val unmarshalTimeoutMs = envOrElse("http.unmarshal.timeoutMs",
    config.getString("http.unmarshal.timeoutMs")).toLong.millis

  def request(url: String,
              method: HttpMethod = HttpMethods.GET,
              entity: RequestEntity = HttpEntity.empty(ContentType(MediaTypes.`application/json`)))(implicit httpClient: HttpClient): Future[JsValue] = method match {
    case HttpMethods.GET =>
      log.info(s"request getting: $url")
      httpClient.request(url).flatMap(response => Unmarshal(response.entity).to[JsValue])
    case HttpMethods.POST =>
      log.info(s"request posting: $url, entity: $entity")
      httpClient.request(url, method, entity).flatMap(response => Unmarshal(response.entity).to[JsValue])
  }

  def requestBlocking(url: String)(implicit httpClient: HttpClient): Option[JsValue] = {
    try {
      log.info(s"block requesting: $url")
      val x = Some(Await.result(Unmarshal(httpClient.requestBlock(url).entity).to[JsValue], unmarshalTimeoutMs))
      log.info(s"block request returned : $url, result: $x")
      x
    } catch {
      case _: TimeoutException => None
      case _: Throwable        => None
    }
  }
}
