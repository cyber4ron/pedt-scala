package com.wandoujia.n4c.pedt.core

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

import org.slf4j.LoggerFactory
import spray.json._

import com.wandoujia.n4c.pedt
import pedt.util.Utility
import pedt.context.JSContext._

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object Task {
  private val log = LoggerFactory.getLogger(Task.getClass)
  def apply(taskDef: JsObject) = new Task(taskDef)
  def apply(taskId: String, taskDef: JsObject) = new Task(Some(taskId.toLowerCase), taskDef)

  /**
   * handle future completion
   */
  private def handleCompletion[U, V](succeed: U => V, failed: Throwable => V): Try[U] => V = {
    case Success(x)  => succeed(x.asInstanceOf[U]) //
    case Failure(ex) => failed(ex)
  }

  private def defaultHandler[U](a: String, b: String)(implicit x: String => U => _ = { msg: String => _: U => log.info(msg) },
                                                      y: String => Throwable => _ = msg => { ex: Throwable => log.error(msg.format(ex.getMessage)) }): Try[U] => _ =
    handleCompletion(x(a), y(b))
}

class Task(val taskId: Option[String],
           val taskDef: JsObject) {
  import Task.{ log, defaultHandler }

  var name: String = _
  var methods = List.empty[Method]
  var distributedMethod: Option[Script] = None
  var promisedMethod: Option[Script] = None
  var rejectedMethod: Option[Script] = None

  taskDef.fields foreach {
    case ("distributed", value) => distributedMethod = Script(value)
    case ("promised", value)    => promisedMethod = Script(value)
    case ("rejected", value)    => rejectedMethod = Script(value)
    case kv @ (taskName, value) =>
      name = taskName
      methods :+= new Method(value.asJsObject)
    case _ => throw new IllegalArgumentException
  }

  distributed(taskDef)

  def this(taskDef: JsObject) = this(None, taskDef)

  import concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  /**
   * 多个method返回seq
   */
  def execute(args: Map[String, JsValue]): Future[AnyRef] = { // execute js (nashorn) 返回的都AnyRef. args用于mix
    log.info(s"in task.execute, args: $args")
    if (methods.length == 1) {
      methods.head.execute(args) recoverWith {
        case ex: Throwable => rejected(ex.getMessage)
      } flatMap { x =>
        val f = promised(x)
        f onComplete defaultHandler(s"Task.execute succeed, args: $args, result: $x", s"task.execute failed, ex.message: %s")
        f
      }
    } else {
      val results = methods map { method =>
        method.execute(args) recoverWith {
          case ex: Throwable => rejected(ex.getMessage)
        } flatMap { x =>
          val f = promised(x)
          f onComplete defaultHandler(s"result of Task.execute: $x, args: $args", s"task.execute failed, ex.message: %s")
          f
        }
      }
      Future.sequence(results.toSeq)
    }
  }

  def distributed(taskObject: JsObject) {
    if (distributedMethod.isDefined) {
      import Utility._
      val f = distributedMethod.get.execute(invokeFunction("stringToJson", PrettyPrinter(taskObject)))
      f onComplete defaultHandler(s"distributed method succeed.", s"distributed method failed, ex.message: %s")
      f waitWithin 1.second
    }
  }

  def promised(taskResult: AnyRef): Future[AnyRef] = { // taskResult是nashorn的返回值，应该是js compatible的
    if (promisedMethod.isDefined) {
      val f = promisedMethod.get.execute(taskResult.asInstanceOf[AnyRef]) recoverWith {
        case ex: Throwable => rejected(ex.getMessage)
      }
      f onComplete defaultHandler(s"promised method succeed.", s"promised method failed, ex.message: %s")
      f
    } else Future { taskResult }
  }

  def rejected(reason: Object): Future[AnyRef] = {
    if (rejectedMethod.isDefined) {
      val f = rejectedMethod.get.execute(reason)
      f onComplete defaultHandler(s"rejected method succeed.", s"rejected method failed, ex.message: %s")
      f
    } else Future { reason }
  }

  def toCompatibleJsObj = invokeFunction("stringToJson", PrettyPrinter(taskDef))
}
