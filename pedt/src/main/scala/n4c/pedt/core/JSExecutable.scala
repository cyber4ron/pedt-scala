package n4c.pedt.core

import java.util.Base64
import n4c.pedt.context.JSContext
import n4c.pedt.util.TaskProxy
import org.slf4j.LoggerFactory
import spray.json.{ JsString, JsValue }

trait JSExecutable {
  def execute(args: Object*): AnyRef
}

trait Content extends JSExecutable {
  val subtype: String
  val encodeType: String
  val value: String // (utf16)
}

object Data {
  private val dataPattern = "^data:(string:)?(?:utf8:)?([^:]+:){0,1}(?:utf8:)?".r

  def parse: String PartialFunction Data = {
    case exeValue if exeValue.startsWith("data:") =>
      val m = dataPattern.findAllMatchIn(exeValue).next()
      m.subgroups.filter(_ != null).toArray match {
        case g if g.isEmpty => // data:
          new Data("string", "utf-8", exeValue.substring(m.end, exeValue.length))
        case g if m.groupCount == 1 && g(0) == "base64:" => // data:base64:
          val bytes = Base64.getDecoder.decode(exeValue.substring(m.end, exeValue.length))
          new Data("string", "base64", new String(bytes).replace("\n", ""))
      }
  }

  def apply: String => Option[Data] = parse.lift

  def apply(value: JsValue): Option[Data] = apply(value.asInstanceOf[JsString].value)
}

class Data(override val subtype: String,
           override val encodeType: String,
           override val value: String = "") extends Content {
  def execute(args: Object*): Option[AnyRef] = {
    import JSContext._
    evalJS(value)
  }
}

object Script {
  private val scriptPattern = "^script:([^:]+:)(?:utf8:)?([^:]+:){0,1}(?:utf8:)?".r

  def parse: String PartialFunction Script = {
    case exeValue if exeValue.startsWith("script:") =>
      val m = scriptPattern.findAllMatchIn(exeValue).next()
      m.subgroups.filter(_ != null).toArray match {
        case g if g.length == 1 && g(0) == "javascript:" => // javascript:
          new Script("javascript", "utf8", exeValue.substring(m.end, exeValue.length))
        case g if g.length == 2 && g(0) == "javascript:" && g(1) == "base64:" => // javascript:base64:
          val bytes = Base64.getDecoder.decode(exeValue.substring(m.end, exeValue.length))
          new Script("javascript", "base64", new String(bytes).replace("\n", ""))
      }
  }

  def apply: String => Option[Script] = parse.lift

  def apply(value: JsValue): Option[Script] = apply(value.asInstanceOf[JsString].value)
}

class Script(override val subtype: String,
             override val encodeType: String,
             override val value: String = "") extends Content {
  private val log = LoggerFactory.getLogger(Script.getClass)
  def execute(args: Object*): Option[AnyRef] = {
    log.info(s"in script.execute, args: $args")
    import JSContext._
    val funcNameCapture = """^[\s]*function\s+([^\s(]+)""".r // 前置换行
    val matches = funcNameCapture.findFirstMatchIn(value)
    if (matches.isDefined) {
      if (matches.get.groupCount == 1) {
        val funcName = matches.get.subgroups.head
        declareAndInvokeJSFunc(funcName, value, args: _*) // args must be java objects
      } else throw new IllegalStateException
    } else {
      evalJS(value)
    }
  }
}

object EmbeddedTask {
  private val taskPattern = "^taskId:([a-z0-9]{32})$".r

  def parse: String PartialFunction Task = {
    case exeValue if taskPattern.pattern.matcher(exeValue).matches =>
      val taskId = taskPattern.findFirstMatchIn(exeValue).get.subgroups.head
      TaskProxy.get(taskId) match {
        case Some(task) => task
      }
  }

  def apply: String => Option[Task] = parse.lift

  def apply(value: JsValue): Option[Task] = apply(value.asInstanceOf[JsString].value)
}
