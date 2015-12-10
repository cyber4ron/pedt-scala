package n4c.pedt.core

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpMethods, MediaTypes}
import n4c.pedt.context.HttpContext.{host, port, request}
import n4c.pedt.util.Conversions.jsValueToJava
import n4c.pedt.util.Utility.{toUrlQueryFormat, TimeBoundedFuture}
import n4c.pedt.util.{Conversions, ScopeProxy, TaskProxy}
import org.slf4j.LoggerFactory
import spray.json.JsValue

import scala.collection.JavaConversions
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

  def executeTask = runTask _

  def map(scope: String, taskId: String, args: Map[String, JsValue]): Future[Seq[RemoteResult]] =
    queryScope(scope) map {
      PEDT.map(_, taskId, args)
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }

  def map(scope: Scope, taskId: String, args: Map[String, JsValue]): Future[Seq[RemoteResult]] = {
    val x = scope.getResources map { res =>
      val kvs = toUrlQueryFormat(args)
      if (kvs == "") request(s"${res}execute_task:$taskId")
      else request(s"${res}execute_task:$taskId?$kvs")
    }
    Future.sequence(x)
  }

  def mapEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]]): Future[Seq[RemoteResult]] = {
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

  def reduce(scope: String, taskId: String, args: Map[String, JsValue], reduceTask: String): Future[LocalResult] = {
    import JavaConversions._
    val futureOfMapped = map(scope, taskId, args)
    Script(reduceTask) map { script =>
      futureOfMapped flatMap { seq =>
        script.execute(seqAsJavaList(seq.map(jsValueToJava)))}
    } getOrElse Future {
      throw new IllegalStateException("sss")
    }
  }

  def reduceEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]], reduceTask: String): Future[LocalResult] = {
    import JavaConversions._
    val futureOfMapped = mapEach(scope, taskId, argsSeq)
    Script(reduceTask) map { script =>
      futureOfMapped flatMap { seq =>
        script.execute(seqAsJavaList(seq.map(jsValueToJava)))}
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

  def subscribe(scope: String) {
    import scala.concurrent.duration._
    val content = s"""{
                  |	"key": "$scope",
                  |	"type": "scope",
                  |	"version": "1.1",
                  |	"receive": "http://$host:$port/notify"
                  |}""".stripMargin
    val entity = HttpEntity(ContentType(MediaTypes.`application/json`), content)
    request(s"subscribe?$scope", HttpMethods.POST, entity) waitWithin 1.second
  }
}
