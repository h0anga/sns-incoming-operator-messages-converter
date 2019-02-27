package converter

import java.util.Properties

import converter.Xml2JsonConverter._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.apache.kafka.streams.{StreamsConfig, TopologyTestDriver}
import org.scalatest._

class Xml2JsonConverterSpec extends FlatSpec with Matchers with GivenWhenThen {

  private val kafkaApplicationId = "sns-incoming-operator-messages-converter"
  private val serverName = "serverName"
  private val portNumber = "portNumber"

  private val inputTopic = "topic-in"
  private val outputTopic = "topic-out"

  private val kafkaMessageInKey = "key"
  private val streamingConfig = {
    val settings = new Properties
    settings.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaApplicationId)
    settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, serverName + ":" + portNumber)
    settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
    settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
    settings.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler])
    settings
  }


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

  it should "map xml to modify instruction case class" in {
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


  private def createTopologyToTest = {
    val kafkaSetup = new KafkaSetup(serverName, portNumber)
    val topology = kafkaSetup.build(inputTopic, outputTopic)
    topology
  }

  it should "test a stream" in {
    val topology = createTopologyToTest
    val topologyTestDriver = new TopologyTestDriver(topology, streamingConfig)

    val keySerde: Serde[String] = Serdes.String
    val valueSerde: Serde[String] = Serdes.String

    val consumerRecordFactory: ConsumerRecordFactory[String, String] = new ConsumerRecordFactory[String, String](inputTopic, keySerde.serializer(), valueSerde.serializer())
    val inputKafkaRecord: ConsumerRecord[Array[Byte], Array[Byte]] = consumerRecordFactory.create(inputTopic, kafkaMessageInKey, kafkaMessageInValue)
    topologyTestDriver.pipeInput(inputKafkaRecord)

    val outputKafkaRecord: ProducerRecord[String, String] = topologyTestDriver.readOutput(outputTopic, keySerde.deserializer(), valueSerde.deserializer())
    val outputValue = outputKafkaRecord.value()

    outputValue shouldEqual expectedOutput
  }

  private val expectedJson =
    """{"modifyVoiceFeaturesInstruction":{"operatorId":"sky","orderId":"33269793","serviceId":"31642339","operatorOrderId":"SogeaVoipModify_YHUORO","features":["CallerDisplay","RingBack","ChooseToRefuse"]}}"""

  private val expectedOutput = expectedJson
  private val expectedXml =
    """|<?xml version="1.0" encoding="UTF-8"?>
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

  private val kafkaMessageInValue = expectedXml
}


