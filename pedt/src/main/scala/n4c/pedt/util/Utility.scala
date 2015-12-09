package n4c.pedt.util

import java.net.URLEncoder
import java.util.concurrent.TimeoutException

import org.slf4j.LoggerFactory
import play.libs.Akka
import spray.json._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

object Utility {
  private val log = LoggerFactory.getLogger(Utility.getClass)
  import concurrent.ExecutionContext.Implicits.global

  sealed trait TimeBounded[T] {
    def withTimeout(after: Duration): Option[T]
  }

  /**
   * wrap the future in a promise, and use a timer to fail the promise if timeout, 
   * then try to wait the value of the p.future
   */
  private[pedt] implicit def TimeBoundedFuture[T](f: Future[T]): TimeBounded[T] = new TimeBounded[T] {
    private val p = Promise[T]()

    def withTimeout(after: Duration): Option[T] = {
      val timer = Akka.system.scheduler.scheduleOnce(after.toMillis.millis) {
        p.tryFailure(new TimeoutException("timeout, promise failed."))
      }

      f.onComplete {
        case Success(x) => timer.cancel();
        case Failure(ex) => log.error(s"future failed, ex: ${ex.getMessage}")
      }

      p.completeWith(f)

      try {
        Some(Await.result(p.future, after + 1.second))
      } catch {
        case ex: Throwable =>
          log.error("await result failed, ex: " + ex.getMessage)
          None
      }
    }
  }

  private[pedt] def toUrlQueryFormat(args: Map[String, JsValue]): String = {
    for (kv <- args)
      yield s"${kv._1}=${URLEncoder.encode(CompactPrinter(kv._2), "UTF-8")}"
  }.mkString("&")
}
