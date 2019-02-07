camel-kafka
==========

This demo shows how to use Apache Camel to invoke on an Apache Kafka broker.
You can set up Apache Kafka something like this:

 * bin/zookeeper-server-start.sh config/zookeeper.properties
 * bin/kafka-server-start.sh config/server.properties
 * bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
 * bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test --producer.config config/producer.properties
 
Then enter a few messages in the producer console. Then run "mvn clean install". This will 
start a Camel route that uses the Kafka component to read from the "test" topic, and to store 
the values in target/results.

