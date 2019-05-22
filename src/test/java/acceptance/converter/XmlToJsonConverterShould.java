package acceptance.converter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

public class XmlToJsonConverterShould extends ConverterBase {

    private String randomValue = generateRandomString();

    @Override
    protected String getMode() {
        return "xml";
    }

    @Override
    protected void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        String expectedValue = formatExpectedValue(orderId);
        assertJsonEquals(expectedValue, value);
    }

    @Override
    protected String createInputMessage() {
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
