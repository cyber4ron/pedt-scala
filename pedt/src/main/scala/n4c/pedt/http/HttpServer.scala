package n4c.pedt.http

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._

class ServiceActor extends Actor with ServerRoute {
  implicit val system = context.system
  def actorRefFactory = context
  def receive = runRoute(route)
}

class HttpServer(host: String, port: Int) {
  def start() {
    implicit val system = ActorSystem("pedt-spray-can")
    val service = system.actorOf(Props(classOf[ServiceActor]), "pedt-http-server")
    implicit val timeout = Timeout(5.seconds)
    IO(Http) ! Http.Bind(service, interface = host, port = port)
  }
}
