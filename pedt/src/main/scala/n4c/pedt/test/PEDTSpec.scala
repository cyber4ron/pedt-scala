package n4c.pedt.test

import com.typesafe.config.ConfigFactory
import n4c.pedt.core.PEDT
import n4c.pedt.http.HttpServer
import n4c.pedt.util.Conversions
import spray.json.{JsValue, JsNumber, JsString}

import scala.concurrent.Future

/**
 * @author fenglei@wandoujia.com on 15/12/3.
 */

object PEDTSpec extends App {
  import scala.concurrent.ExecutionContext.Implicits.global // non-blocking

  Future {
    new MockN4CService.HttpServer().start()
  }

  Future {
    new HttpServer("127.0.0.1", 8083).start()
  }

  Future {
    new HttpServer("127.0.0.1", 8084).start()
  }

  Future {
    new HttpServer("127.0.0.1", 8085).start()
  }

  // Thread.sleep(1000 * 1000)

  // val xxxx = Arguments.mix(Some(Arguments(Map("1" -> JsNumber("3")))), Map("1" -> JsNumber("4")))

  // connection count
  PEDT.map("n4c:/a/b/c/sink:*", "30d9f7c5c9eb1a52c41af0bd4e2d835b", Map.empty[String, JsValue]) // map, declare var
  PEDT.map("n4c:/a/b/c/sink:*", "50428e7e8797cbab92c39cefbf0c8f88", Map("1" -> JsNumber("1"))) // map acc
  PEDT.map("n4c:/a/b/c/sink:*", "50428e7e8797cbab92c39cefbf0c8f88", Map("1" -> JsNumber("2"))) // map acc
  PEDT.map("n4c:/a/b/c/sink:*", "50428e7e8797cbab92c39cefbf0c8f88", Map("1" -> JsNumber("3"))) // map acc
  PEDT.map("n4c:/a/b/c/sink:*", "50428e7e8797cbab92c39cefbf0c8f88", Map("1" -> JsNumber("4"))) // map dec
  val x = PEDT.map("n4c:/a/b/c/sink:*", "297bcc44afe6d4c42751d6682312e2e4", Map.empty[String, JsValue]) // get var

  // word count
  val mapped = PEDT.mapEach("n4c:/a/b/c/map:*", "b6dc1169078fc4da7913fa153e47e1a5",
    Array(Map("1" -> JsString("a b c c")),
      Map("1" -> JsString("a b c c")),
      Map("1" -> JsString("c c")),
      Map("1" -> JsString("c c")),
      Map("1" -> JsString("a c"))))

  import spray.json._
  val mappedJsValue = JsArray(mapped.map(x => Conversions.nashornToString(Some(x)).parseJson): _*)
  val y = PEDT.run("script:javascript:base64:ZnVuY3Rpb24gd29yZF9jb3VudF9yZWR1Y2UoZGljdHMpIHsKICAgIHByaW50KGRpY3RzKTsKICAgIHByaW50KGRpY3RzLnNpemUoKSk7CiAgICB2YXIgcmVzdWx0ID0ge307CiAgICBmb3IodmFyIGkgPSAwOyBpIDwgZGljdHMuc2l6ZSgpOyBpKyspIHsKICAgICAgICBwcmludChKU09OLnN0cmluZ2lmeShkaWN0c1tpXSkpOwogICAgICAgIGZvcih2YXIgd29yZCBpbiBkaWN0c1tpXSkgewogICAgICAgICAgICBwcmludCh3b3JkKTsKICAgICAgICAgICAgaWYod29yZCBpbiByZXN1bHQpIHJlc3VsdFt3b3JkXSs9ZGljdHNbaV1bd29yZF07CiAgICAgICAgICAgIGVsc2UgcmVzdWx0W3dvcmRdID0gZGljdHNbaV1bd29yZF07CiAgICAgICAgfQogICAgfQogICAgcHJpbnQoSlNPTi5zdHJpbmdpZnkocmVzdWx0KSk7CiAgICByZXR1cm4gcmVzdWx0Owp9Cg==",
    mappedJsValue)

  val z = PEDT.reduce("n4c:/a/b/c/map:*",
    "b6dc1169078fc4da7913fa153e47e1a5",
    Map("1" -> JsString("a b c c")),
    Some("script:javascript:base64:ZnVuY3Rpb24gd29yZF9jb3VudF9yZWR1Y2UoZGljdHMpIHsKICAgIHByaW50KGRpY3RzKTsKICAgIHByaW50KGRpY3RzLnNpemUoKSk7CiAgICB2YXIgcmVzdWx0ID0ge307CiAgICBmb3IodmFyIGkgPSAwOyBpIDwgZGljdHMuc2l6ZSgpOyBpKyspIHsKICAgICAgICBwcmludChKU09OLnN0cmluZ2lmeShkaWN0c1tpXSkpOwogICAgICAgIGZvcih2YXIgd29yZCBpbiBkaWN0c1tpXSkgewogICAgICAgICAgICBwcmludCh3b3JkKTsKICAgICAgICAgICAgaWYod29yZCBpbiByZXN1bHQpIHJlc3VsdFt3b3JkXSs9ZGljdHNbaV1bd29yZF07CiAgICAgICAgICAgIGVsc2UgcmVzdWx0W3dvcmRdID0gZGljdHNbaV1bd29yZF07CiAgICAgICAgfQogICAgfQogICAgcHJpbnQoSlNPTi5zdHJpbmdpZnkocmVzdWx0KSk7CiAgICByZXR1cm4gcmVzdWx0Owp9Cg=="))

  val a = PEDT.reduceEach("n4c:/a/b/c/map:*",
    "b6dc1169078fc4da7913fa153e47e1a5",
    Array(Map("1" -> JsString("a b c c")),
      Map("1" -> JsString("a b c c")),
      Map("1" -> JsString("c c")),
      Map("1" -> JsString("c c")),
      Map("1" -> JsString("a c"))),
    Some("script:javascript:base64:ZnVuY3Rpb24gd29yZF9jb3VudF9yZWR1Y2UoZGljdHMpIHsKICAgIHByaW50KGRpY3RzKTsKICAgIHByaW50KGRpY3RzLnNpemUoKSk7CiAgICB2YXIgcmVzdWx0ID0ge307CiAgICBmb3IodmFyIGkgPSAwOyBpIDwgZGljdHMuc2l6ZSgpOyBpKyspIHsKICAgICAgICBwcmludChKU09OLnN0cmluZ2lmeShkaWN0c1tpXSkpOwogICAgICAgIGZvcih2YXIgd29yZCBpbiBkaWN0c1tpXSkgewogICAgICAgICAgICBwcmludCh3b3JkKTsKICAgICAgICAgICAgaWYod29yZCBpbiByZXN1bHQpIHJlc3VsdFt3b3JkXSs9ZGljdHNbaV1bd29yZF07CiAgICAgICAgICAgIGVsc2UgcmVzdWx0W3dvcmRdID0gZGljdHNbaV1bd29yZF07CiAgICAgICAgfQogICAgfQogICAgcHJpbnQoSlNPTi5zdHJpbmdpZnkocmVzdWx0KSk7CiAgICByZXR1cm4gcmVzdWx0Owp9Cg=="))

  //
    val b = PEDT.run("", JsArray(JsNumber(22), JsString("zzz")))
    // val c = client.executeTask("", JsArray(JsNumber(22), JsString("zzz")))
    val d = PEDT.runTask("", Map("1" -> JsBoolean(true), "2" -> JsObject("x" -> JsTrue)))

  // illegal input

  // wrong scope, invalid query result
  // wrong taskId, invalid task def
  // wrong executable
  // wrong argument(type, etc.). todo: 在本地执行脚本时，会直接传java对象，做层隔离(可以传JSValue)

  // js engine会维护状态，不要挂。todo: 失败恢复
  // js engine线程安全性，函数幂等性

  // 不要传播异常出client？

  //

  Thread.sleep(1000 * 1000)
}
