package converter

import java.util.Properties

import brave.Tracing
import brave.kafka.streams.KafkaStreamsTracing
import brave.sampler.Sampler
import converter.Xml2JsonConverter.xmlToJson
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream.{Consumed, KStream, Predicate, ValueMapper}
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig, Topology}
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.kafka11.KafkaSender


class KafkaSetup(private val server: String, private val port: String) {

  private implicit val stringSerde: Serde[String] = Serdes.String()

  private var stream: KafkaStreams = _

  private val bootstrapServers = server + ":" + port

  private val xmlPredicate: Predicate[_ >: String, _ >: String] = (_: String, value: String) => {
    println(s"""Value: $value""")
//    value.startsWith("""<?xml version="1.0" encoding="UTF-8"?>""")
    true
  }

  private val tracing = setupTracing

  def start(appName: String, inputTopicName: String, outputTopicName: String) = {
    println(s"Input topic: $inputTopicName")
    println(s"Outut topic: $outputTopicName")

    val streamingConfig = {
      val settings = new Properties
      settings.put(StreamsConfig.APPLICATION_ID_CONFIG, appName)
      settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler])
      settings.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor")
      settings.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor")
      settings
    }

    val topology = build(inputTopicName, outputTopicName)
    stream = tracing.kafkaStreams(topology, streamingConfig)
    stream.start()
  }

  def setupTracing: KafkaStreamsTracing = {
    val sender = KafkaSender.newBuilder.bootstrapServers(bootstrapServers).build
    val reporter = AsyncReporter.builder(sender).build
    val tracing = Tracing.newBuilder.localServiceName("xml-to-json-stream-transform").sampler(Sampler.ALWAYS_SAMPLE).spanReporter(reporter).build
    KafkaStreamsTracing.create(tracing)
  }

  def shutDown(): Unit = {
    stream.close()
  }

  def build(inputTopicName: String, outputTopicName: String): Topology = {
    val builder = new StreamsBuilder
    val emptyStringPredicate: Predicate[_ >: String, _ >: String] = (_: String, value: String) => {
      println(s"""json to output: $value""")
      value.isEmpty
    }

    val xmlToJsonMapper: ValueMapper[String,String] = xmlToJson(_)

    val inputStream: KStream[String, String] = builder.stream(inputTopicName, Consumed.`with`(stringSerde, stringSerde))
    val jsonStream: KStream[String, String] = //inputStream.filter(xmlPredicate)
      inputStream.transformValues(tracing.mapValues("xml_to_json", xmlToJsonMapper))

//    val xmlStream: KStream[String, String] = inputStream.filter(xmlPredicate)
//    val jsonStream: KStream[String, String] = xmlStream.mapValues(line => xmlToJson(line))
    jsonStream.filterNot(emptyStringPredicate)
      .to(outputTopicName)

    builder.build
  }
}