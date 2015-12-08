package n4c.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import n4c.util.Conversions
import spray.json._

object MarshallingSupport {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  // marshall
  implicit def sprayJsValueMarshaller(implicit printer: AnyRef => String = Conversions.nashornToString): ToResponseMarshaller[AnyRef] = // 改名字sprayJsValueMarshaller
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer)

  // unmarshal
  implicit val stringUnmarshaller: Unmarshaller[HttpEntity, String] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      new String(data.toArray, charset.value)
    }

  implicit val jsValueUmMarshaller: Unmarshaller[HttpEntity, JsValue] =
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      println(new String(data.toArray, charset.value))
      try {
        val x = new String(data.toArray, charset.value)
        new String(data.toArray, charset.value).parseJson // 还有简单写法吗
      } catch {
        case _: Throwable => JsString(new String(data.toArray, charset.value))
      }
    }
}
