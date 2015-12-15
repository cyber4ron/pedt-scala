package com.wandoujia.n4c.pedt.http

import akka.actor.{ Actor, Props }
import akka.io.IO
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.duration._

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
class ServiceActor extends Actor with ServerRoute {
  implicit val system = context.system
  def actorRefFactory = context
  def receive = runRoute(route)
}

object HttpServer {
  private val log = LoggerFactory.getLogger(HttpServer.getClass)
}

class HttpServer(host: String, port: Int, responseTimeoutMs: Long = 5000) {
  import HttpServer.log
  import com.wandoujia.n4c.pedt.context.HttpContext.system

  def this(config: Config) = this(config.getString("web.host"),
    config.getInt("web.port"),
    config.getLong("http.response.timeoutMs"))

  def this() = this(ConfigFactory.load())

  implicit val timeout = Timeout(responseTimeoutMs.millis)

  def start() {
    val listener = system.actorOf(Props(classOf[ServiceActor]), s"pedt-http-listener-$host-$port")
    IO(Http) ! Http.Bind(listener, interface = host, port = port)
    log.info(s"http server[$host:$port] started.")
  }

  def stop() {
    IO(Http) ! Http.Unbind
  }
}
