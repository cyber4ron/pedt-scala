package n4c.pedt.core

import n4c.pedt.core.Method.Executable
import n4c.pedt.core.Scope.Resources
import n4c.pedt.util.{Conversions, ScopeProxy}
import spray.json._

import scala.concurrent.Future

object Scope {
  type Resources = Seq[String]

  private[n4c] def apply(system: String, path: String, scopeFilter: String, res: Resources): Option[Scope] =
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
  private[n4c] def apply(args: JsValue) = {
    new Arguments(args.asJsObject.fields)
  }

  def mix(self: Option[Arguments], other: Map[String, JsValue]): Option[Arguments] = {
    var mixed = Map.empty[String, JsValue]
    if(self.isDefined) { self.get.scalaArgs.foreach(kv => mixed = mixed.updated(kv._1, kv._2)) }
    for ((k, v) <- other) mixed = mixed.updated(k, v)

    if (mixed.nonEmpty) Some(new Arguments(mixed)) else None
  }
}

class Arguments(private[n4c] val scalaArgs: Map[String, JsValue]) {
  import scala.collection.JavaConversions._
  val javaArgs: java.util.Map[String, Object] = scalaArgs.map(kv => kv._1 -> Conversions.jsValueToJava(kv._2))
  def getArg: java.util.Map[String, Object] = javaArgs
  def getArgArray: Array[Object] = javaArgs.values().toArray
}

object Method {
  type Executable = { def execute(x: Object*): Future[AnyRef] } // call by reflection
  def apply(methodDef: JsObject) = new Method(methodDef)
}

class Method(methodDef: JsObject) {
  var executable: Executable = _
  var distrType: String = _
  var scope: Option[Scope] = None
  var arguments: Option[Arguments] = None

  methodDef.fields.foreach {
    case ("scope", sc: JsValue)       => scope = ScopeProxy.get(sc.asInstanceOf[JsString].value) // type
    case ("arguments", args: JsValue) => arguments = Some(Arguments(args.asJsObject))
    case (dType @ ("run" | "map"), value: JsValue) => // value其实是个string
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
        case task: Task             => task.execute(arguments.map(_.getArgArray).getOrElse(Array.empty[Object]))
        case _                   => throw new IllegalStateException
      }
      case "map" =>
        if (scope.isEmpty) throw new IllegalStateException("")
        executable match {
          case task: Task =>
            if (scope.isEmpty) throw new IllegalStateException("")
            PEDT.map(scope.get, task.taskId.get, arguments.get.scalaArgs)
          case _ => throw new IllegalStateException("")
        }
      case _ => throw new IllegalStateException
    }
  }
}
