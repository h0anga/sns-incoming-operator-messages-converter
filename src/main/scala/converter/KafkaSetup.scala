package converter

import java.util.Properties

import converter.Xml2JsonConverter.{fromXml, toJson}
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream.{Consumed, KStream, Predicate}
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig, Topology}



class KafkaSetup(private val server: String, private val port: String) {

  private implicit val stringSerde: Serde[String] = Serdes.String()

  private var stream: KafkaStreams = _

  def start(inputTopicName: String, outputTopicName: String) = {

    val bootstrapServers = server + ":" + port

    val streamingConfig = {
      val settings = new Properties
      settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "xml-to-json-converter")
      settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler])
      settings
    }
    val topology = build(inputTopicName, outputTopicName)
    stream = new KafkaStreams(topology, streamingConfig)
    stream.start()
  }

  def shutDown(): Unit = {
    stream.close()
  }

  def build(inputTopicName: String, outputTopicName: String): Topology = {
    val builder = new StreamsBuilder

    val emptyStringPredicate: Predicate[_ >: String, _ >: String] = (_: String, value: String) => {
      value.isEmpty
    }

    val inputStream: KStream[String, String] = builder.stream(inputTopicName, Consumed.`with`(stringSerde, stringSerde))

    val jsonValues: KStream[String, String] = inputStream.mapValues(line => toJson(fromXml(line)))

    val goodJsonValues: KStream[String, String] = jsonValues.filterNot(emptyStringPredicate)

    goodJsonValues.to(outputTopicName)

    builder.build
  }
}