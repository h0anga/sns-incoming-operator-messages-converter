package converter

import scala.util.Properties

object Xml2JsonConverterApp extends App {
  private val kafkabroker: String = Properties.envOrElse("KAFKA_BROKER_SERVER", "localhost")
  private val kafkabrokerPort: String = Properties.envOrElse("KAFKA_BROKER_PORT", "9092")

  private val incomingOperatorMessagesTopic: String = Properties.envOrElse("INPUT_KAFKA_TOPIC", getInputTopicName)
  private val modifyOperatorMessagesTopic: String = Properties.envOrElse("OUTPUT_KAFKA_TOPIC", getOutputTopicName)

  private val convertionMode: String = Properties.envOrElse("MODE", "xml")

  private val appName: String = Properties.envOrElse("APP_NAME", getAppName)

  private def getAppName = {
    var appName = "sns-incoming-operator-messages-converter"
    args.sliding(2, 2).toList.collect {
      case Array("--app-name", argAN: String) => appName = argAN
    }

    appName
  }

  private def getInputTopicName = {
    var INPUT_KAFKA_TOPIC = "INCOMING_OP_MSGS"
    args.sliding(2, 2).toList.collect {
      case Array("--input-topic", argIT: String) => INPUT_KAFKA_TOPIC = argIT
    }

    INPUT_KAFKA_TOPIC
  }

  private def getOutputTopicName = {
    var OUTPUT_KAFKA_TOPIC = "modify.op.msgs"
    args.sliding(2, 2).toList.collect {
      case Array("--output-topic", argOT: String) => OUTPUT_KAFKA_TOPIC = argOT
    }

    OUTPUT_KAFKA_TOPIC
  }

  println(s"MODE: $convertionMode")
  println(s"SERVER: $kafkabroker")
  println(s"PORT: $kafkabrokerPort")
  println(s"IN: $incomingOperatorMessagesTopic")
  println(s"OUT: $modifyOperatorMessagesTopic")


  def createConverter(): Converter = {
    if (convertionMode.equals("mqConnectorJsonContainingXml"))
      return MqJsonContainingXmlToJsonConverter
    return XmlToJsonConverter
  }

  val kafkaSetup = new KafkaSetup(createConverter(), kafkabroker, kafkabrokerPort)
  kafkaSetup.start(appName, incomingOperatorMessagesTopic, modifyOperatorMessagesTopic)

  sys.ShutdownHookThread {
    kafkaSetup.shutDown()
  }
}
