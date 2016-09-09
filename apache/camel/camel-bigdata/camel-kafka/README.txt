camel-kafka
==========

This demo shows how to use Apache Camel to invoke on an Apache Kafka broker.
The test launches the Apache Kafka broker and creates a "test" topic. It also
starts a producer and writes a key/value pair "somekey"/"somevalue" to the
broker.

It then starts a Camel route with uses the Kafka component to read from the
"test" topic, and to store the value in a file in target/results, where the
filename corresponds to the key.

