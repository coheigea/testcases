/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.coheigea.bigdata.kafka;

import java.io.File;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Future;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.Assert;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

/**
 * A simple test that starts a Kafka broker, creates a "test" topic, sends a message to it and consumes it.
 * Authentication is enforced using SASL SSL and GSS.
 * 
 * We also plug in a CustomSASLSSLAuthorizer that enforces some authorization rules.
 */
@org.junit.Ignore
public class KafkaAuthorizerSASLSSLGSSTest {
    
    private static KafkaServerStartable kafkaServer;
    private static TestingServer zkServer;
    private static int port;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        // JAAS Config file
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        File f = new File(basedir + "/src/test/resources/kafka_kerberos.jaas");
        System.setProperty("java.security.auth.login.config", f.getPath());
        
        zkServer = new TestingServer();
        
        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        
        final Properties props = new Properties();
        props.put("broker.id", 1);
        props.put("host.name", "localhost");
        props.put("port", port);
        props.put("log.dir", "/tmp/kafka");
        props.put("zookeeper.connect", zkServer.getConnectString());
        props.put("replica.socket.timeout.ms", "1500");
        props.put("controlled.shutdown.enable", Boolean.TRUE.toString());
        // Enable SASL_SSL
        props.put("listeners", "SASL_SSL://localhost:" + port);
        props.put("security.inter.broker.protocol", "SASL_SSL");
        props.put("sasl.enabled.mechanisms", "GSSAPI");
        props.put("sasl.mechanism.inter.broker.protocol", "GSSAPI");
        
        props.put("ssl.keystore.location", KafkaAuthorizerTest.class.getResource("/brokerstore.jks").getPath());
        props.put("ssl.keystore.password", "bspass");
        props.put("ssl.key.password", "bkpass");
        props.put("ssl.truststore.location", KafkaAuthorizerTest.class.getResource("/truststore.jks").getPath());
        props.put("ssl.truststore.password", "security");
        
        // Plug in custom authorizer
        props.put("authorizer.class.name", "org.apache.coheigea.bigdata.kafka.CustomSASLSSLAuthorizer");

        KafkaConfig config = new KafkaConfig(props);
        kafkaServer = new KafkaServerStartable(config);
        kafkaServer.startup();
        
        // Create some topics
        ZkClient zkClient = new ZkClient(zkServer.getConnectString(), 30000, 30000, ZKStringSerializer$.MODULE$);

        final ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServer.getConnectString()), false);
        AdminUtils.createTopic(zkUtils, "test", 1, 1, new Properties(), RackAwareMode.Enforced$.MODULE$);
        AdminUtils.createTopic(zkUtils, "dev", 1, 1, new Properties(), RackAwareMode.Enforced$.MODULE$);
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        if (kafkaServer != null) {
            kafkaServer.shutdown();
        }
        if (zkServer != null) {
            zkServer.stop();
        }
    }
    
    @org.junit.Test
    public void testAuthorizedRead() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        producerProps.put("sasl.mechanism", "GSSAPI");
        
        producerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        producerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.getClass().getResource("/servicestore.jks").getPath());
        producerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        producerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.getClass().getResource("/truststore.jks").getPath());
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final Producer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create the Consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:" + port);
        consumerProps.put("group.id", "test");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        consumerProps.put("sasl.mechanism", "GSSAPI");
        
        consumerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        consumerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.getClass().getResource("/clientstore.jks").getPath());
        consumerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "cspass");
        consumerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "ckpass");
        consumerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.getClass().getResource("/truststore.jks").getPath());
        consumerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList("test"));
        
        // Send a message
        producer.send(new ProducerRecord<String, String>("test", "somekey", "somevalue"));
        producer.flush();
        
        // Poll until we consume it
        
        ConsumerRecord<String, String> record = null;
        for (int i = 0; i < 1000; i++) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            if (records.count() > 0) {
                record = records.iterator().next();
                break;
            }
        }

        Assert.assertNotNull(record);
        Assert.assertEquals("somevalue", record.value());

        producer.close();
        consumer.close();
    }
    
    // "alice" is not authorized to write to "dev"
    @org.junit.Test
    @org.junit.Ignore
    public void testUnauthorizedWrite() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        producerProps.put("sasl.mechanism", "PLAIN");
        
        producerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        producerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, this.getClass().getResource("/servicestore.jks").getPath());
        producerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        producerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, this.getClass().getResource("/truststore.jks").getPath());
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final Producer<String, String> producer = new KafkaProducer<>(producerProps);
        
        // Send a message
        try {
            Future<RecordMetadata> record = 
                producer.send(new ProducerRecord<String, String>("dev", "somekey", "somevalue"));
            producer.flush();
            record.get();
            Assert.fail("Authorization failure expected");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains("Not authorized to access topics"));
        }
        
        producer.close();
    }
}
