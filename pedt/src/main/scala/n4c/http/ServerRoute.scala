package n4c.http

import akka.http.scaladsl.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.{ Directives, Route }
import n4c.core.PEDT
import spray.json._

import scala.collection.immutable.HashMap

class ServerRoute extends Directives {

  def route(pedt: PEDT): Route = {
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
        import MarshallingSupport._
        parameterSeq { params =>
          complete {
            pedt.runTask(taskId, params.filter(_._1 != "").map(x => x._1 -> {
              try {
                val xx = x._2.parseJson
                xx
              } catch {
                case _: Throwable => JsString(x._2)
              }
            }).toMap).getOrElse("undefined").asInstanceOf[AnyRef]
          }
        }
      }
    }
  }
}
