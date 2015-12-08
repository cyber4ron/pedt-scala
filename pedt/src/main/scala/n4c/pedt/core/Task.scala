package n4c.pedt.core

import javax.script.ScriptException

import n4c.pedt.context.JSContext
import JSContext._
import jdk.nashorn.api.scripting.NashornException
import org.slf4j.LoggerFactory
import spray.json._

object Task {
  def apply(taskDef: JsObject) = new Task(taskDef)
  def apply(taskId: String, taskDef: JsObject) = new Task(Some(taskId.toLowerCase), taskDef)

  def handelEx(handler: Throwable => Option[AnyRef]): Throwable PartialFunction Option[AnyRef] = {
    case ex: ScriptException  => handler(ex)
    case ex: NashornException => handler(ex)
    case ex: Throwable        => handler(ex)
  }
}

class Task(val taskId: Option[String],
           val taskDef: JsObject) {
  private val log =  LoggerFactory.getLogger(Task.getClass)

  var name: String = _
  var methods = Array.empty[Method]
  var distributedMethod: Option[Script] = None
  var promisedMethod: Option[Script] = None
  var rejectedMethod: Option[Script] = None

  taskDef.fields foreach {
    case ("distributed", value) => distributedMethod = Some(Script(value).get)
    case ("promised", value)    => promisedMethod = Some(Script(value).get)
    case ("rejected", value)    => rejectedMethod = Some(Script(value).get)
    case kv @ (taskName, value) =>
      name = taskName
      methods :+= new Method(value.asJsObject)
    case _ => throw new IllegalArgumentException
  }

  distributed(taskDef)

  def this(taskDef: JsObject) = this(None, taskDef)

  def execute(args: Map[String, JsValue]): Option[AnyRef] = { // execute js (nashorn) 返回的都AnyRef. args用于mix
    log.info(s"in task.execute, args: $args")
      if (methods.length == 1) {
        try {
          methods(0).execute(args).flatMap {x =>
            log.info(s"result of Task.execute: $x, args: $args")
            promised(x)
          }
        } catch Task.handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else None)
    } else {
      val results = methods map { method =>
        try {
          method.execute(args).flatMap(x => promised(x))
        } catch Task.handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else None)
      } flatMap {x => x}
      if(!results.isEmpty) Some(results) else None
    }
  }

  def distributed(taskObject: JsObject) {
    try {
      if (distributedMethod.isDefined) distributedMethod.get.execute(invokeJSFunc("stringToJson", PrettyPrinter(taskObject)))
    } catch Task.handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else None)
  }

  def promised(taskResult: AnyRef): Option[AnyRef] = { // taskResult是nashorn的返回值，应该是js compatible的
    try {
      if (promisedMethod.isDefined) promisedMethod.get.execute(taskResult.asInstanceOf[AnyRef])
      else Some(taskResult)
    } catch Task.handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else None)
  }

  def rejected(reason: Object): Option[AnyRef] = {
    try {
      if (rejectedMethod.isDefined) rejectedMethod.get.execute(reason)
      else Some(reason)
    } catch Task.handelEx(ex => Some(ex.getMessage))
  }

  def toCompatibleJsObj = invokeJSFunc("stringToJson", PrettyPrinter(taskDef)) // todo: remove hardcode
}
