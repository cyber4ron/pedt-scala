package n4c.pedt.context

import javax.script.{ScriptException, Invocable, ScriptEngine, ScriptEngineManager}

import n4c.pedt.util.Conversions
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

  private val manager: ScriptEngineManager = new ScriptEngineManager
  private val engine: ScriptEngine = manager.getEngineByName("nashorn")
  private val invocable = engine.asInstanceOf[Invocable]

  val jsHelperScrip = sys.env.getOrElse("JS_HELPER_SCRIPT", "js_helper.json")
  try {
    val x = Source.fromFile(jsHelperScrip).mkString.parseJson.asJsObject.fields
    x.foreach { x =>
      log.info(s"registering function: ${x._2.asInstanceOf[JsString].value}")
      evalJSSync(x._2.asInstanceOf[JsString].value)
    }
  } catch {
    case ex: Throwable => log.error(ex.getMessage)
  }

  def getEngine = engine
  def getInvocable = invocable

  // async
  def evalJS(script: String): Future[AnyRef] = {
    val f = Future {
      log.info(s"$engine, evaluating: [$script]")
      engine.eval(script)
    }
    f onComplete {
      case Success(x) => log.info(s"eval: [$script] succeed, result: $x")
      case Failure(ex) => log.error(s"eval: [$script] failed, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeJSFunc(funcName: String, args: Object*): Future[AnyRef] = {
    val f = Future {
                     log.info(s"$invocable, invoking: $funcName, args: $args")
                     invocable.invokeFunction(funcName, args: _*)
                   }
    f onComplete {
      case Success(x) => log.info(s"invoke function: $funcName succeed, args: $args, result: $x")
      case Failure(ex) => log.error(s"invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  def declareAndInvokeJSFunc(funcName: String, function: String, args: Object*): Future[AnyRef] = {
    val f = evalJS(function) flatMap {x => invokeJSFunc(funcName, args: _*)}
    f onComplete {
      case Success(x) =>  log.info(s"declare and invoke function: $funcName succeed, args: $args, result: $x")
      case Failure(ex) => log.error(s"declare and invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  // sync
  def evalJSSync(script: String): Future[AnyRef] = {
    val f = Future {
      log.info(s"$engine, evaluating: [$script]")
      engine.synchronized {
        engine.eval(script)
      }
    }
    f onComplete {
      case Success(x) => log.info(s"eval: [$script] succeed, result: ${Conversions.nashornToString(Some(x))}")
      case Failure(ex) => log.error(s"eval: [$script] failed, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeJSFuncSync(funcName: String, args: Object*): Future[AnyRef] = {
    val f = Future {
      log.info(s"$invocable, invoking: $funcName, args: $args")
      engine.synchronized {
        invocable.invokeFunction(funcName, args: _*)
      }
    }
    f onComplete {
      case Success(x) => log.info(s"invoke function: $funcName succeed, args: $args, result: ${Conversions.nashornToString(Some(x))}")
      case Failure(ex) => log.error(s"invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  def declareAndInvokeJSFuncSync(funcName: String, function: String, args: Object*): Future[AnyRef] = {
    val f = evalJSSync(function) flatMap {x => invokeJSFuncSync(funcName, args: _*)}
    f onComplete {
      case Success(x) =>  log.info(s"declare and invoke function: $funcName succeed, args: $args, result: ${Conversions.nashornToString(Some(x))}")
      case Failure(ex) => log.error(s"declare and invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  // blocking
  def invokeJSFuncBlocking(funcName: String, args: Object*): Option[AnyRef] = {
    log.info(s"$invocable, blocking invoke: $funcName, args: $args")
    try {
      val x = Some(invocable.invokeFunction(funcName, args: _*))
      log.info(s"$invocable, blocking invoke succeed: $funcName, result: $x")
      x
    } catch {
      case ex: ScriptException => log.error(s"${ex.getMessage}, $funcName, $args"); None
    }
  }
}
