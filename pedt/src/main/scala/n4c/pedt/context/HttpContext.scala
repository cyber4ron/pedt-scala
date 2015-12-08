package n4c.pedt.context

import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import n4c.pedt.http.{HttpClient, MarshallingSupport}
import org.slf4j.LoggerFactory
import spray.json.JsValue

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties._

object HttpContext {
  val log = LoggerFactory.getLogger(HttpContext.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global
  import MarshallingSupport._

  val config = ConfigFactory.load()

  implicit val httpClient = new HttpClient
  implicit val requestTimeOutMs = envOrElse("http.request.timeoutMs",
    config.getString("http.request.timeoutMs")).toLong

  val unmarshalTimeoutMs = envOrElse("http.unmarshal.timeoutMs",
    config.getString("http.unmarshal.timeoutMs")).toLong.millis

  def request(url: String)(implicit httpClient: HttpClient): Future[JsValue] = {
    log.info(s"requesting: $url")
    httpClient.request(url).flatMap(response => Unmarshal(response.entity).to[JsValue])
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
