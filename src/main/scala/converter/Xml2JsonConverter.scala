package converter

import net.liftweb.json._
import net.liftweb.json.Serialization.write

object Xml2JsonConverter {

  def fromXmlToFullInstruction(xmlString: String): FullModifyVoiceFeaturesInstruction = {
    val incomingXml = scala.xml.XML.loadString(xmlString)
    val operatorId = incomingXml.\@("operatorId")
    val orderId = incomingXml.\\("orderId").text
    val operatorOrderId = incomingXml.\\("operatorOrderId").text
    val serviceId = incomingXml.\\("modifyFeaturesInstruction").\@("serviceId")
    val features = incomingXml.\\("feature").map(_.\@("code"))
    ModifyVoiceFeaturesInstruction(orderId)
    FullModifyVoiceFeaturesInstruction(operatorId, orderId, serviceId, operatorOrderId, features)
  }

  def fromXml(xmlString: String): ModifyVoiceFeaturesInstruction = {
    val incomingXml = scala.xml.XML.loadString(xmlString)
    val orderId = incomingXml.\\("orderId").text
    ModifyVoiceFeaturesInstruction(orderId)
  }

  def toJson(instruction: FullModifyVoiceFeaturesInstruction): String = {
    s"""{"modifyVoiceFeaturesInstruction":${write(instruction)(DefaultFormats)}}"""
  }

  def toJson(instruction: ModifyVoiceFeaturesInstruction): String = {
    s"""${write(instruction)(DefaultFormats)}"""
  }
}
