package n4c.pedt.context

import com.typesafe.config.ConfigFactory
import n4c.pedt.http.HttpClient
import n4c.pedt.util.Marshalling
import n4c.pedt.util.Utility.TimeBoundedFuture
import org.slf4j.LoggerFactory
import spray.http._
import spray.json.JsValue
import spray.util._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties._

object HttpContext {
  private val log = LoggerFactory.getLogger(HttpContext.getClass)

  import Marshalling._

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load()

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  implicit val httpClient = new HttpClient(config)
  val requestTimeOutMs = envOrElse("http.request.timeoutMs",
    config.getString("http.request.timeoutMs")).toLong.millis

  val unmarshalTimeoutMs = envOrElse("http.unmarshal.timeoutMs",
    config.getString("http.unmarshal.timeoutMs")).toLong.millis

  def request(url: String,
              method: HttpMethod = HttpMethods.GET,
              entity: HttpEntity = HttpEntity.Empty)(implicit httpClient: HttpClient): Future[JsValue] = method match {
      case HttpMethods.GET =>
        log.info(s"request getting: $url")
        httpClient.request(url).map { resp =>
          JsValueUnmarshaller(resp.entity).get
        }
      case HttpMethods.POST =>
        log.info(s"request posting: $url, entity: $entity")
        httpClient.request(url, method, entity).map { resp =>
          JsValueUnmarshaller(resp.entity).get
        }
    }


  def requestBlocking(url: String)(implicit httpClient: HttpClient): Option[JsValue] = {
    log.info(s"block requesting: $url")
    httpClient.request(url).waitWithin(requestTimeOutMs).map { resp =>
      JsValueUnmarshaller(resp.entity).get
    }
  }
}
