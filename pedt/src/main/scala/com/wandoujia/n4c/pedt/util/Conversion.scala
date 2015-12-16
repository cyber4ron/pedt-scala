package com.wandoujia.n4c.pedt.util

import jdk.nashorn.api.scripting.ScriptObjectMirror

import org.slf4j.LoggerFactory
import spray.json._

import com.wandoujia.n4c.pedt.context.JSContext._

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object Conversion {
  private val log = LoggerFactory.getLogger(Conversion.getClass)

  /**
   * marshalling用到
   */
  private[pedt] def nashornToString(ref: AnyRef): String = ref match {
    case x: ScriptObjectMirror => x.asInstanceOf[ScriptObjectMirror].getClassName match {
      case "Array"     => invokeFunctionBlocking("jsonToString", x).map(_.toString).get
      case "Object"    => invokeFunctionBlocking("jsonToString", x).map(_.toString).get
      case "Arguments" => """{"warn": "function type result is unsupported."}"""
      case "Function"  => """{"warn": "function type result is unsupported."}"""
      case _           => """{"warn": "unsupported ScriptObjectMirror type."}"""
    }
    case x: AnyRef => x.toString // should be plain type, e.g. String/Integer/...
    case _         => """{"warn": "result is null"}""" // null
  }

  private[pedt] def nashornToJsValue(ref: AnyRef): Option[JsValue] = ref match {
    case x: ScriptObjectMirror =>
      import spray.json._
      x.asInstanceOf[ScriptObjectMirror].getClassName match {
        case "Array"    => invokeFunctionBlocking("jsonToString", x).map(_.toString.parseJson)
        case "Object"   => invokeFunctionBlocking("jsonToString", x).map(_.toString.parseJson)
        case "Function" => None
        case _          => None
      }
    case x => Some(JsString(x.toString))
  }

  private[pedt] def jsValueToScala(value: JsValue): AnyRef = {
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

  private[pedt] def jsValueToJava(value: JsValue): Object = {
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
