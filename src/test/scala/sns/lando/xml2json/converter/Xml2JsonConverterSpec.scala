package sns.lando.xml2json.converter

import converter.Xml2JsonConverter._
import converter.{ModifyVoiceFeaturesInstruction, Xml2JsonConverter}
import org.scalatest._

class Xml2JsonConverterSpec extends FlatSpec with Matchers with GivenWhenThen {

  val operatorId = "sky"
  val orderId = "33269793"
  val serviceId = "31642339"
  val operatorOrderId = "SogeaVoipModify_YHUORO"
  val features = Seq("CallerDisplay","RingBack","ChooseToRefuse")

  it should "create instruction with operatorId, orderId, serviceId, operatorOrderId, features" in {
    val instruction = ModifyVoiceFeaturesInstruction(operatorId, orderId, serviceId, operatorOrderId, features)

    instruction.operatorId should be (operatorId)
    instruction.orderId should be (orderId)
    instruction.serviceId should be (serviceId)
    instruction.operatorOrderId should be (operatorOrderId)
    instruction.features should contain theSameElementsAs features
  }

  it should "map xml to case class" in {
    val instruction  = fromXml(expectedXml)

    instruction.operatorId should be (operatorId)
    instruction.orderId should be (orderId)
    instruction.serviceId should be (serviceId)
    instruction.operatorOrderId should be (operatorOrderId)
    instruction.features should contain theSameElementsAs  features
  }

  it should "convert xml to json" in {
    val instruction  = fromXml(expectedXml)
    toJson(instruction) should be (expectedJson)
  }

  private val expectedJson =
    """{"modifyVoiceFeaturesInstruction":{"operatorId":"sky","orderId":"33269793","serviceId":"31642339","operatorOrderId":"SogeaVoipModify_YHUORO","features":["CallerDisplay","RingBack","ChooseToRefuse"]}}"""

  private val expectedXml =
    """
      |<transaction receivedDate="2018-11-15T10:29:07" operatorId="sky" operatorTransactionId="op_trans_id_095025_228" operatorIssuedDate="2011-06-01T09:51:12">
      |  <instruction version="1" type="PlaceOrder">
      |    <order>
      |      <type>modify</type>
      |      <operatorOrderId>SogeaVoipModify_YHUORO</operatorOrderId>
      |      <operatorNotes>Test: notes</operatorNotes>
      |      <orderId>33269793</orderId>
      |    </order>
      |    <modifyFeaturesInstruction serviceId="31642339" operatorOrderId="SogeaVoipModify_YHUORO" operatorNotes="Test: addThenRemoveStaticIpToAnFttcService">
      |      <features>
      |          <feature code="CallerDisplay"/>
      |          <feature code="RingBack"/>
      |          <feature code="ChooseToRefuse"/>
      |      </features>
      |    </modifyFeaturesInstruction>
      |  </instruction>
      |</transaction>
    """.stripMargin
}


