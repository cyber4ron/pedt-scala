package n4c.core

import n4c.core.Method.Executable
import n4c.core.Scope.Resources
import n4c.util.{ Conversions, ScopeProxy }
import spray.json._

import scala.collection.immutable.Iterable

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
  private[n4c] def apply(argsJson: JsValue) = {
    import scala.collection.JavaConversions._
    new Arguments(argsJson.asJsObject.fields.map(kv => kv._1 -> Conversions.jsValueToJava(kv._2)))
  }

  private[n4c] def apply(args: Iterable[(String, JsValue)]) = {
    import scala.collection.JavaConversions._
    new Arguments(args.map { kv =>
      kv._1 -> Conversions.jsValueToJava(kv._2)
    }.toMap[String, Object])
  }

  def mix(self: Option[Arguments], other: Map[String, JsValue]): Option[Arguments] = { // 会覆盖??
    var mixed = Map.empty[String, Object]

    import collection.JavaConversions._
    self.foreach { args => for ((k, v) <- args.get) mixed = mixed.updated(k, v) }
    for ((k, v) <- other) mixed = mixed.updated(k, Conversions.jsValueToJava(v))

    if (mixed.nonEmpty) Some(new Arguments(mapAsJavaMap(mixed))) else None
  }
}

class Arguments(private val arguments: java.util.Map[String, Object]) {
  def get: java.util.Map[String, Object] = arguments
  def getArgumentArray: Array[Object] = arguments.values().toArray
}

object Method {
  type Executable = { def execute(x: Object*): Any } // call by reflection
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

  def execute(args: Map[String, JsValue]): Any = {
    import n4c.context.HttpContext._

    log.info(arguments + s", in method.execute, args: $args")
    arguments = Arguments.mix(arguments, args)
    log.info(arguments + s", in method.execute")
    arguments.foreach(x => println(s"arguments: ${x.get}"))

    distrType match {
      case "run" => executable match {
        case _: Data | _: Script => executable.execute(arguments.map(_.getArgumentArray).getOrElse(Array.empty[Object]): _*)
        case _: Task             => executable.execute(arguments.map(_.getArgumentArray).getOrElse(Array.empty[Object]))
        case _                   => throw new IllegalStateException
      }
      case "map" =>
        if (scope.isEmpty) throw new IllegalStateException("")
        executable match {
          case t: Task => scope.get.getResources map { res =>
            if (t.taskId.isDefined) requestBlocking(s"$res/execute_task:${t.taskId.get}")
          }
          case _ => throw new IllegalStateException("")
        }
      case _ => throw new IllegalStateException
    }
  }
}
