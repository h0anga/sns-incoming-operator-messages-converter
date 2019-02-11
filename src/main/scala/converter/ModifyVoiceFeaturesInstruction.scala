package converter

case class ModifyVoiceFeaturesInstruction(operatorId: String, orderId: String, serviceId: String, operatorOrderId: String, features: Seq[String])