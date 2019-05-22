package acceptance.converter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

public class XmlToJsonConverterShould extends ConverterBase {

    private String randomValue = generateRandomString();

    @Override
    protected Map<String, String> calculateEnvProperties() {
        Map<String, String> envProperties = new HashMap<>();
        envProperties.put(ENV_KEY_MODE, "xml");
        envProperties.put(ENV_KEY_KAFKA_BROKER_SERVER, KAFKA_CONTAINER.getNetworkAliases().get(0));
        envProperties.put(ENV_KEY_KAFKA_BROKER_PORT, "" + 9092);
        return envProperties;
    }

    @Override
    protected ProducerRecord createKafkaProducerRecord() {
        return new ProducerRecord(XML_TOPIC, orderId, createXmlMessage());
    }

    @Override
    protected void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        String expectedValue = formatExpectedValue(orderId);
        assertJsonEquals(expectedValue, value);
    }

    private String createXmlMessage() {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<order>" +
                        "<orderId>%s</orderId>\n" +
                        "<randomValue>%s</randomValue> +" +
                        "</order>", orderId, randomValue
        );
    }

    @Test
    void convertXmlToJson() throws ExecutionException, InterruptedException {
        writeMessageToInputTopic();

        assertKafkaMessageEquals();
    }

    private String formatExpectedValue(String orderId) {
        return String.format(
                "{" +
                        "  \"order\":{" +
                        "    \"orderId\":\"%s\"," +
                        "    \"randomValue\":\"%s\"" +
                        "  }" +
                        "}",
                orderId, randomValue
        );
    }


    @AfterEach
    public void tearDown() {
        System.out.println("Kafka Logs = " + KAFKA_CONTAINER.getLogs());
        System.out.println("Converter Logs = " + converterContainer.getLogs());
    }
}
