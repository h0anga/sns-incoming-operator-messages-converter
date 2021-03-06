package converter

import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.{StreamsConfig, TestInputTopic, TestOutputTopic, TopologyTestDriver}
import org.scalatest._

import java.util.{Properties, UUID}

class KafkaSetupSpec extends FlatSpec with Matchers {
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
    settings
  }

  private val orderId = UUID.randomUUID().toString

  private val expectedJson =
    s"""{"instruction":{"operatorId":"sky","receivedDate":"2018-11-15T10:29:07","order":{"operatorNotes":"Test: notes","orderId":"$orderId"},"modifyFeaturesInstruction":{"serviceId":"31642339","features":{"feature":[{"code":"CallerDisplay"},{"code":"RingBack"},{"code":"ChooseToRefuse"}]}}},"traceId":"""

  private val kafkaMessageInValue =
    s"""|<?xml version="1.0" encoding="UTF-8"?>
       |<instruction receivedDate="2018-11-15T10:29:07" operatorId="sky">
       |    <order>
       |      <operatorNotes>Test: notes</operatorNotes>
       |      <orderId>$orderId</orderId>
       |    </order>
       |    <modifyFeaturesInstruction serviceId="31642339">
       |      <features>
       |          <feature code="CallerDisplay"/>
       |          <feature code="RingBack"/>
       |          <feature code="ChooseToRefuse"/>
       |      </features>
       |    </modifyFeaturesInstruction>
       |</instruction>
    """.stripMargin

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

    val testServiceTopic: TestInputTopic[String, String] = topologyTestDriver.createInputTopic(inputTopic, keySerde.serializer(), valueSerde.serializer())
    testServiceTopic.pipeInput(kafkaMessageInKey, kafkaMessageInValue)

    val testOutputTopic: TestOutputTopic[String, String] = topologyTestDriver.createOutputTopic(outputTopic, keySerde.deserializer(), valueSerde.deserializer())
    val outputValue = testOutputTopic.readValue()

    outputValue should startWith (expectedJson)
  }
}
