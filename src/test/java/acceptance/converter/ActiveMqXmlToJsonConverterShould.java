package acceptance.converter;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

public class ActiveMqXmlToJsonConverterShould extends ConverterBase {


    private String randomValue = generateRandomString();
    private String traceyId = generateRandomString();

    @Override
    protected String getMode() {
        return "mqConnectorJsonContainingXml";
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Kafka Logs = " + KAFKA_CONTAINER.getLogs());
        System.out.println("Converter Logs = " + converterContainer.getLogs());
    }

    @Test
    public void convertsAnyXmlToJson() throws ExecutionException, InterruptedException {

//        createTopics();

        writeMessageToInputTopic();

        assertKafkaMessageEquals();
    }

    protected String createInputMessage() {
        return String.format("{"
                + "\"ORDER_ID\":\"%s\","
                + "\"TRACEY_ID\":\"%s\","
                + "\"XML\":\"%s\""
                + "}", orderId, traceyId, createXmlMessage());
    }

    @Override
    protected void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        String expectedValue = formatExpectedValue(orderId);
        assertJsonEquals(expectedValue, value);
    }

    private String formatExpectedValue(String orderId) {
        return String.format("{\"order\":{\"orderId\":\"%s\"," +
                "\"randomValue\":\"%s\"},\"traceId\":\"%s\"}",
                orderId, randomValue, "${json-unit.ignore}"
        );
    }

    private void createTopics() {
        AdminClient adminClient = AdminClient.create(getKafkaProperties());
        NewTopic xmlTopic = new NewTopic(INPUT_TOPIC, 1, (short) 1);
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
