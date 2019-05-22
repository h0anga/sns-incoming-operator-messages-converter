package acceptance.converter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

@Testcontainers
public abstract class ConverterBase {
    private static final String KAFKA_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    private static final String KAFKA_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

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
}
