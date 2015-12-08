package n4c.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import n4c.core.PEDT

class HttpServer(host: String, port: Int) {
  def start() {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val route = new ServerRoute().route(new PEDT(ConfigFactory.load())) // config?
    val serverRoute = Directives.respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*"))(route)

    val source = Http().bind(host, port)
    source.runForeach(_.handleWith(serverRoute))
  }
}
