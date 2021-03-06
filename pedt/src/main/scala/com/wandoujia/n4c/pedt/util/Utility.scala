package com.wandoujia.n4c.pedt.util

import java.net.URLEncoder
import java.util.concurrent.TimeoutException

import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * @author fenglei@wandoujia.com, 2015-12
 *
 */
object Utility {
  private val log = LoggerFactory.getLogger(Utility.getClass)
  import concurrent.ExecutionContext.Implicits.global

  sealed trait TimeBounded[T] {
    def waitWithin(time: Duration): Option[T]
  }

  import com.wandoujia.n4c.pedt.context.HttpContext.system

  /**
   * wrap the future in a promise, and use a timer to fail the promise if timeout,
   * then try to wait the value of the p.future
   */
  private[pedt] implicit def TimeBoundedFuture[T](f: Future[T]): TimeBounded[T] = new TimeBounded[T] {
    private val p = Promise[T]()

    def waitWithin(time: Duration): Option[T] = {
      val timer = system.scheduler.scheduleOnce(time.toMillis.millis) {
        p.tryFailure(new TimeoutException("timeout, promise failed."))
      }

      f.onComplete {
        case Success(x)  => timer.cancel();
        case Failure(ex) => log.error(s"future failed, ex: ${ex.getMessage}")
      }

      p.completeWith(f)

      try {
        Some(Await.result(p.future, time + 1.second)) //
      } catch {
        case ex: TimeoutException =>
          log.error("await result timeout, ex: " + ex.getMessage)
          None
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
