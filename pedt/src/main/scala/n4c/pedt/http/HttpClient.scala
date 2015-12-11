package n4c.pedt.http

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import n4c.pedt.util.Utility.TimeBoundedFuture
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * has to be thread safe
 *
 */
class HttpClient(config: Config) {
  private val log = LoggerFactory.getLogger(classOf[HttpClient])

  implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = Timeout(15.seconds) // implicit execution context

  // implicit val x = 1000 * 1000L //

  val host = config.getString("web.host")
  val port = config.getInt("web.port")

  def request(uri: String,
              method: HttpMethod = HttpMethods.GET,
              entity: HttpEntity = HttpEntity.Empty): Future[HttpResponse] = method match {
    case HttpMethods.GET =>
      log.info(s"requesting: $uri")
      (IO(Http) ? HttpRequest(method, Uri(uri))).mapTo[HttpResponse]

    case HttpMethods.POST =>
      (IO(Http) ? HttpRequest(method, uri = uri, entity = entity)).mapTo[HttpResponse]
  }

  def requestBlock(uri: String,
                   method: HttpMethod = HttpMethods.GET,
                   entity: HttpEntity = HttpEntity.Empty,
                   waitMs: Long = 1000L): Option[HttpResponse] = {
    request(uri, method, entity) waitWithin waitMs.millis
  }
}
