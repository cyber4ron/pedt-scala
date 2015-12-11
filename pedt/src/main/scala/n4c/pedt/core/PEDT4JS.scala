package n4c.pedt.core

import scala.concurrent.Future

import org.slf4j.LoggerFactory
import spray.json._

import n4c.pedt.util.{Conversion, Utility}

object PEDT4JS {
  private val log = LoggerFactory.getLogger(PEDT.getClass)
  import concurrent.ExecutionContext.Implicits.global

  def queryScope(scope: String): Array[String] = PEDT.queryScope(scope).map(_.getResources.toArray).getOrElse(Array.empty[String])

  def fetchTask(taskId: String): String = PEDT.fetchTask(taskId).map(x => PrettyPrinter(x.taskDef)).getOrElse("undefined")

  def run(task: String, args: String): Future[String] = PEDT.run(task, args.parseJson.asJsObject.fields.values.toSeq: _*)
                                                        .map(x => Conversion.nashornToString(x))

  def runTask(taskId: String, args: String): Future[String] = PEDT.runTask(taskId, args.parseJson.asJsObject.fields)
                                                              .map(x => Conversion.nashornToString(x))

  def executeTask = runTask _

  def map(scope: String, taskId: String, args: String): Future[Array[String]] = {
    val fs = PEDT.map(scope, taskId, args.parseJson.asJsObject.fields)
    fs.map(seq => seq.map(jsV => PrettyPrinter(jsV)).toArray)
  }

  def reduce(scope: String, taskId: String, args: String, reduceTask: String): Future[String] =
    PEDT.reduce(scope, taskId, args.parseJson.asJsObject.fields, reduceTask)
    .map(x => Conversion.nashornToString(x))

  def daemon(scope: String, taskId: String, daemonTask: String, daemonArgs: String): Future[Array[String]] = {
    val fs = PEDT.daemon(scope, taskId, daemonTask, daemonArgs.parseJson.asJsObject.fields.values.toSeq: _*)
    fs.map(seq => seq.map(jsV => PrettyPrinter(jsV)).toArray)
  }

  def subscribe(scope: String) = PEDT.subscribe(scope)

  def waitWithin[T](future: Future[T], durationMs: Long): Any = {
    import Utility.TimeBoundedFuture; import scala.concurrent.duration._
    try {
      future waitWithin durationMs.millis
    } catch {
      case ex: Throwable => log.warn(s"wait future failed, ex: ${ex.getMessage}")
    }
  }
}
