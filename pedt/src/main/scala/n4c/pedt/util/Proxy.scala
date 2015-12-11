package n4c.pedt.util

import java.net.URLEncoder

import com.typesafe.config.ConfigFactory
import n4c.pedt.context.HttpContext._
import n4c.pedt.core.{Scope, Task}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.Properties.envOrElse

/**
 * should be thread safe
 */
abstract class Proxy[Proxied, Cached](getUrl: => String) {
  val url = getUrl
  var cache = Map.empty[String, Cached]

  def apply(b: Cached): Option[Proxied]

  def retrieve(key: String): Option[Cached]

  def get(key: String): Option[Proxied] = {
    if (cache.contains(key)) cache.get(key).flatMap(apply)
    else retrieve(key).flatMap(put(key, _))
  }

  def put(taskId: String, value: Cached): Option[Proxied] = {
    cache = cache.updated(taskId, value)
    apply(value)
  }
}

object ScopeProxy extends Proxy[Scope, Scope]({
  val config = ConfigFactory.load()
  envOrElse("n4c.service.resource", config.getString("n4c.service.resource"))
}) {
  private val log = LoggerFactory.getLogger(ScopeProxy.getClass)

  override def apply(a: Scope): Option[Scope] = Some(a)

  override def retrieve(scope: String): Option[Scope] = {
    if (Array("?", "::*", "::?", ":::").contains(scope)) return None

    val parts = scope.split("\\:") // "::".split("\\:") = Array("", "", c) ?
    if (parts.length != 3) throw new IllegalArgumentException
    val Array(system, path, scopeFilter) = parts

    requestBlocking(s"$url${URLEncoder.encode(s"$system:$path", "UTF-8")}") flatMap {
      case x: JsArray =>
        val resources = x.asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value)
        Scope(system, path, scopeFilter, resources) // 改
      case _ => None
    }
  }

  def invalidate(scope: String) {
    log.info(s"invalidating scope:[$scope]...")
    cache = cache - scope
  }
}

object TaskProxy extends Proxy[Task, JsValue]({
  val config = ConfigFactory.load()
  envOrElse("n4c.service.task", config.getString("n4c.service.task"))
}) {
  private val log = LoggerFactory.getLogger(TaskProxy.getClass)
  override def apply(taskDef: JsValue): Option[Task] = {
    try {
      Some(Task(taskDef.asJsObject))
    } catch {
      case ex: Throwable => log.error(ex.getCause.toString); None
    }
  }

  override def retrieve(taskId: String): Option[JsValue] = requestBlocking(s"$url$taskId")
}
