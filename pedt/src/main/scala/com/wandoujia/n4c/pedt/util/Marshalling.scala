package com.wandoujia.n4c.pedt.util

import scala.concurrent.Future
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.http._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.json._

object Marshalling {
  private val log = LoggerFactory.getLogger(Marshalling.getClass)

  val config = ConfigFactory.load()
  val serviceTimeout = config.getLong("http.request.timeoutMs").millis

  /**
   *  Future[AnyRef] -> HttpEntity of MediaTypes.`text/plain`
   */
  implicit val AnyRefFutureMarshaller: Marshaller[Future[AnyRef]] = {
    import Utility.TimeBoundedFuture
    Marshaller.of[Future[AnyRef]](MediaTypes.`text/plain`) { (future, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType,
        Conversion.nashornToString(future waitWithin serviceTimeout getOrElse {
          """{"error": "wait future failed when marshalling."}"""
        })))
    }
  }

  /**
   *  HttpEntity of MediaTypes.`text/plain` -> JsValue
   */
  implicit val JsValueUnmarshaller = Unmarshaller[JsValue](MediaTypes.`text/plain`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      log.info(s"unmarshalling entity, data: ${data.asString(contentType.charset)}")
      data.asString(contentType.charset).parseJson // throws ParsingException
    case HttpEntity.Empty => JsString("")
  }
}
