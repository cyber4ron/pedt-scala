package com.wandoujia.n4c.pedt.context

import javax.script.{ Invocable, ScriptEngine, ScriptEngineManager, ScriptException }

import com.wandoujia.n4c.pedt.util.Conversion
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.Future
import scala.io.Source
import scala.util.{ Failure, Success }

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object JSContext {
  private val log = LoggerFactory.getLogger(JSContext.getClass)
  import concurrent.ExecutionContext.Implicits.global

  private val manager: ScriptEngineManager = new ScriptEngineManager
  private val engine: ScriptEngine = manager.getEngineByName("nashorn")
  private val invocable = engine.asInstanceOf[Invocable]

  // js helper
  val jsHelperScript = sys.env.getOrElse("JS_HELPER_SCRIPT", "js_helper.json")
  try {
    val x = Source.fromFile(jsHelperScript).mkString.parseJson.asJsObject.fields
    x.foreach { x =>
      log.info(s"registering function: ${x._2.asInstanceOf[JsString].value}")
      engine.eval(x._2.asInstanceOf[JsString].value)
    }
  } catch {
    case ex: Throwable => log.error(ex.getMessage)
  }

  // js pedt env
  try {
    val jsPEDTEnvScript = sys.env.getOrElse("JS_PEDT_SCRIPT", "pedt.js")
    log.info(s"setting up js pedt env: ${Source.fromFile(jsPEDTEnvScript).mkString}")
    engine.eval(Source.fromFile(jsPEDTEnvScript).mkString)
  } catch {
    case ex: Throwable => log.error(ex.getMessage)
  }

  def getEngine = engine
  def getInvocable = invocable

  def load() {}

  // 并行执行js
  // nashorn支持并行，但避免shared变量。references: tbd
  def eval(script: String): Future[AnyRef] = {
    val f = Future {
      log.info(s"$engine, evaluating: [$script]")
      engine.eval(script)
    }
    f onComplete {
      case Success(x)  => log.info(s"eval: [$script] succeed, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"eval: [$script] failed, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeFunction(funcName: String, args: Object*): Future[AnyRef] = {
    val f = Future {
      log.info(s"$invocable, invoking: $funcName, args: $args")
      invocable.invokeFunction(funcName, args: _*)
    }
    f onComplete {
      case Success(x)  => log.info(s"invoke function: $funcName succeed, args: $args, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  def declareAndInvokeFunction(funcName: String, function: String, args: Object*): Future[AnyRef] = {
    val f = eval(function) flatMap { x => invokeFunction(funcName, args: _*) }
    f onComplete {
      case Success(x)  => log.info(s"declare and invoke function: $funcName succeed, args: $args, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"declare and invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  // 串行执行js，不过同时会有并行接口执行。
  // 目前串行这些方法还不能用于嵌套task的执行，以后需要可以实现。
  def evalExclusive(script: String): Future[AnyRef] = {
    val f = Future {
      log.info(s"$engine, evaluating: [$script]...")
      engine.synchronized {
        engine.eval(script)
      }
    }
    f onComplete {
      case Success(x)  => log.info(s"eval: [$script] succeed, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"eval: [$script] failed, ex.message: ${ex.getMessage}")
    }
    f
  }

  def invokeFunctionExclusive(funcName: String, args: Object*): Future[AnyRef] = {
    val f = Future {
      log.info(s"$invocable, invoking: $funcName, args: $args")
      engine.synchronized {
        invocable.invokeFunction(funcName, args: _*)
      }
    }
    f onComplete {
      case Success(x)  => log.info(s"invoke function: $funcName succeed, args: $args, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  def declareAndInvokeFunctionExclusive(funcName: String, function: String, args: Object*): Future[AnyRef] = {
    val f = evalExclusive(function) flatMap { x => invokeFunctionExclusive(funcName, args: _*) }
    f onComplete {
      case Success(x)  => log.info(s"declare and invoke function: $funcName succeed, args: $args, result: ${Conversion.nashornToString(x)}")
      case Failure(ex) => log.error(s"declare and invoke function: $funcName failed, args: $args, ex.message: ${ex.getMessage}")
    }
    f
  }

  // 阻塞等待结果(不做串行)
  def invokeFunctionBlocking(funcName: String, args: Object*): Option[AnyRef] = {
    log.info(s"$invocable, blocking invoke: $funcName, args: $args")
    try {
      val x = invocable.invokeFunction(funcName, args: _*)
      log.info(s"$invocable, blocking invoke succeed: $funcName, result: ${Conversion.nashornToString(x)}")
      Some(x)
    } catch {
      case ex: ScriptException =>
        log.error(s"${ex.getMessage}, $funcName, $args")
        None
    }
  }

  //  val f = Future {
  //    engine.eval("function stringToJson(jsonStr) { return JSON.parse(jsonStr); }") // java.lang.NoClassDefFoundError: Could not initialize class com.wandoujia.n4c.pedt.context.JSContext$
  //  }
  //  println(">>>> waiting...")
  //  Await.result(f, 1.second)
}
