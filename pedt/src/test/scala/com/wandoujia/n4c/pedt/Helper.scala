package com.wandoujia.n4c.pedt

import java.security.MessageDigest

import scala.io.Source

object Helper extends App {

  import java.util.Base64
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/init_connection_count.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/get_current_connection_count.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/update_current_connection_count.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/word_count_map.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/word_count_reduce.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/call_from_js.js").mkString.getBytes))
  println(Base64.getEncoder.encodeToString(Source.fromFile("pedt/src/test/resources/test_task.js").mkString.getBytes))

  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/init_connection_count.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/get_current_connection_count.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/update_current_connection_count.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/word_count_map.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/word_count_reduce.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/call_from_js.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)
  println(MessageDigest.getInstance("MD5").digest(Source.fromFile("pedt/src/test/resources/test_task.json").mkString.getBytes).map("%02X".format(_)).mkString.toLowerCase)

}
