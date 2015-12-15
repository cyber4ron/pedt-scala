package com.wandoujia.n4c.pedt.context

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.wandoujia.n4c.pedt.http.HttpClient
import com.wandoujia.n4c.pedt.util.Utility
import com.wandoujia.n4c.pedt.util.Marshalling._
import Utility.TimeBoundedFuture
import org.slf4j.LoggerFactory
import spray.http._
import spray.json.JsValue
import spray.util._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Properties._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object HttpContext {
  private val log = LoggerFactory.getLogger(HttpContext.getClass)
  implicit val system: ActorSystem = ActorSystem("pedt-spray-http")

  val config = ConfigFactory.load()

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  val requestTimeOutMs = envOrElse("http.request.timeoutMs",
    config.getString("http.request.timeoutMs")).toLong.millis
  val unmarshalTimeoutMs = envOrElse("http.unmarshal.timeoutMs",
    config.getString("http.unmarshal.timeoutMs")).toLong.millis

  implicit val httpClient = new HttpClient(config)

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
