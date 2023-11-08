# Kafka Connect: Debezium, MongoDB and Spring Boot Demo

Spring Boot application demonstrating using Kafka Connect for Change Data Capture (CDC) to publish outbound events to Kafka from MongoDB.

## Running The Demo

The demo steps are as follows (and detailed below):
- Start the Docker containers (Kafka, Zookeeper, MongoDB, Debezium) via docker-compose.
- Submit the Kafka Connect connector definition via curl.
- Start the Spring Boot demo application.
- Start a console-consumer to listen on the outbox topic (populated by Debezium).
- Submit a REST request to the application to create an item
- The application creates the item entity.  The source connector writes an event to the outbound topic via Change Data Capture (CDC).  The console-consumer consumes this event.

Demo steps breakdown:

Build Spring Boot application with Java 17:
```
mvn clean install
```

Start Docker containers:
```
docker-compose up -d
```

Check status of Kafka Connect:
```
curl localhost:8083
```

List registered connectors (empty array initially):
```
curl localhost:8083/connectors
```

Register connector:
```
curl -X POST localhost:8083/connectors -H "Content-Type: application/json" -d @./connector/debezium-mongodb-source-connector.json
```

List registered connectors:
```
curl localhost:8083/connectors
["debezium-mongodb-source-connector"]
```

Start Spring Boot application:
```
java -jar target/kafka-connect-debezium-mongodb-1.0.0.jar
```

Jump onto Kafka docker container:
```
docker exec -ti kafka bash
```

Start `kafka-console-consumer` to listen for the CDC event:
```
kafka-console-consumer --topic mongodb.demo.items --bootstrap-server kafka:29092
```

In a terminal window use curl to submit a POST REST request to the application to create an item:
```
curl -i -X POST localhost:9001/v1/items -H "Content-Type: application/json" -d '{"name": "test-item"}'
```

A response should be returned with the 201 CREATED status code and the new item id in the Location header:
```
HTTP/1.1 201 
Location: 653d06f08faa89580090466e
```

The Spring Boot application should log the successful item persistence:
```
Item created with id: 653d06f08faa89580090466e
```

View the CDC event consumed by the `kafka-console-consumer` from Kafka:
```
{"schema":{"type":"struct","fields":[{"type":"string","optional":true,"name":"io.debezium.data.Json","version":1,"field": [...]
```

Get the item that has been created using curl:
```
curl -i -X GET localhost:9001/v1/items/653d06f08faa89580090466e
```

A response should be returned with the 200 SUCCESS status code and the item in the response body:
```
HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sat, 28 Oct 2023 15:19:36 GMT

{"id":"653d06f08faa89580090466e","name":"test-item"}
```

In a terminal window use curl to submit a PUT REST request to the application to update the item:
```
curl -i -X PUT localhost:9001/v1/items/653d06f08faa89580090466e -H "Content-Type: application/json" -d '{"name": "test-item-update"}'
```

A response should be returned with the 204 NO CONTENT status code:
```
HTTP/1.1 204 
```

The Spring Boot application should log the successful update of the item:
```
Item updated with id: 653d06f08faa89580090466e - name: test-item-update
```

A second CDC event representing the update should be consumed by the `kafka-console-consumer`.

Delete the item using curl:
```
curl -i -X DELETE localhost:9001/v1/items/653d06f08faa89580090466e
```

The Spring Boot application should log the successful deletion of the item:
```
Deleted item with id: 653d06f08faa89580090466e
```

A third CDC event representing the delete should be consumed by the `kafka-console-consumer`

Delete registered connector:
```
curl -i -X DELETE localhost:8083/connectors/debezium-mongodb-source-connector
```

Stop containers:
```
docker-compose down
```

## Component Tests

The test demonstrates the application publishing events using Debezium (Kafka Connect) for Change Data Capture.   They use a dockerised Kafka broker, a dockerised Debezium Kafka Connect, a dockerised MongoDB database, and a dockerised instance of the application.

For more on the component tests see: https://github.com/lydtechconsulting/component-test-framework

Build Spring Boot application jar:
```
mvn clean install
```

Build Docker container:
```
docker build -t ct/kafka-connect-debezium-mongodb:latest .
```

Run tests:
```
mvn test -Pcomponent
```

Run tests leaving containers up:
```
mvn test -Pcomponent -Dcontainers.stayup
```

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```

## Kafka Connect / Debezium

### Create MongoDB connector

The Debezium MongoDB source connector configuration is defined in `connector/debezium-mongodb-source-connector.json`.

https://debezium.io/documentation/reference/stable/connectors/mongodb.html

When an item is created, updated or deleted, CDC results in an outbound event being emitted with the change information.

The component tests create and delete the connector via the `DebeziumClient` class in the `component-test-framework`.

## Docker Clean Up

Manual clean up (if left containers up):
```
docker rm -f $(docker ps -aq)
```

Further docker clean up if network/other issues:
```
docker system prune
docker volume prune
```
