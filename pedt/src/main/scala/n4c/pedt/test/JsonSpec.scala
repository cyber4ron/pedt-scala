package n4c.pedt.test

import javax.script.{ Invocable, ScriptEngine, ScriptEngineManager }

import jdk.nashorn.api.scripting.ScriptObjectMirror
import n4c.pedt.core.Arguments
import spray.json._

/**
 * @author fenglei@wandoujia.com on 15/12/5.
 */

object JsonSpec extends App {
  val ee = JsArray(JsString("x"), JsNumber(10))
  val yy = JsObject(Map("arg1" -> JsString("str"), "arg2" -> JsNumber(1), "arg3" -> JsBoolean(true)))
  val zz = JsObject(Map("arg1" -> yy, "arg2" -> ee, "arg3" -> JsBoolean(true)))

  val xx = Arguments(zz) // to n4c.test

  val manager: ScriptEngineManager = new ScriptEngineManager
  val engine: ScriptEngine = manager.getEngineByName("nashorn")

  val json = engine.eval("JSON").asInstanceOf[ScriptObjectMirror]

  engine.eval("function printAny2(x){ print(x); };")

  val invocable = engine.asInstanceOf[Invocable]
  invocable.invokeFunction("printAny2", xx.getArg) // {arg1={arg1=str, arg2=1, arg3=true}, arg2=[x, 10], arg3=true}. ok.

  Thread.sleep(1000)
}
