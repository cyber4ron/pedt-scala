package n4c.pedt.test

import java.security.MessageDigest

import scala.io.Source

/**
 * @author fenglei@wandoujia.com on 15/12/6.
 */

object Helper extends App {

  import java.util.Base64
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/task1.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/task2.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/task21.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/task3.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/task4.js").mkString.getBytes))

  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task1.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task2.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task21.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task3.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task4.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/task5.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)

  /**
   * 30d9f7c5c9eb1a52c41af0bd4e2d835b
   * 297bcc44afe6d4c42751d6682312e2e4
   * 50428e7e8797cbab92c39cefbf0c8f88
   * b6dc1169078fc4da7913fa153e47e1a5
   * 132ecd77dc5a8ecfd36ce6bb149d208f
   */

}
