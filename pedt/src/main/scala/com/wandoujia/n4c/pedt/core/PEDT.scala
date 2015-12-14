package com.wandoujia.n4c.pedt.core

import java.net.URLEncoder

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.http.{ HttpEntity, HttpMethods, MediaTypes }
import spray.json.{ CompactPrinter, JsValue }

import scala.collection.JavaConversions
import scala.concurrent.Future
import scala.concurrent.duration._

import com.wandoujia.n4c.pedt
import pedt.context.HttpContext.{ host, httpClient, port, request }
import pedt.util.{ ScopeProxy, TaskProxy }
import pedt.util.Utility.{ TimeBoundedFuture, toUrlQueryFormat }
import pedt.util.Conversion._

object PEDT {
  type RemoteResult = JsValue // http json result type
  type LocalResult = AnyRef // nashorn methods result type

  private val log = LoggerFactory.getLogger(PEDT.getClass)

  import concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load()
  val subscribeUrl = config.getString("n4c.service.subscribe")
  val registerTaskUrl = config.getString("n4c.service.register-task")
  val requestTimeout = config.getLong("http.request.timeoutMs").millis

  val subscribeTemplate = s"""{
                             |	"key": "%s",
                             |	"type": "scope",
                             |	"version": "1.1",
                             |	"receive": "http://%s:%s/notify:"
                             |}""".stripMargin

  // methods
  def queryScope(scope: String): Option[Scope] = {
    try {
      ScopeProxy.get(scope)
    } catch {
      case ex: Throwable =>
        log.warn(s"get scope failed, scope: $scope")
        None
    }
  }

  def fetchTask(taskId: String): Option[Task] = {
    try {
      TaskProxy.get(taskId)
    } catch {
      case ex: Throwable =>
        log.warn(s"get task failed, taskId: $taskId, ex: ${ex.getMessage}")
        None
    }
  }

  /**
   * @param task: script:javascript:...
   */
  def run(task: String, args: JsValue*): Future[LocalResult] = {
    Script(task) map {
      _.execute(args.map(jsValueToJava): _*)
    } getOrElse Future {
      throw new IllegalStateException(s"parse task failed, task:[$task]")
    }
  }

  def runTask(taskId: String, args: Map[String, JsValue]): Future[LocalResult] = {
    TaskProxy.get(taskId) map {
      _.execute(args)
    } getOrElse Future {
      throw new IllegalStateException(s"get task failed, taskId:[$taskId]")
    }
  }

  def executeTask = runTask _

  def map(scope: String, taskId: String, args: Map[String, JsValue]): Future[Seq[RemoteResult]] =
    queryScope(scope) map {
      PEDT.map(_, taskId, args)
    } getOrElse Future {
      throw new IllegalStateException(s"query scope failed, scope: [$scope]")
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
      throw new IllegalStateException(s"query scope failed, scope: [$scope]")
    }
  }

  def reduce(scope: String, taskId: String, args: Map[String, JsValue], reduceTask: String): Future[LocalResult] = {
    import JavaConversions._
    val futureOfMapped = map(scope, taskId, args)
    Script(reduceTask) map { script =>
      futureOfMapped flatMap { seq =>
        script.execute(seqAsJavaList(seq.map(jsValueToJava)))
      }
    } getOrElse Future {
      throw new IllegalStateException(s"parse reduceTask failed, reduceTask: [$reduceTask]")
    }
  }

  def reduceEach(scope: String, taskId: String, argsSeq: Seq[Map[String, JsValue]], reduceTask: String): Future[LocalResult] = {
    import JavaConversions._
    val futureOfMapped = mapEach(scope, taskId, argsSeq)
    Script(reduceTask) map { script =>
      futureOfMapped flatMap { seq =>
        script.execute(seqAsJavaList(seq.map(jsValueToJava)))
      }
    } getOrElse Future {
      throw new IllegalStateException(s"parse reduceTask failed, reduceTask: [$reduceTask]")
    }
  }

  def daemon(scope: String, taskId: String, daemonTask: String, daemonArgs: Map[String, JsValue]): Future[Seq[RemoteResult]] = {
    run(daemonTask, daemonArgs.values.toSeq: _*).map(nashornToJsValue) flatMap { x =>
      if (x.isDefined) map(scope, taskId, Map[String, JsValue]("x" -> x.get))
      else Future {
        throw new IllegalStateException(s"run daemon failed, taskId: [$taskId]")
      }
    }
  }

  def subscribe(scope: String) {
    val entity = HttpEntity(MediaTypes.`text/plain`, subscribeTemplate.format(scope, host, port))
    try {
      request(s"$subscribeUrl${URLEncoder.encode(scope, "UTF-8")}", HttpMethods.POST, entity) waitWithin requestTimeout
    } catch {
      case ex: Throwable => log.warn(s"subscribe $scope failed, ex: ${ex.getMessage}")
    }
  }

  def registerTask(taskId: String, taskDef: JsValue) = {
    val entity = HttpEntity(MediaTypes.`text/plain`, CompactPrinter(taskDef))
    try {
      request(s"$registerTaskUrl$taskId", HttpMethods.POST, entity) waitWithin requestTimeout
    } catch {
      case ex: Throwable => log.warn(s"register task failed, taskId: $taskId, ex: ${ex.getMessage}")
    }
  }
}
