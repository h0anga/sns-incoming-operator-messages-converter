package converter

case class FullModifyVoiceFeaturesInstruction(operatorId: String, orderId: String, serviceId: String, operatorOrderId: String, features: Seq[String])