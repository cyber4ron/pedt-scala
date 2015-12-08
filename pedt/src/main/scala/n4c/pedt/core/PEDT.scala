package n4c.pedt.core

import com.typesafe.config.Config
import PEDT.{ LocalResult, RemoteResult }
import n4c.pedt.context.HttpContext
import n4c.pedt.util.{Utility, Conversions, TaskProxy, ScopeProxy}
import Conversions.jsValueToJava
import Utility.toUrlQueryFormat
import n4c.util.Conversions
import org.slf4j.LoggerFactory
import spray.json.JsValue

import scala.collection.JavaConversions._
import scala.concurrent.{ Await, TimeoutException }

object PEDT {
  type RemoteResult = JsValue
  type LocalResult = Any

  val executeTaskUrlFormat = ""

  def apply(config: Config) = new PEDT(config)
}

class PEDT(config: Config) {
  val log = LoggerFactory.getLogger(PEDT.getClass)

  def queryScope(scope: String): Option[Scope] = ScopeProxy.get(scope)

  def fetchTask(taskId: String): Option[Task] = TaskProxy.get(taskId)

  /**
   * @param task: String/Function/Object, 以"task:"为前缀的字符串，或函数，或对象。
   */
  def run(task: String, args: JsValue*): Option[LocalResult] = Script(task).map(_.execute(args.map(Conversions.jsValueToJava): _*))

  def runTask(taskId: String, args: Map[String, JsValue]): Option[LocalResult] = TaskProxy.get(taskId).map(_.execute(args))

  def executeTask = run _

  def map(scope: String, taskId: String, args: Map[String, JsValue]): Seq[RemoteResult] = {
    import HttpContext.{ unmarshalTimeoutMs, httpClient, request }
    queryScope(scope) map { scope =>
      scope.getResources map { res =>
        val kvs = toUrlQueryFormat(args)
        if (kvs == "") request(s"${res}execute_task:$taskId")
        else request(s"${res}execute_task:$taskId?$kvs")
      } map { future =>
        try {
          Some(Await.result(future, unmarshalTimeoutMs))
        } catch {
          case _: TimeoutException => None
          case _: Throwable        => None
        }
      } filter (_.isDefined) map (_.get)
    } getOrElse Array.empty[JsValue].toSeq
  }

  def mapEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]]): Seq[RemoteResult] = {
    import HttpContext._
    queryScope(scope) map { scope =>
      val resources: Seq[String] = scope.getResources
      argsSeq.zipWithIndex map { args =>
        val kvs = toUrlQueryFormat(args._1)
        if (kvs == "") request(s"${resources(args._2 % resources.length)}execute_task:$taskId")
        else request(s"${resources(args._2 % resources.length)}execute_task:$taskId?$kvs")
      } map { future =>
        try {
          Some(Await.result(future, unmarshalTimeoutMs))
        } catch {
          case _: TimeoutException => None
          case _: Throwable        => None
        }
      } filter (_.isDefined) map (_.get)
    } getOrElse Array.empty[JsValue].toSeq
  }

  def reduce(scope: String, taskId: String, args: Map[String, JsValue], reduce: Option[String]): Option[LocalResult] = {
    val mapped = map(scope, taskId, args)
    import collection.JavaConversions._
    reduce flatMap { x =>
      Script(x) map { y => Some(y.execute(seqAsJavaList(mapped.map(jsValueToJava)))) }
    } getOrElse None
  }

  def reduceEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]], reduce: Option[String]): Option[LocalResult] = {
    val mapped = mapEach(scope, taskId, argsSeq)
    reduce flatMap { x =>
      Script(x) map { y => Some(y.execute(seqAsJavaList(mapped.map(jsValueToJava)))) }
    } getOrElse None
  }

  /**
   * function task.daemon(distributionScope, taskId, daemon, daemonArgs)
   *  - 参数：
   *     distributionScope, task: (参见task.map)
   *     daemon: function, 在task.map行为之前调用的函数
   *     daemonArgs: Object, 使用task.run()来调用daemon时传入的参数
   *  - 返回值：Array，是调用task.map()之后的结果。
   */
  def daemon(scope: String, taskId: String, daemonTask: String, daemonArgs: JsValue*): Option[Seq[RemoteResult]] = {
    import Conversions._
    run(daemonTask, daemonArgs: _*).map(x => map(scope, taskId, Map("x" -> x)))
  }
}
