package converter

import scala.util.Properties

object Xml2JsonConverterApp {
  private val kafkabroker: String = Properties.envOrElse("KAFKA_BROKER_SERVER", "localhost")
  private val kafkabrokerPort: String = Properties.envOrElse("KAFKA_BROKER_PORT", "9092")

  private val incomingOperatorMessagesTopic: String = Properties.envOrElse("INPUT_KAFKA_TOPIC", "incoming.op.msgs")
  private val modifyOperatorMessagesTopic: String = Properties.envOrElse("OUTPUT_KAFKA_TOPIC", "modify.op.msgs")

  private val appName: String = Properties.envOrElse("APP_NAME", "sns-incoming-operator-messages-converter")

  def main(args: Array[String]): Unit = {
    println(s"APP NAME: $appName")
    println(s"SERVER: $kafkabroker")
    println(s"PORT: $kafkabrokerPort")
    println(s"IN: $incomingOperatorMessagesTopic")
    println(s"OUT: $modifyOperatorMessagesTopic")

  }

  val kafkaSetup = new KafkaSetup(kafkabroker, kafkabrokerPort)
  kafkaSetup.start(appName, incomingOperatorMessagesTopic, modifyOperatorMessagesTopic)

  sys.ShutdownHookThread {
    kafkaSetup.shutDown()
  }
}
