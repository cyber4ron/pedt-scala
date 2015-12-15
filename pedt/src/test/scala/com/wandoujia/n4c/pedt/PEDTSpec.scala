package com.wandoujia.n4c.pedt

import java.security.MessageDigest
import java.util.Base64

import com.typesafe.config.ConfigFactory
import com.wandoujia.n4c.pedt.core.PEDT
import com.wandoujia.n4c.pedt.http.HttpServer
import com.wandoujia.n4c.pedt.util.Conversion
import com.wandoujia.n4c.pedt.util.Utility._
import org.scalatest.{ BeforeAndAfterAll, WordSpec, run }
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object PEDTSpec extends App {
  run(new PEDTSpec()) // for IDE test only
}

class PEDTSpec extends WordSpec with BeforeAndAfterAll {
  private val log = LoggerFactory.getLogger(classOf[PEDTSpec])

  var n4cService: MockN4CService.HttpServer = _
  var pedtWorker1: HttpServer = _
  var pedtWorker2: HttpServer = _
  var pedtWorker3: HttpServer = _

  log.info(Console.YELLOW + "JS_HELPER_SCRIPT: " + sys.env("JS_HELPER_SCRIPT") + Console.RESET)
  log.info(Console.YELLOW + "JS_PEDT_SCRIPT: " + sys.env("JS_PEDT_SCRIPT") + Console.RESET)
  log.info(Console.YELLOW + "WORKING DIR: " + new java.io.File(".").getCanonicalPath + Console.RESET)

  override def beforeAll() {
    Future {
      n4cService = new MockN4CService.HttpServer()
      n4cService.start()
    }

    Future {
      pedtWorker1 = new HttpServer(ConfigFactory.parseString("""web {
                                                               |  host = "127.0.0.1"
                                                               |  port = 8083
                                                               |}
                                                               |
                                                               |http {
                                                               |  request.timeoutMs = 5000
                                                               |  response.timeoutMs = 5000
                                                               |  unmarshal.timeoutMs = 1000
                                                               |}
                                                               |""".stripMargin))
      pedtWorker1.start()
    }

    Future {
      pedtWorker2 = new HttpServer(ConfigFactory.parseString("""web {
                                                               |  host = "127.0.0.1"
                                                               |  port = 8084
                                                               |}
                                                               |
                                                               |http {
                                                               |  request.timeoutMs = 5000
                                                               |  response.timeoutMs = 5000
                                                               |  unmarshal.timeoutMs = 1000
                                                               |}
                                                               |""".stripMargin))
      pedtWorker2.start()
    }

    Future {
      pedtWorker3 = new HttpServer(ConfigFactory.parseString("""web {
                                                               |  host = "127.0.0.1"
                                                               |  port = 8085
                                                               |}
                                                               |
                                                               |http {
                                                               |  request.timeoutMs = 5000
                                                               |  response.timeoutMs = 5000
                                                               |  unmarshal.timeoutMs = 1000
                                                               |}
                                                               |""".stripMargin))
      pedtWorker3.start()
    }
  }

  override def afterAll() {
    n4cService.stop()
    pedtWorker1.stop()
    pedtWorker2.stop()
    pedtWorker3.stop()
  }

  "pedt.download_task" should {
    "pass fetchTask test" in {
      val v = PEDT.fetchTask("30bf98606c585eef4ba24537231df3fb")
      assert(PrettyPrinter(v.get.taskDef) == """{
                                               |  "x": {
                                               |    "run": "script:javascript:base64:ZnVuY3Rpb24gdGVzdCh4KSB7CiAgICBwcmludCh4KTsKICAgIHJldHVybiB4Owp9Cg=="
                                               |  }
                                               |}""".stripMargin, "fetchTask failed.")
    }
  }

  "pedt.query" should {
    "pass queryScope test" in {
      val v = PEDT.queryScope("n4c:/a/b/c/map:*")
      assert(v.get.getResources.sorted == Seq("http://127.0.0.1:8083/", "http://127.0.0.1:8084/", "http://127.0.0.1:8085/"), "queryScope failed.")
    }
  }

  "pedt.run / execute_task" should {
    "complete runTas testk" in {
      val v = PEDT.runTask("30bf98606c585eef4ba24537231df3fb", Map("1" -> JsBoolean(true))) waitWithin 1.second
      assert(Conversion.nashornToString(v.get) == "true", "runTask failed.")
    }
  }

  "pedt.map / run / execute_task" should {
    "pass connection count test" in {
      PEDT.map("n4c:/a/b/c/sink:*", "30d9f7c5c9eb1a52c41af0bd4e2d835b", Map.empty[String, JsValue]) // map, declare var
      Thread.sleep(100)

      PEDT.map("n4c:/a/b/c/sink:*", "3c4b2ad93a3b98279c95e33a18eb994b", Map("1" -> JsNumber("1"))) // map acc
      PEDT.map("n4c:/a/b/c/sink:*", "3c4b2ad93a3b98279c95e33a18eb994b", Map("1" -> JsNumber("2"))) // map acc
      PEDT.map("n4c:/a/b/c/sink:*", "3c4b2ad93a3b98279c95e33a18eb994b", Map("1" -> JsNumber("3"))) // map acc
      PEDT.map("n4c:/a/b/c/sink:*", "3c4b2ad93a3b98279c95e33a18eb994b", Map("1" -> JsNumber("-1"))) // map dec

      Thread.sleep(100)
      val v = PEDT.map("n4c:/a/b/c/sink:*", "2543693a4c27f3c97e2c2f9cca4000ea", Map.empty[String, JsValue]) waitWithin 1.seconds // get var
      val num = v.get.map(x => x.asInstanceOf[JsNumber].value.toInt).head
      assert(num >= -1 && num <= 6, "connection count failed.") // 正常是5，但nashorn并行执行，有随机性。
    }

    "pass word count test" in {
      val mapped = PEDT.mapEach("n4c:/a/b/c/map:*", "893d7569b4c01d8c8b6d3e053ebafb66",
        Array(Map("1" -> JsString("a b c c")),
          Map("1" -> JsString("a b c c")),
          Map("1" -> JsString("c c")),
          Map("1" -> JsString("c c")),
          Map("1" -> JsString("a c"))))

      val mappedJsValue = mapped.map(x => JsArray(x.map(y => Conversion.nashornToString(y).parseJson): _*))
      val v = mappedJsValue.flatMap { values =>
        PEDT.run(s"script:javascript:base64:${Base64.getEncoder.encodeToString(io.Source.fromFile("src/test/resources/word_count_reduce.js").mkString.getBytes)}", values)
      } waitWithin 1.second

      assert(Conversion.nashornToString(v.get) == """{"a":3,"b":2,"c":9}""", "word count failed.")
    }
  }

  "pedt.reduce" should {
    "pass reduce test" in {
      val f = PEDT.reduce("n4c:/a/b/c/map:*",
        "893d7569b4c01d8c8b6d3e053ebafb66",
        Map("1" -> JsString("a b c c")),
        "script:javascript:base64:" + Base64.getEncoder.encodeToString(io.Source.fromFile("src/test/resources/word_count_reduce.js").mkString.getBytes))

      val v = f waitWithin 1.second
      assert(Conversion.nashornToString(v.get) == """{"a":3,"b":3,"c":6}""", "reduce failed.")
    }

    "complete reduceEach test" in {
      val f = PEDT.reduceEach("n4c:/a/b/c/map:*",
        "893d7569b4c01d8c8b6d3e053ebafb66",
        Array(Map("1" -> JsString("a b c c")),
          Map("1" -> JsString("a b c c")),
          Map("1" -> JsString("c c")),
          Map("1" -> JsString("c c")),
          Map("1" -> JsString("a c"))),
        "script:javascript:base64:" + Base64.getEncoder.encodeToString(io.Source.fromFile("src/test/resources/word_count_reduce.js").mkString.getBytes))

      val v = f waitWithin 1.second
      assert(Conversion.nashornToString(v.get) == """{"a":3,"b":2,"c":9}""", "reduceEach failed.")
    }
  }

  "pedt.daemon" should {
    "complete daemon test" in {
      val v = PEDT.daemon("n4c:/a/b/c/map:*", "30bf98606c585eef4ba24537231df3fb", s"""script:javascript:function xxx(x) {print("in daemon."); return x;}""", Map("1" -> JsNumber(9))) waitWithin 1.second
      assert(v.get.map(x => x.asInstanceOf[JsNumber].value.toInt) == Vector(9, 9, 9), "daemon failed.")
      ""
    }
  }

  "pedt.subscribe" should {
    "pass subscribe test" in {
      PEDT.subscribe("n4c:/a/b/c/map")
      val v1 = PEDT.map("n4c:/a/b/c/map:*", "30bf98606c585eef4ba24537231df3fb", Map("1" -> JsNumber(5))) waitWithin 1.seconds // get var
      assert(v1.get.map(x => x.asInstanceOf[JsNumber].value.toInt) == Vector(5, 5, 5), "subscribe test failed.")

      import com.wandoujia.n4c.pedt.context.HttpContext._
      val resp = request("http://127.0.0.1:8089/lost_worker:127.0.0.1%3A8084") waitWithin 1.second
      assert(resp.get.asInstanceOf[JsObject].toString(CompactPrinter) == """{"status":"ok"}""", "mock lost worker failed.")

      Thread.sleep(1000L)

      val v2 = PEDT.map("n4c:/a/b/c/map:*", "30bf98606c585eef4ba24537231df3fb", Map("1" -> JsNumber(5))) waitWithin 1.seconds // get var
      assert(v2.get.map(x => x.asInstanceOf[JsNumber].value.toInt) == Vector(5, 5), "subscribe test failed.")
    }
  }

  "pedt.register" should {
    "pass registerTask test" in {
      val json = """{"x":{"run":"script:javascript:function xx(){print(\"ok.\");}"}}"""
      val taskId = MessageDigest.getInstance("MD5").digest(json.getBytes).map("%02X".format(_)).mkString.toLowerCase
      PEDT.registerTask(taskId, json.parseJson)

      println(taskId)
      val x = PEDT.fetchTask(taskId)
      val taskJson = PEDT.fetchTask(taskId).get.taskDef.toString(CompactPrinter)
      assert(json == taskJson, "register failed.")
    }
  }

  "call pedt interfaces from js" should { // nashorn里要invoke(function_name, args...)，所以目前不支持匿名(不过以后可以rewrite js以支持匿名)
    "pass task test" in {
      val v = PEDT.runTask("d71634053f6eeaa6b21eb2fe82506af4", Map.empty[String, JsValue]) waitWithin 1.second
      assert(Conversion.nashornToString(v.get).replace(" ", "") == "[{x=1},{x=1}]", "runTask failed.")
    }
  }
}
