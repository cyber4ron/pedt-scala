package com.wandoujia.n4c.pedt.core

import scala.concurrent.Future
import scala.util.{ Failure, Success }

import org.slf4j.LoggerFactory
import spray.json._

import com.wandoujia.n4c.pedt
import pedt.util.{ Conversion, ScopeProxy }
import Method.Executable
import Scope.Resources

/**
 * @author fenglei@wandoujia.com, 2015-12
 */
object Scope {
  type Resources = Seq[String]

  private[pedt] def apply(system: String, path: String, scopeFilter: String, res: Resources): Option[Scope] =
    Some(new Scope(system,
      if (path == "") None else Some(path),
      scopeFilter, res))

}

class Scope(val system: String,
            val path: Option[String] = None,
            val scopeFilter: String,
            private val resources: Resources) {
  def getResources: Resources = scopeFilter match {
    case "*" | "" => resources
    case _        => Seq.empty[String]
  }
}

object Arguments {
  private[pedt] def apply(args: JsValue) = {
    new Arguments(args.asJsObject.fields)
  }

  def mix(self: Option[Arguments], other: Map[String, JsValue]): Option[Arguments] = {
    var mixed = Map.empty[String, JsValue]
    if (self.isDefined) { self.get.scalaArgs.foreach(kv => mixed = mixed.updated(kv._1, kv._2)) }
    for ((k, v) <- other) mixed = mixed.updated(k, v)

    if (mixed.nonEmpty) Some(new Arguments(mixed)) else None
  }
}

class Arguments(private[pedt] val scalaArgs: Map[String, JsValue]) {
  import collection.JavaConverters._
  private val javaArgs: java.util.Map[String, Object] = scalaArgs.map(kv => kv._1 -> Conversion.jsValueToJava(kv._2)).asJava
  def getArg: java.util.Map[String, Object] = javaArgs
  def getArgArray: Array[Object] = javaArgs.values().toArray
}

object Method {
  private val log = LoggerFactory.getLogger(Method.getClass)
  type Executable = { def execute(x: Object*): Future[AnyRef] } // JSExecutable or EmbeddedTask. call by reflection...
  def apply(methodDef: JsObject) = new Method(methodDef)
}

class Method(methodDef: JsObject) {
  import Method.log
  import concurrent.ExecutionContext.Implicits.global

  var executable: Executable = _
  var distrType: String = _
  var scope: Option[Scope] = None
  var arguments: Option[Arguments] = None

  methodDef.fields foreach {
    case ("scope", sc: JsValue)       => if (sc.isInstanceOf[JsString]) scope = ScopeProxy.get(sc.asInstanceOf[JsString].value)
    case ("arguments", args: JsValue) => arguments = Some(Arguments(args.asJsObject))
    case (dType @ ("run" | "map"), value: JsValue) =>
      distrType = dType
      val x = Data(value) orElse Script(value) orElse EmbeddedTask(value) orElse None
      if (x.isEmpty) throw new IllegalArgumentException
      else executable = x.get.asInstanceOf[Executable]
    case _ => throw new IllegalArgumentException
  }

  def execute(args: Map[String, JsValue]): Future[AnyRef] = {
    arguments = Arguments.mix(arguments, args)

    distrType match {
      case "run" => executable match {
        case executable @ (_: Data | _: Script) => executable.execute(arguments.map(_.getArgArray).getOrElse(Array.empty[Object]): _*)
        case task: Task =>
          val f = task.execute(arguments.map(_.getArg).getOrElse(Map.empty[String, Object]))
          f onComplete {
            case Success(x)  => log.info(s"execute method: $executable succeed, args: ${arguments.get.getArgArray}, result: $x")
            case Failure(ex) => log.error(s"execute method:: $executable failed, args: ${arguments.get.getArgArray}, ex.message: ${ex.getMessage}")
          }
          f
        case _ => throw new IllegalStateException
      }
      case "map" =>
        if (scope.isEmpty) throw new IllegalStateException("empty scope")
        executable match {
          case task: Task =>
            if (scope.isEmpty) throw new IllegalStateException("empty scope")
            PEDT.map(scope.get, task.taskId.get, arguments.get.scalaArgs)
          case _ => throw new IllegalStateException("try to map non-task")
        }
      case x => throw new IllegalStateException(s"undefined distribution type: $x")
    }
  }
}
