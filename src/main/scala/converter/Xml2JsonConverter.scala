package converter

import brave.Tracing
import org.json4s
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.Xml.toJson
import org.json4s.native.JsonMethods._

import scala.xml.Elem

object Xml2JsonConverter {
  def fromXml(xmlString: String): Elem = {
    scala.xml.XML.loadString(xmlString)
  }

  def xmlToJson(instruction: String): String = {
    val span = Tracing.currentTracer().currentSpan()
    val traceId = span.context().traceIdString()
    println(s"TraceId: $traceId")

    val xml = scala.xml.XML.loadString(instruction)
    val json: json4s.JValue = toJson(xml)
    val tracedJson = json merge JObject("traceId" -> JString(traceId))
    compact(render(tracedJson))
  }
}
