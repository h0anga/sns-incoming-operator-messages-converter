package converter

import org.json4s.Xml.toJson
import org.json4s.native.JsonMethods._

object XmlToJsonConverter extends Converter {
  override def xmlToJson(instruction: String): String = {
    val xml = scala.xml.XML.loadString(instruction)
    val json = toJson(xml)
    pretty(render(json))
  }

}
