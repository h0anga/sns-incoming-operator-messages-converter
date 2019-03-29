package converter

import net.liftweb.json._
import net.liftweb.json.Serialization.write
import org.json4s
import org.json4s.Xml.{toJson, toXml}
import org.json4s.native.JsonMethods._

import scala.xml.Elem

object Xml2JsonConverter {
  def fromXml(xmlString: String): Elem = {
    scala.xml.XML.loadString(xmlString)
  }

  def xmlToJson(instruction: String): String = {
    val xml = scala.xml.XML.loadString(instruction)
    val json = toJson(xml)
    pretty(render(json))
  }
}
