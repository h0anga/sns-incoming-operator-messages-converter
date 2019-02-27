package converter

import net.liftweb.json._
import net.liftweb.json.Serialization.write

object Xml2JsonConverter {
  def fromXml(xmlString: String): ModifyVoiceFeaturesInstruction = {
    val incomingXml = scala.xml.XML.loadString(xmlString.trim)
    val operatorId = incomingXml.\@("operatorId")
    val orderId = incomingXml.\\("orderId").text
    val operatorOrderId = incomingXml.\\("operatorOrderId").text
    val serviceId = incomingXml.\\("modifyFeaturesInstruction").\@("serviceId")
    val features = incomingXml.\\("feature").map(_.\@("code"))
    ModifyVoiceFeaturesInstruction(operatorId, orderId, serviceId, operatorOrderId, features)
  }

  def toJson(instruction: ModifyVoiceFeaturesInstruction): String = {
    s"""{"modifyVoiceFeaturesInstruction":${write(instruction)(DefaultFormats)}}"""
  }
}
