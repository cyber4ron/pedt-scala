package n4c.pedt.util

import java.net.URLEncoder

import spray.json._

object Utility {
  private[n4c] def toUrlQueryFormat(args: Map[String, JsValue]): String = {
    for (kv <- args) yield s"${kv._1}=${URLEncoder.encode(CompactPrinter(kv._2), "UTF-8")}"
  }.mkString("&")
}
