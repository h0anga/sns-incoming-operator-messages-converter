package converter

import scala.util.Properties

object Xml2JsonConverterApp extends App {
  private val kafkabroker: String = Properties.envOrElse("KAFKA_BROKER_SERVER", "localhost")
  private val kafkabrokerPort: String = Properties.envOrElse("KAFKA_BROKER_PORT", "9092")

  private val lluStreamMessagesTopic = "incoming.op.msgs"
  private val lluStreamMessagesConvertedToJsonTopic = "incoming.op.msgs.converted.to.json"

  val kafkaSetup = new KafkaSetup(kafkabroker, kafkabrokerPort)
  kafkaSetup.start(lluStreamMessagesTopic, lluStreamMessagesConvertedToJsonTopic)

  sys.ShutdownHookThread {
    kafkaSetup.shutDown()
  }
}
