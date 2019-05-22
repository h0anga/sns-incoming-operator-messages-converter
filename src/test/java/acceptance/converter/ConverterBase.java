package acceptance.converter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
public abstract class ConverterBase {
    protected static final String ENV_KEY_MODE = "MODE";
    protected static final String ENV_KEY_KAFKA_BROKER_SERVER = "KAFKA_BROKER_SERVER";
    protected static final String ENV_KEY_KAFKA_BROKER_PORT = "KAFKA_BROKER_PORT";
    protected static final String XML_TOPIC = "INCOMING_OP_MSGS";
    protected static final String JSON_TOPIC = "modify.op.msgs";

    private static final String KAFKA_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    private static final String KAFKA_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    protected String orderId = generateRandomString();

    @Container
    protected static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.2.1").withEmbeddedZookeeper();

    @Container
    protected GenericContainer converterContainer = new GenericContainer("sns-incoming-operator-messages-converter:0.1.1")
            .withNetwork(KAFKA_CONTAINER.getNetwork())
            .withEnv(calculateEnvProperties());

    protected abstract Map<String, String> calculateEnvProperties();

    protected Properties getKafkaProperties() {
        String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put("acks", "all");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KAFKA_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KAFKA_SERIALIZER);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KAFKA_DESERIALIZER);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KAFKA_DESERIALIZER);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.getClass().getName());
        return props;
    }

    @BeforeEach
    public void setup() {
        assertTrue(KAFKA_CONTAINER.isRunning());
        assertTrue(converterContainer.isRunning());
    }

    protected void writeMessageToInputTopic() throws InterruptedException, ExecutionException {
        new KafkaProducer<String, String>(getKafkaProperties()).send(createKafkaProducerRecord()).get();
    }

    protected abstract ProducerRecord createKafkaProducerRecord();

    protected String generateRandomString() {
        return String.valueOf(new Random().nextLong());
    }

    protected void assertKafkaMessageEquals() {
        ConsumerRecords<String, String> recs = pollForResults();
        assertFalse(recs.isEmpty());

        Spliterator<ConsumerRecord<String, String>> spliterator = Spliterators.spliteratorUnknownSize(recs.iterator()
                , 0);
        Stream<ConsumerRecord<String, String>> consumerRecordStream = StreamSupport.stream(spliterator, false);
        Optional<ConsumerRecord<String, String>> expectedConsumerRecord =
                consumerRecordStream.filter(cr -> foundExpectedRecord(cr.key()))
                .findAny();
        expectedConsumerRecord.ifPresent(cr -> assertRecordValueJson(cr));
        if (!expectedConsumerRecord.isPresent())
            fail("Did not find expected record");
    }

    private boolean foundExpectedRecord(String key) {
        return orderId.equals(key);
    }

    protected abstract void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord);

    private ConsumerRecords<String, String> pollForResults() {
        KafkaConsumer<String, String> consumer = createKafkaConsumer(getKafkaProperties());
        Duration duration = Duration.ofSeconds(3);
        return consumer.poll(duration);
    }

    private KafkaConsumer<String, String> createKafkaConsumer(Properties props) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(ActiveMqXmlToJsonConverterShould.JSON_TOPIC));
        return consumer;
    }
}
