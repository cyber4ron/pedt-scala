package com.wandoujia.n4c.pedt.core

import scala.concurrent.Future

import org.slf4j.LoggerFactory
import spray.json._

import com.wandoujia.n4c.pedt.util.Utility

object PEDT4JS {
  private val log = LoggerFactory.getLogger(PEDT4JS.getClass)
  import concurrent.ExecutionContext.Implicits.global

  def queryScope(scope: String): AnyRef = PEDT.queryScope(scope).map(_.getResources.toArray).getOrElse("undefined")

  def fetchTask(taskId: String): String = PEDT.fetchTask(taskId).map(x => PrettyPrinter(x.taskDef)).getOrElse("undefined")

  def run(task: String, args: String): Future[PEDT.LocalResult] = {
    log.info(s"running task[$task], args: $args")
    PEDT.run(task, args.stripPrefix("\"").stripSuffix("\"").replace("\\", "").parseJson.asJsObject.fields.values.toSeq: _*)
  }

  def runTask(taskId: String, args: String): Future[PEDT.LocalResult] = PEDT.runTask(taskId, args.stripPrefix("\"").stripSuffix("\"").replace("\\", "").parseJson.asJsObject.fields)

  def map(scope: String, taskId: String, args: String): Future[Array[PEDT.LocalResult]] =
    PEDT.map(scope, taskId, args.stripPrefix("\"").stripSuffix("\"").replace("\\", "").parseJson.asJsObject.fields).map(seq => seq.toArray)

  def reduce(scope: String, taskId: String, args: String, reduceTask: String): Future[PEDT.LocalResult] =
    PEDT.reduce(scope, taskId, args.stripPrefix("\"").stripSuffix("\"").replace("\\", "").parseJson.asJsObject.fields, reduceTask)

  def daemon(scope: String, taskId: String, daemonTask: String, daemonArgs: String): Future[Array[PEDT.LocalResult]] =
    PEDT.daemon(scope, taskId, daemonTask, daemonArgs.stripPrefix("\"").stripSuffix("\"").replace("\\", "").parseJson.asJsObject.fields).map(seq => seq.toArray)

  def subscribe(scope: String) {
    PEDT.subscribe(scope)
  }

  def waitWithin[T](future: Future[T], durationMs: Long): Any = {
    import Utility.TimeBoundedFuture

    import scala.concurrent.duration._
    try {
      future waitWithin durationMs.millis getOrElse "undefined"
    } catch {
      case ex: Throwable => log.warn(s"wait future failed, ex: ${ex.getMessage}")
    }
  }
}
