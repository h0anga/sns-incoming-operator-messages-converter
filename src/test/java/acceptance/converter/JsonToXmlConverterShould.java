package acceptance.converter;

import com.eclipsesource.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.xmlunit.assertj.XmlAssert.assertThat;

public class JsonToXmlConverterShould extends ConverterBase {

    private String randomValue = generateRandomString();

    @Override
    protected String getMode() {
        return "jsonToXml";
    }

    @Override
    protected void assertRecordValueJson(ConsumerRecord<String, String> consumerRecord) {
        String value = consumerRecord.value();
        System.out.println("Value: " + value);
        String expected = formatExpectedValue();
        System.out.println("Expected: " + expected);
        assertThat(value).and(expected).areIdentical();
    }

    private String formatExpectedValue() {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<order>" +
                        "<orderId>%s</orderId>" +
                        "<randomValue>%s</randomValue>" +
                        "</order>", orderId, randomValue
        );

    }

    @Override
    protected String createInputMessage() {
        return new JsonObject()
                .add("order", new JsonObject()
                        .add("orderId", orderId)
                        .add("randomValue", randomValue))
                .toString();
    }

    @Test
    void convertXmlToJson() throws ExecutionException, InterruptedException {
        writeMessageToInputTopic();

        assertKafkaMessageEquals();
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Kafka Logs = " + KAFKA_CONTAINER.getLogs());
        System.out.println("Converter Logs = " + converterContainer.getLogs());
    }

}
