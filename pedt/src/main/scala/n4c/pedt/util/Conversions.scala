package n4c.pedt.util

import n4c.pedt.context.JSContext
import JSContext._
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.slf4j.LoggerFactory
import spray.json._

object Conversions {
  private val log = LoggerFactory.getLogger(Conversions.getClass)
  /**
   * 一般marshalling用到
   */
  private[n4c] implicit def nashornToString(ref: Option[AnyRef]): String = ref.flatMap {
    case x: ScriptObjectMirror => x.asInstanceOf[ScriptObjectMirror].getClassName match {
        case "Array" => invokeJSFunc("jsonToString", x).map(_.toString)
        case "Object" => invokeJSFunc("jsonToString", x).map(_.toString)
        case "Function" => Some("function type is unsupported.")
        case _ => Some("unsupported ScriptObjectMirror type.")
      }
    case x: AnyRef => Some(x.toString) // should be plain type, e.g. String/Integer/...
    case _ => Some("value of ref is null.") // null
  } getOrElse "shouldn't be here."

  private[n4c] implicit def nashornToJsValue(ref: AnyRef): Option[JsValue] = ref match {
    case x: ScriptObjectMirror =>
      import spray.json._
      x.asInstanceOf[ScriptObjectMirror].getClassName match {
        case "Array"    => invokeJSFunc("jsonToString", x).map(_.toString.parseJson)
        case "Object"   => invokeJSFunc("jsonToString", x).map(_.toString.parseJson)
        case "Function" => None
        case _          => None
      }
    case x => Some(x.toString.parseJson) // should be plain type, e.g. String/Integer/...
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
