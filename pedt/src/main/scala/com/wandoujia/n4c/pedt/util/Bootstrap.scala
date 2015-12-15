package com.wandoujia.n4c.pedt.util

import com.typesafe.config.ConfigFactory
import com.wandoujia.n4c.pedt.http.HttpServer

/**
 * @author fenglei@wandoujia.com on 2015-12
 */
object Bootstrap extends App {
  val daemonWorker = new HttpServer(ConfigFactory.load())
  daemonWorker.start()
}
