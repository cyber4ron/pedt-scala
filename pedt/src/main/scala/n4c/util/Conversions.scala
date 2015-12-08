package n4c.util

import n4c.context.JSContext._
import jdk.nashorn.api.scripting.ScriptObjectMirror
import spray.json._

object Conversions {
  private[n4c] implicit def nashornToString(obj: Any): String = obj.asInstanceOf[AnyRef] match {
    case x: ScriptObjectMirror =>
      x.asInstanceOf[ScriptObjectMirror].getClassName match {
        case "Array"    => invokeJSFunc("jsonToString", x).toString
        case "Object"   => invokeJSFunc("jsonToString", x).toString
        case "Function" => "function is unsupported."
        case _          => "unsupported."
      }
    case x: AnyRef => x.toString // should be plain type, e.g. String/Integer/...
    case _         => "" // null
  }

  private[n4c] implicit def nashornToJsValue(obj: Any): JsValue = obj.asInstanceOf[AnyRef] match {
    case x: ScriptObjectMirror =>
      import spray.json._
      x.asInstanceOf[ScriptObjectMirror].getClassName match {
        case "Array"    => invokeJSFunc("jsonToString", x).toString.parseJson
        case "Object"   => invokeJSFunc("jsonToString", x).toString.parseJson
        case "Function" => JsString("function is unsupported.")
        case _          => JsString("unsupported.")
      }
    case x => x.toString.parseJson // should be plain type, e.g. String/Integer/...
  }

  private[n4c] def jsValueToScala(value: JsValue): AnyRef = {
    value match {
      case _: JsBoolean => new java.lang.Boolean(value.asInstanceOf[JsBoolean].value)
      case _: JsString  => value.asInstanceOf[JsString].value
      case _: JsNumber => value.asInstanceOf[JsNumber].value match {
        case num if num.isValidLong   => long2Long(num.toLongExact)
        case num if num.isExactDouble => double2Double(num.toDouble) // to n4c.test
      }
      case _: JsArray  => value.asInstanceOf[JsArray].elements.map(jsValueToScala)
      case _: JsObject => value.asInstanceOf[JsObject].fields.map(kv => kv._1 -> jsValueToScala(kv._2))

      case _           => throw new IllegalArgumentException("unsupported JsValue Type.")
    }
  }

  private[n4c] def jsValueToJava(value: JsValue): Object = {
    import collection.JavaConversions._
    value match {
      case _: JsBoolean => new java.lang.Boolean(value.asInstanceOf[JsBoolean].value)
      case _: JsString  => value.asInstanceOf[JsString].value
      case _: JsNumber => value.asInstanceOf[JsNumber].value match {
        case num if num.isValidLong   => new java.lang.Long(num.toLongExact)
        case num if num.isExactDouble => new java.lang.Double(num.toDouble) // to n4c.test
      }
      case _: JsArray  => seqAsJavaList(value.asInstanceOf[JsArray].elements.map(jsValueToJava))
      case _: JsObject => mapAsJavaMap(value.asInstanceOf[JsObject].fields.map(kv => kv._1 -> jsValueToJava(kv._2)))

      case _           => throw new IllegalArgumentException("unsupported JsValue Type.")
    }
  }
}
