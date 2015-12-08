package n4c.pedt.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import n4c.pedt.util.Conversions
import spray.json._

object MarshallingSupport {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  // marshall
  implicit def AnyRefMarshaller(implicit printer: Option[AnyRef] => String = Conversions.nashornToString): ToResponseMarshaller[Option[AnyRef]] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer)

  // unmarshal
  implicit val stringUnmarshaller: Unmarshaller[HttpEntity, String] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      new String(data.toArray, charset.value)
    }

  implicit val jsValueUmMarshaller: Unmarshaller[HttpEntity, JsValue] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      try {
        new String(data.toArray, charset.value).parseJson
      } catch {
        case _: Throwable => JsString(new String(data.toArray, charset.value))
      }
    }
}
