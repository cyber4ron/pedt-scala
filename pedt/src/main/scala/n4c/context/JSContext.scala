package n4c.context

import javax.script.{ ScriptException, Invocable, ScriptEngine, ScriptEngineManager }

import org.slf4j.LoggerFactory

import scala.io.Source
import spray.json._

/**
 * todo: thread safety?, 单次执行需求(记录状态和挂掉重启？)
 */
object JSContext {
  val log = LoggerFactory.getLogger(JSContext.getClass)

  val manager: ScriptEngineManager = new ScriptEngineManager
  val engine: ScriptEngine = manager.getEngineByName("nashorn")
  val invocable = engine.asInstanceOf[Invocable]

  val jsHelperScrip = sys.env.getOrElse("JS_HELPER_SCRIPT", "pedt/src/main/resources/js_helper.json")
  try {
    val x = Source.fromFile(jsHelperScrip).mkString.parseJson.asJsObject.fields
    x.foreach { x =>
      log.info(s"registering function: ${x._2.asInstanceOf[JsString].value}")
      evalJS(x._2.asInstanceOf[JsString].value)
    }
  } catch {
    case ex: Throwable => log.error(ex.getMessage)
  }

  def getEngine = engine
  def getInvocable = invocable

  def evalJS(script: String) = {
    log.info(s"$engine, eval: $script")
    try {
      engine.eval(script)
    } catch {
      case ex @ (_: ScriptException | _: NullPointerException) =>
        log.error(s"engine: $engine, eval: $script, ex: ${ex.getMessage}}")
    }
  }

  def invokeJSFunc(funcName: String, args: Object*): AnyRef = {
    log.info(s"$invocable, invoke: $funcName")
    invocable.invokeFunction(funcName, args: _*)
  }

  def declareAndInvokeJSFunc(funcName: String, function: String, args: Object*): AnyRef = {
    evalJS(function)
    invokeJSFunc(funcName, args: _*)
  }
}
