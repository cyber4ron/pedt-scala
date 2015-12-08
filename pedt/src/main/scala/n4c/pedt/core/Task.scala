package n4c.pedt.core

import javax.script.ScriptException

import n4c.pedt.context.JSContext
import JSContext._
import jdk.nashorn.api.scripting.NashornException
import spray.json._

/**
 * 一个task总是一个JSON对象(object)，它拥有至少一个成员名（"map"/"run"），用来表示分发方法(distribution method)，
 * 以及可能的成员属性，包括scope和arguments等。
 *
 * 如果一个task的map/run成员是任务标识（taskId），那么它是一个需要从远程下载的已发布任务定义(published taskDef)；map成员只接受taskId。
 *
 * 如果一个task的run成员是字符串（string），那么它是被编码在taskDef中可执行的普通本地任务(local task)；
 *
 * 如果它是对象(object)，那么它将作为一个本地任务定义(local/unpublished taskDef)来处理。
 *
 * examples:
 *
 * "x": {
 * 		"run": "task:570b41ba61ade63987d318b0c08e4fa4",
 * 		"scope": "n4c:/a/b/c/n4c.test:*",
 * 		"arguments": {
 * 			"arg1": 1,
 * 			"arg2": true
 * 		}
 * 	}
 *
 * "x": {
 * 		"map": "task:570b41ba61ade63987d318b0c08e4fa4",
 * 	}
 *
 * "x": {
 * 		"run": "data:string:utf8:base64:SGVsbG8gV29ybGQhCg==",
 * 	}
 *
 * {
 * 	"taskX": {
 * 		"run": "task:570b41ba61ade63987d318b0c08e4fa4",
 * 		"scope": "n4c:/a/b/c/n4c.test:*",
 * 		"arguments": {
 * 			"arg1": 1,
 * 			"arg2": true
 * 		}
 * 	},
 *
 * 	"distributed": ""script:javascript:base64:Y29uc29sZS5sb2coImhpIikK"",
 * 	"promised": "script:javascript:utf8:console.log(\"hi\")",
 * 	"rejected": "script:javascript:base64: ... "
 * }
 *
 */
object Task {
  def apply(taskDef: JsObject) = new Task(taskDef)
  def apply(taskId: String, taskDef: JsObject) = new Task(Some(taskId.toLowerCase), taskDef)
}

class Task(val taskId: Option[String],
           val taskDef: JsObject) {

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

  def mixArguments(ref: Map[String, JsValue]) = {
    methods.foreach(x => Arguments.mix(x.arguments, ref))
    this
  }

  def handelEx(handler: Throwable => AnyRef): Throwable PartialFunction AnyRef = {
    case ex: ScriptException  => handler(ex)
    case ex: NashornException => handler(ex)
    case ex: Throwable        => handler(ex)
  }

  def execute(args: Map[String, JsValue]): Any = { // execute js (nashorn) 返回的都AnyRef. args用于mix
    if (methods.length == 1) try {
      log.info(s"in task.execute, args: $args")
      val result = methods(0).execute(args)
      promised(result)
    } catch handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else ex.getMessage)
    else methods map { method =>
      try {
        val result = method.execute(args)
        promised(result)
      } catch handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else ex.getMessage)
    }
  }

  /**
   * function taskDef.distributed(taskObject)
   *  - 参数：
   *     taskObject: Object, 由taskDef解码得到的对象
   *  - 返回值：无
   */
  def distributed(taskObject: JsObject) {
    try {
      if (distributedMethod.isDefined) distributedMethod.get.execute(invokeJSFunc("stringToJson", PrettyPrinter(taskObject)))
    } catch handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else ex.getMessage)
  }

  /**
   * function taskDef.promised(taskResult)
   *  - 参数：
   *     taskResult: Object, 重写后的taskOrder
   *  - 返回值：未确定类型, 返回任务的执行结果taskResult或任意可能的值
   */
  def promised(taskResult: Any): AnyRef = { // taskResult是nashorn的返回值，应该是js compatible的
    try {
      if (promisedMethod.isDefined) promisedMethod.get.execute(taskResult.asInstanceOf[AnyRef])
      else taskResult.asInstanceOf[AnyRef]
    } catch handelEx(ex => if (rejectedMethod.isDefined) rejected(ex.getMessage) else ex.getMessage)
  }

  /**
   * function taskDef.rejected(reason)
   *  - 参数：
   *     reason: JSON_Supported, 一个描述错误的值或对象
   *  - 返回值：未确定类型, 返回任务的执行结果taskResult或任意可能的值
   *
   *  它可以在每次taskDef执行出错时得到一次处置机会。
   */
  def rejected(reason: Object): AnyRef = {
    try {
      if (rejectedMethod.isDefined) rejectedMethod.get.execute(reason)
      else reason
    } catch handelEx(ex => ex.getMessage)
  }

  def toCompatibleJsObj = invokeJSFunc("stringToJson", PrettyPrinter(taskDef)) // todo: remove hardcode
}
