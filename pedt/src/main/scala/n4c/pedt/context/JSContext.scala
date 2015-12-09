package n4c.pedt.context

import javax.script.{ScriptException, Invocable, ScriptEngine, ScriptEngineManager}

import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

/**
 * todo: thread safety?, 单次执行需求(记录状态和挂掉重启？)
 */
object JSContext {
  private val log = LoggerFactory.getLogger(JSContext.getClass)

  import concurrent.ExecutionContext.Implicits.global

  val manager: ScriptEngineManager = new ScriptEngineManager
  val engine: ScriptEngine = manager.getEngineByName("nashorn")
  val invocable = engine.asInstanceOf[Invocable]

  val jsHelperScrip = sys.env.getOrElse("JS_HELPER_SCRIPT", "js_helper.json")
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

  def evalJS(script: String): Future[AnyRef] = {
    log.info(s"$engine, eval: $script")
    val f = Future { engine.eval(script) }
    f onComplete {
      case Success(returned) => returned
      case Failure(ex) => log.error("script: [$script] failed, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeJSFunc(funcName: String, args: Object*): Future[AnyRef] = {
    log.info(s"$invocable, invoke: $funcName, args: $args")
    val f = Future { invocable.invokeFunction(funcName, args: _*) }
    f onComplete {
      case Success(returned) => returned
      case Failure(ex) => log.error(s"function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeJSFuncBlocking(funcName: String, args: Object*): Option[AnyRef] = {
    log.info(s"$invocable, blocking invoke: $funcName, args: $args")
    try {
      Some(invocable.invokeFunction(funcName, args: _*))
    } catch {
      case ex: ScriptException => log.error(s"${ex.getMessage}, $funcName, $args"); None
    }
  }

  def declareAndInvokeJSFunc(funcName: String, function: String, args: Object*): Future[AnyRef] = {
    log.info(s"$invocable, invoke: $funcName, args: $args")
    val f = Future {
      engine.eval(function)
      invocable.invokeFunction(funcName, args: _*)
    }
    f onComplete {
      case Success(returned) => returned
      case Failure(ex) => log.error(s"function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }
}
