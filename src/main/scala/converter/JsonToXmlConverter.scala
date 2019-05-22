package converter

import org.json4s.Xml.toXml
import org.json4s.native.JsonMethods.parse

object JsonToXmlConverter extends Converter {
  override def xmlToJson(instruction: String): String = {
    println(s"Input JSON: $instruction")
    val json = parse(instruction)
    val xml = toXml(json)
    val xmlWithDeclaration = """<?xml version="1.0" encoding="UTF-8"?>""" + xml
    xmlWithDeclaration
  }
}
