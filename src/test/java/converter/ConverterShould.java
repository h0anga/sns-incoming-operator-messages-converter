package converter;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Testcontainers
public class ConverterShould {
    private static final String ENV_KEY_KAFKA_BROKER_SERVER = "KAFKA_BROKER_SERVER";
    private static final String ENV_KEY_KAFKA_BROKER_PORT = "KAFKA_BROKER_PORT";

    private static final String XML_TOPIC = "incoming.op.msgs";
    private static final String JSON_TOPIC = "modify.op.msgs";

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("5.2.1").withEmbeddedZookeeper();

    @Container
    private GenericContainer converterContainer = new GenericContainer("sns-incoming-operator-messages-converter:0.1.1")
            .withNetwork(KAFKA_CONTAINER.getNetwork())
            .withEnv(calculateEnvProperties());

    private String randomValue = "" + new Random().nextInt();
    private String server;
    private String port;

    private Map<String, String> calculateEnvProperties() {
        Map<String, String> envProperties = new HashMap<>();
        String bootstrapServers = KAFKA_CONTAINER.getNetworkAliases().get(0);
        envProperties.put(ENV_KEY_KAFKA_BROKER_SERVER, bootstrapServers);
        envProperties.put(ENV_KEY_KAFKA_BROKER_PORT, "" + 9092);
        server = bootstrapServers;
        port = "" + 9092;
        return envProperties;
    }

    private Map<String, String> calculateEnvProperties1() {
        Map<String, String> envProperties = new HashMap<>();
        String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();
        int i = bootstrapServers.lastIndexOf(":");
        envProperties.put(ENV_KEY_KAFKA_BROKER_SERVER, "localhost");
        envProperties.put(ENV_KEY_KAFKA_BROKER_PORT, "" + 9092);
        System.out.println("Env Server: " + server);
        System.out.println("Env Port: " + port);
        return envProperties;
    }

    @BeforeEach
    public void setup() {
        assertTrue(KAFKA_CONTAINER.isRunning());
        assertTrue(converterContainer.isRunning());
        KAFKA_CONTAINER.addExposedPorts(Integer.parseInt(port));
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Kafka Logs = " + KAFKA_CONTAINER.getLogs());
        System.out.println("Converter Logs = " + converterContainer.getLogs());
    }

    @Test
    public void t() throws ExecutionException, InterruptedException {


        String KafkaDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
        String KafkaSerializer = "org.apache.kafka.common.serialization.StringSerializer";

        Properties props = getProperties(KafkaDeserializer, KafkaSerializer);


        KafkaProducer<String, String> lluStreamMessagesTopicProducer = new KafkaProducer<>(props);

        String orderId = String.valueOf(new Random().nextLong());
        String messageValue = createMessage(orderId);

        createTopics(props);

        //when
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(JSON_TOPIC));
        Duration immediately = Duration.ofSeconds(0);
        consumer.poll(immediately);

        lluStreamMessagesTopicProducer.send(new ProducerRecord(XML_TOPIC, orderId, messageValue)).get();
        lluStreamMessagesTopicProducer.flush();
        lluStreamMessagesTopicProducer.close();
        Thread.sleep(5000);

        //then


        Duration duration = Duration.ofSeconds(4);
        ConsumerRecords<String, String> recs = consumer.poll(duration);
        assertFalse(recs.isEmpty());
        boolean foundMatchingRecord = false;
        Iterator<ConsumerRecord<String, String>> consumerRecordIterator = recs.iterator();
        while (consumerRecordIterator.hasNext()) {
            ConsumerRecord<String, String> consumerRecord = consumerRecordIterator.next();
            String key = consumerRecord.key();
            if (orderId.equals(key)) {
                String value = consumerRecord.value();
                String expectedValue = formatExpectedValue(orderId);
                assertJsonEquals(expectedValue, value);
//                assertEquals(expectedValue, value);
                foundMatchingRecord = true;
            } else {
                System.out.println("key = " + key);
                System.out.println("value = " + consumerRecord.value());
            }
        }
        assertTrue("Did not find expected record", foundMatchingRecord);
    }

    private String formatExpectedValue(String orderId) {
        return String.format(
                "{" +
                        "  \"order\":{" +
                        "    \"orderId\":\"%s\"," +
                        "    \"randomValue\":\"%s\"" +
                        "  }," +
                        "  \"traceId\":\"${json-unit.ignore}\"" +
                        "}",
                orderId, randomValue
        );
    }


    private void createTopics(Properties props) {


        AdminClient adminClient = AdminClient.create(props);
        NewTopic xmlTopic = new NewTopic(XML_TOPIC, 1, (short) 1);
        NewTopic jsonTopic = new NewTopic(JSON_TOPIC, 1, (short) 1);

        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(xmlTopic);
        newTopics.add(jsonTopic);

        CreateTopicsResult createTopicsResult = adminClient.createTopics(newTopics, new CreateTopicsOptions().timeoutMs(10000));
        Map<String, KafkaFuture<Void>> futureResults = createTopicsResult.values();
        futureResults.values().forEach(f -> {
            try {
                f.get(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        });
        adminClient.close();
    }

    private String createMessage(String orderId) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<order>" +
                        "<orderId>%s</orderId>\n" +
                        "<randomValue>%s</randomValue> +" +
                        "</order>", orderId, randomValue
        );
    }

    private String createModifyVoiceMessage(String orderId) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                <transaction receivedDate=\"2018-11-15T10:29:07\" operatorId=\"sky\" operatorTransactionId=\"op_trans_id_095025_228\" operatorIssuedDate=\"2011-06-01T09:51:12\">\n" +
                        "                  <instruction version=\"1\" type=\"PlaceOrder\">\n" +
                        "                    <order>\n" +
                        "                      <type>modify</type>\n" +
                        "                      <operatorOrderId>SogeaVoipModify_${opereratorOrderId}</operatorOrderId>\n" +
                        "                      <operatorNotes>Test: notes</operatorNotes>\n" +
                        "                      <orderId>%1$s</orderId>\n" +
                        "                    </order>\n" +
                        "                    <modifyFeaturesInstruction serviceId=\"31642339\" operatorOrderId=\"SogeaVoipModify_${opereratorOrderId}\" operatorNotes=\"Test: addThenRemoveStaticIpToAnFttcService\">\n" +
                        "                      <features>\n" +
                        "                          <feature code=\"CallerDisplay\"/>\n" +
                        "                          <feature code=\"RingBack\"/>\n" +
                        "                          <feature code=\"ChooseToRefuse\"/>\n" +
                        "                      </features>\n" +
                        "                    </modifyFeaturesInstruction>\n" +
                        "                  </instruction>\n" +
                        "                </transaction>",
                orderId);
    }


    private Properties getProperties(String kafkaDeserializer, String kafkaSerializer) {
        String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();
        //        String bootstrapServers = KAFKA_CONTAINER.getNetworkAliases().get(0) + ":9092";

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put("acks", "all");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaSerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaSerializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaDeserializer);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaDeserializer);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.getClass().getName());
        return props;
    }
}
