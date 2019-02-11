package converter

import scala.util.Properties

object Xml2JsonConverterApp extends App {
  private val kafkabroker: String = Properties.envOrElse("KAFKA_BROKER_SERVER", "localhost")
  private val kafkabrokerPort: String = Properties.envOrElse("KAFKA_BROKER_PORT", "9092")

  private val incomingOperatorMessagesTopic = "incoming.op.msgs"
  private val modifyOperatorMessagesTopic = "modify.op.msgs"

  val kafkaSetup = new KafkaSetup(kafkabroker, kafkabrokerPort)
  kafkaSetup.start(incomingOperatorMessagesTopic, modifyOperatorMessagesTopic)

  sys.ShutdownHookThread {
    kafkaSetup.shutDown()
  }
}
