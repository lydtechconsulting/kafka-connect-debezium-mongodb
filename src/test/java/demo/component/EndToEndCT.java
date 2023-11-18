package demo.component;

import java.time.Duration;
import java.util.List;

import demo.rest.api.CreateItemRequest;
import demo.rest.api.UpdateItemRequest;
import demo.util.TestRestData;
import dev.lydtech.component.framework.client.debezium.DebeziumClient;
import dev.lydtech.component.framework.client.kafka.KafkaClient;
import dev.lydtech.component.framework.client.service.ServiceClient;
import dev.lydtech.component.framework.extension.ComponentTestExtension;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
@ExtendWith(ComponentTestExtension.class)
@ActiveProfiles("component-test")
public class EndToEndCT {

    private static final String GROUP_ID = "EndToEndCT";
    private Consumer consumer;

    @BeforeEach
    public void setup() {
        String serviceBaseUrl = ServiceClient.getInstance().getBaseUrl();
        RestAssured.baseURI = serviceBaseUrl;

        // Configure the test Kafka consumer to listen to the CDC topic for item.
        consumer = KafkaClient.getInstance().createConsumer(GROUP_ID, "mongodb.demo.items");

        // Configure the debezium source connector.  Attempt to delete it initially in case it is still in place.
        DebeziumClient.getInstance().deleteConnector("debezium-mongodb-source-connector");
        DebeziumClient.getInstance().createConnector("connector/debezium-mongodb-source-connector.json");

        // Clear the topic.
        consumer.poll(Duration.ofSeconds(1));
    }

    @AfterEach
    public void tearDown() {
        DebeziumClient.getInstance().deleteConnector("debezium-mongodb-source-connector");
        consumer.close();
    }

    /**
     * A REST request is POSTed to the v1/item endpoint in order to create a new Item entity.
     *
     * The item is then updated to change the name.
     *
     * The item is then deleted.
     *
     * An outbound event is emitted using change data capture for each of the create, update, and delete calls, with
     * Debezium writing these to Kafka.  The test consumer consumes these to assert they were emitted.
     */
    @Test
    public void testChangeDataCapture() throws Exception {

        // Test the POST endpoint to create an item.
        CreateItemRequest createRequest = TestRestData.buildCreateItemRequest(RandomStringUtils.randomAlphabetic(1).toUpperCase()+RandomStringUtils.randomAlphabetic(7).toLowerCase()+"1");
        Response createItemResponse = sendCreateItemRequest(createRequest);
        String itemId = createItemResponse.header("Location");
        assertThat(itemId, notNullValue());
        log.info("Create item response location header: "+itemId);

        // Test the GET endpoint to fetch the item.
        sendGetItemRequest(itemId, createRequest.getName());

        // Test the PUT endpoint to update the item name.
        UpdateItemRequest updateRequest = TestRestData.buildUpdateItemRequest(RandomStringUtils.randomAlphabetic(1).toUpperCase()+RandomStringUtils.randomAlphabetic(7).toLowerCase()+"2");
        sendUpdateRequest(itemId, updateRequest);

        // Ensure the name was updated.
        sendGetItemRequest(itemId, updateRequest.getName());

        // Test the DELETE endpoint to delete the item.
        sendDeleteRequest(itemId);

        // Ensure the deleted item cannot be found.
        sendGetItemRequest(itemId, HttpStatus.NOT_FOUND);

        // Ensure the three CDC events were emitted, one for create, one for update, and one for delete.
        List<ConsumerRecord<String, String>> outboundEvents = KafkaClient.getInstance().consumeAndAssert("testChangeDataCapture", consumer, 3, 3);
        assertThat(outboundEvents.size(), equalTo(3));
        log.info("CDC create event: "+outboundEvents.get(0));
        assertThat(outboundEvents.get(0).key(), containsString(itemId));
        assertThat(outboundEvents.get(0).value(), containsString(itemId));
        assertThat(outboundEvents.get(0).value(), containsString(createRequest.getName()));
        // The event should contain the create operation 'c'.
        assertThat(outboundEvents.get(0).value(), containsString("\"op\":\"c\""));

        log.info("CDC update event: "+outboundEvents.get(1));
        assertThat(outboundEvents.get(1).key(), containsString(itemId));
        assertThat(outboundEvents.get(1).value(), containsString(itemId));
        assertThat(outboundEvents.get(1).value(), containsString(updateRequest.getName()));
        // The event should contain the update operation 'u'.
        assertThat(outboundEvents.get(1).value(), containsString("\"op\":\"u\""));

        log.info("CDC delete event: "+outboundEvents.get(2));
        assertThat(outboundEvents.get(2).key(), containsString(itemId));
        // The event should contain the delete operation 'd'.
        assertThat(outboundEvents.get(2).value(), containsString("\"op\":\"d\""));
    }

    private static Response sendCreateItemRequest(CreateItemRequest createRequest) {
        Response createItemResponse = given()
                .header("Content-type", "application/json")
                .and()
                .body(createRequest)
                .when()
                .post("/v1/items")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .response();
        return createItemResponse;
    }

    private static void sendUpdateRequest(String location, UpdateItemRequest updateRequest) {
        given()
                .pathParam("id", location)
                .header("Content-type", "application/json")
                .and()
                .body(updateRequest)
                .when()
                .put("/v1/items/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .extract()
                .response();
    }

    private static void sendDeleteRequest(String location) {
        given()
                .pathParam("id", location)
                .when()
                .delete("/v1/items/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private static void sendGetItemRequest(String location, String expectedName) {
        given()
                .pathParam("id", location)
                .when()
                .get("/v1/items/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("name", containsString(expectedName));
    }

    private static void sendGetItemRequest(String location, HttpStatus expectedHttpStatus) {
        given()
                .pathParam("id", location)
                .when()
                .get("/v1/items/{id}")
                .then()
                .assertThat()
                .statusCode(expectedHttpStatus.value());
    }
}
