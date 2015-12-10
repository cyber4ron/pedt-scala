package n4c.pedt.http

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.{Directives, Route}
import n4c.pedt.core.PEDT
import n4c.pedt.util.ScopeProxy
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.immutable.HashMap
import scala.concurrent.duration._

class ServerRoute extends Directives {
  private val log = LoggerFactory.getLogger(classOf[ServerRoute])

  def route(): Route = {
    path("ping") {
      get {
        complete("pong!")
      }
    } ~ path("ping2") { // directive -> route(request -> future) ?
      get {
        complete {
          implicit def sprayJsValueMarshaller(implicit printer: JsObject => String = PrettyPrinter): ToResponseMarshaller[JsObject] =
            Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer)
          JsObject(HashMap("1" -> JsString("10")))
        }
      }
    } ~ path("execute_task:" ~ "^([a-z0-9]{32})$".r) { taskId =>
      get {
        import MarshallingSupport.AnyRefMarshaller
        parameterSeq { params =>
          complete {
            import n4c.pedt.util.Utility._
            val x = PEDT.runTask(taskId, params.filter(_._1 != "").map(x => x._1 -> {
              try {
                x._2.parseJson
              } catch {
                case _: Throwable => JsString(x._2)
              }
            }).toMap)
            val y = x waitWithin 1000.millis
            if(y.isEmpty) {
              Thread.sleep(1)
            }
            log.info(s"to marshal and response, x: $x, y: $y")
            y
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
