package com.wandoujia.n4c.pedt.http

import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import com.wandoujia.n4c.pedt.util.Utility.TimeBoundedFuture
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import concurrent.ExecutionContext.Implicits.global

class HttpClient(config: Config) {
  private val log = LoggerFactory.getLogger(classOf[HttpClient])

  import com.wandoujia.n4c.pedt.context.HttpContext.system

  implicit val timeout: Timeout = Timeout(15.seconds) // implicit execution context

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  def request(uri: String,
              method: HttpMethod = HttpMethods.GET,
              entity: HttpEntity = HttpEntity.Empty): Future[HttpResponse] = method match {
    case HttpMethods.GET =>
      log.info(s"requesting: $uri")
      val f = (IO(Http) ? HttpRequest(method, Uri(uri))).mapTo[HttpResponse]
      f onComplete {
        case Success(x)  => log.info(s"$uri succeed, result: ${x.entity}")
        case Failure(ex) => log.warn(s"$uri failed, ex: ${ex.getMessage}")
      }
      f

    case HttpMethods.POST =>
      log.info(s"requesting: $uri")
      val f = (IO(Http) ? HttpRequest(method, uri = uri, entity = entity)).mapTo[HttpResponse]
      f onComplete {
        case Success(x)  => log.info(s"$uri succeed, result: ${x.entity}")
        case Failure(ex) => log.warn(s"$uri failed, ex: ${ex.getMessage}")
      }
      f
  }

  def requestBlock(uri: String,
                   method: HttpMethod = HttpMethods.GET,
                   entity: HttpEntity = HttpEntity.Empty,
                   waitMs: Long = 1000L): Option[HttpResponse] = {
    request(uri, method, entity) waitWithin waitMs.millis
  }
}
