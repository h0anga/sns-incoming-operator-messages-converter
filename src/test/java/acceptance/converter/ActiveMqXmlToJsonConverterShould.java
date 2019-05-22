package acceptance.converter;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ActiveMqXmlToJsonConverterShould extends ConverterBase {
    private static final String ENV_KEY_KAFKA_BROKER_SERVER = "KAFKA_BROKER_SERVER";
    private static final String ENV_KEY_KAFKA_BROKER_PORT = "KAFKA_BROKER_PORT";

    private static final String XML_TOPIC = "INCOMING_OP_MSGS";
    private static final String JSON_TOPIC = "modify.op.msgs";

    private String randomValue = generateRandomString();
    private String orderId = generateRandomString();
    private String traceyId = generateRandomString();

    @Override
    protected Map<String, String> calculateEnvProperties() {
        Map<String, String> envProperties = new HashMap<>();
        envProperties.put(ENV_KEY_KAFKA_BROKER_SERVER, KAFKA_CONTAINER.getNetworkAliases().get(0));
        envProperties.put(ENV_KEY_KAFKA_BROKER_PORT, "" + 9092);
        return envProperties;
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Kafka Logs = " + KAFKA_CONTAINER.getLogs());
        System.out.println("Converter Logs = " + converterContainer.getLogs());
    }

    @Test
    public void convertsAnyXmlToJson() throws ExecutionException, InterruptedException {

//        createTopics();

        //when
        writeMessageToInputTopic();

        //then
        assertKafkaMessageEquals();
    }

    @Override
    protected ProducerRecord createKafkaProducerRecord() {
        return new ProducerRecord(XML_TOPIC, orderId, createJsonMessage());
    }

    private String createJsonMessage() {
        return String.format("{"
                + "\"ORDER_ID\":\"%s\","
                + "\"TRACEY_ID\":\"%s\","
                + "\"XML\":\"%s\""
                + "}", orderId, traceyId, createXmlMessage());
    }

    private void assertKafkaMessageEquals() {
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

    private void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        String expectedValue = formatExpectedValue(orderId);
        assertJsonEquals(expectedValue, value);
    }

    @NotNull
    private String generateRandomString() {
        return String.valueOf(new Random().nextLong());
    }

    private ConsumerRecords<String, String> pollForResults() {
        KafkaConsumer<String, String> consumer = createKafkaConsumer(getKafkaProperties());
        Duration duration = Duration.ofSeconds(3);
        return consumer.poll(duration);
    }

    @NotNull
    private KafkaConsumer<String, String> createKafkaConsumer(Properties props) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(JSON_TOPIC));
        return consumer;
    }

    private String formatExpectedValue(String orderId) {
        return String.format("{\"order\":{\"orderId\":\"%s\"," +
                "\"randomValue\":\"%s\"},\"traceId\":\"%s\"}",
                orderId, randomValue, "${json-unit.ignore}"
        );
    }

    private void createTopics() {
        AdminClient adminClient = AdminClient.create(getKafkaProperties());
        NewTopic xmlTopic = new NewTopic(XML_TOPIC, 1, (short) 1);
        NewTopic jsonTopic = new NewTopic(JSON_TOPIC, 1, (short) 1);

        List<NewTopic> newTopics = new ArrayList<>();
        newTopics.add(xmlTopic);
        newTopics.add(jsonTopic);

        CreateTopicsResult createTopicsResult = adminClient.createTopics(newTopics,
                new CreateTopicsOptions().timeoutMs(10000));
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

    private String createXmlMessage() {
        return String.format(
                "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>" +
                        "<order>" +
                        "<orderId>%s</orderId>" +
                        "<randomValue>%s</randomValue>" +
                        "</order>", orderId, randomValue
        );
    }

    private String createModifyVoiceMessage(String orderId) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                <transaction receivedDate=\"2018-11-15T10:29:07\" operatorId=\"sky\" " +
                        "operatorTransactionId=\"op_trans_id_095025_228\" " +
                        "operatorIssuedDate=\"2011-06-01T09:51:12\">\n" +
                        "                  <instruction version=\"1\" type=\"PlaceOrder\">\n" +
                        "                    <order>\n" +
                        "                      <type>modify</type>\n" +
                        "                      <operatorOrderId>SogeaVoipModify_${opereratorOrderId}</operatorOrderId" +
                        ">\n" +
                        "                      <operatorNotes>Test: notes</operatorNotes>\n" +
                        "                      <orderId>%1$s</orderId>\n" +
                        "                    </order>\n" +
                        "                    <modifyFeaturesInstruction serviceId=\"31642339\" " +
                        "operatorOrderId=\"SogeaVoipModify_${opereratorOrderId}\" operatorNotes=\"Test: " +
                        "addThenRemoveStaticIpToAnFttcService\">\n" +
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

}
