package n4c.pedt.http

import n4c.pedt.core.PEDT
import n4c.pedt.util.{Marshalling, ScopeProxy}
import org.slf4j.LoggerFactory
import spray.json._
import spray.routing.HttpService
import Marshalling._
import concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

trait ServerRoute extends HttpService {
  private val log = LoggerFactory.getLogger(classOf[ServerRoute])

  def route = {
    path("ping") {
      get {
        complete("pong!")
      }
    } ~ path("execute_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
      get {
        parameterSeq { params =>
          complete {
            val f = PEDT.runTask(taskId, params.filter(_._1 != "").map(x => x._1 -> {
              try {
                x._2.parseJson
              } catch {
                case _: Throwable => JsString(x._2)
              }
            }).toMap)
            f onComplete {
              case Success(x) => log.info(s"task: [$taskId] succeed, result: $x")
              case Failure(ex) => log.warn(s"task: [$taskId] succeed, ex: ${ex.getMessage}")
            }
            f
          }
        }
      }
    } ~ path("notify:" ~ "^(.+)$".r) { scope =>
      get {
        parameterSeq { params =>
          complete {
            ScopeProxy.invalidate(scope)
            """{"status": "ok"}"""
          }
        }
      }
    }
  }
}
