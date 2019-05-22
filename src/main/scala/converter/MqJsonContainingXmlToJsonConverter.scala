package converter

import brave.Tracing
import org.apache.commons.text.StringEscapeUtils
import org.json4s
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.Xml.toJson
import org.json4s.native.JsonMethods._

import scala.xml.Elem

object MqJsonContainingXmlToJsonConverter extends Converter {
//  def fromXml(xmlString: String): Elem = {
//    scala.xml.XML.loadString(xmlString)
//  }


  def xmlToJson(instruction: String): String = {
    println(s"Input message: $instruction")
    val span = Tracing.currentTracer().currentSpan()
    val traceId = span.context().traceIdString()

    val xmlField = readXmlFieldFromJson(instruction)
    val xmlString = stripSurroundingQuotes(xmlField)

    println(s"""xmlString: $xmlString""")

    val xml = scala.xml.XML.loadString(xmlString)
    val json: json4s.JValue = toJson(xml)
    val tracedJson = json merge JObject("traceId" -> JString(traceId))

    val result = pretty(render(tracedJson))
    println(
      s"""MqJsonContainingXmlToJsonConverter
         |result: $result""".stripMargin)
    result
  }

  private def readXmlFieldFromJson(instruction: String): String = {
    implicit val formats = DefaultFormats
    val json = parse(instruction)

    val xmlNode = compact(render(json\\"XML"))
    println(s"""xmlNode: $xmlNode""")
    xmlNode
  }

  private def stripSurroundingQuotes(xmlString: String): String = {
    val unescaped = StringEscapeUtils.unescapeJson(xmlString)
    unescaped.substring(unescaped.indexOf("?>") + 2, unescaped.length - 1)
  }
}
