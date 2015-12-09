package n4c.pedt.core

import n4c.pedt.context.HttpContext
import n4c.pedt.util.Conversions.jsValueToJava
import n4c.pedt.util.Utility.toUrlQueryFormat
import n4c.pedt.util.{Conversions, ScopeProxy, TaskProxy}
import org.slf4j.LoggerFactory
import spray.json.JsValue

import scala.concurrent.Future

object PEDT {
  type RemoteResult = JsValue // http json result
  type LocalResult = AnyRef //  returned type of nashorn methods

  private val log = LoggerFactory.getLogger(PEDT.getClass)

  import concurrent.ExecutionContext.Implicits.global

  val executeTaskUrlFormat = ""

  def queryScope(scope: String): Option[Scope] = ScopeProxy.get(scope)

  def fetchTask(taskId: String): Option[Task] = TaskProxy.get(taskId)

  /**
   * @param task: String/Function/Object, 以"task:"为前缀的字符串，或函数，或对象。
   */
  def run(task: String, args: JsValue*): Future[LocalResult] = {
    Script(task) map {
      _.execute(args.map(Conversions.jsValueToJava): _*)
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def runTask(taskId: String, args: Map[String, JsValue]): Future[LocalResult] = {
    TaskProxy.get(taskId) map {
      _.execute(args)
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def executeTask = run _

  def map(scope: String, taskId: String, args: Map[String, JsValue]): Future[Seq[RemoteResult]] =
    queryScope(scope) map {
      PEDT.map(_, taskId, args)
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }

  def map(scope: Scope, taskId: String, args: Map[String, JsValue]): Future[Seq[RemoteResult]] = {
    import HttpContext.{httpClient, request}
    val x = scope.getResources map { res =>
      val kvs = toUrlQueryFormat(args)
      if (kvs == "") request(s"${res}execute_task:$taskId")
      else request(s"${res}execute_task:$taskId?$kvs")
    }
    Future.sequence(x)
  }

  def mapEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]]): Future[Seq[RemoteResult]] = {
    import HttpContext._
    queryScope(scope) map { scope =>
      val resources: Seq[String] = scope.getResources
      val x = argsSeq.zipWithIndex map { args =>
        val kvs = toUrlQueryFormat(args._1)
        if (kvs == "") request(s"${resources(args._2 % resources.length)}execute_task:$taskId")
        else request(s"${resources(args._2 % resources.length)}execute_task:$taskId?$kvs")
      }
      Future.sequence(x)
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def reduce(scope: String, taskId: String, args: Map[String, JsValue], reduce: Option[String]): Future[LocalResult] = {
    val futureOfMapped = map(scope, taskId, args)
    reduce flatMap { x =>
      Script(x) map { y => futureOfMapped map { z =>
        y.execute(z.map(jsValueToJava))}
      }
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def reduceEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]], reduce: Option[String]): Future[LocalResult] = {
    val futureOfMapped = mapEach(scope, taskId, argsSeq)
    reduce flatMap { x =>
      Script(x) map { y => futureOfMapped map {
        z => y.execute(z.map(jsValueToJava))}
      }
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def daemon(scope: String, taskId: String, daemonTask: String, daemonArgs: JsValue*): Future[Seq[RemoteResult]] = {
    run(daemonTask, daemonArgs: _*).map(Conversions.nashornToJsValue) flatMap { x =>
      if(x.isDefined) map(scope, taskId, Map[String, JsValue]("x" -> x.get))
      else Future {
        throw new IllegalStateException("sss")
      }
    }
  }
}
