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

import java.util.Arrays;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.curator.test.TestingServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

/**
 */
public class KafkaTest {
    
    private static KafkaServerStartable kafkaServer;
    private static TestingServer zkServer;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        zkServer = new TestingServer();
        System.out.println("CONNECT: " + zkServer.getConnectString());
        KafkaConfig config = getKafkaConfig(zkServer.getConnectString());
        kafkaServer = new KafkaServerStartable(config);
        kafkaServer.startup();
    }
    
    @org.junit.Test
    public void testKafka() throws Exception {
        
        ZkClient zkClient = new ZkClient(zkServer.getConnectString(), 30000, 30000, ZKStringSerializer$.MODULE$);
        
        final ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServer.getConnectString()), false);
        AdminUtils.createTopic(zkUtils, "test", 1, 1, new Properties(), RackAwareMode.Enforced$.MODULE$);
        
        Properties clientProps = new Properties();
        clientProps.put("bootstrap.servers", "localhost:12345");
        clientProps.put("group.id", "test");
        clientProps.put("serializer.class", "kafka.serializer.StringEncoder");
        clientProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        clientProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        clientProps.put("acks", "all");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer
            <String, String>(clientProps);
        consumer.subscribe(Arrays.asList("test"));
        
        Properties props = new Properties();
        props.put("metadata.broker.list", "localhost:12345");
        props.put("bootstrap.servers", "localhost:12345");
        props.put("acks", "all");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //props.put("producer.type", "async");
        props.put("batch.size", "1");
        props.put("request.required.acks", "1");
        props.put("client.id", "producertest");
        props.put("key.serializer", 
            "org.apache.kafka.common.serialization.StringSerializer");
         props.put("value.serializer", 
            "org.apache.kafka.common.serialization.StringSerializer");
         
        
        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);
        producer.send(new KeyedMessage<String, String>("test","b***REMOVED***2"));
        producer.close();
        
        Thread.sleep(1000);
        
        ConsumerRecords<String, String> records = consumer.poll(100);
        System.out.println("REC: " + records.count());
        for (ConsumerRecord<String, String> record : records)
            
            // print the offset,key and value for the consumer records.
            System.out.printf("BLAH offset = %d, key = %s, value = %s\n", 
               record.offset(), record.key(), record.value());
        
    }
    
    private static KafkaConfig getKafkaConfig(final String zkConnectString) {
        /*
        scala.collection.Iterator<Properties> propsI =
            TestUtils.createBrokerConfigs(1, true).iterator();
        assert propsI.hasNext();
        Properties props = propsI.next();
        assert props.containsKey("zookeeper.connect");
        // Props
        //KEY: controlled.shutdown.enable true
        //KEY: port 41085
        //KEY: broker.id 0
        //KEY: host.name localhost
        //KEY: zookeeper.connect 127.0.0.1:43422
        //KEY: replica.socket.timeout.ms 1500
        //KEY: log.dir /tmp/kafka-115764
        */
        
        Properties props = new Properties();
        props.put("broker.id", 1);
        props.put("host.name", "localhost");
        props.put("port", "12345");
        props.put("log.dir", "/tmp/kafka");
        props.put("zookeeper.connect", zkConnectString);
        props.put("replica.socket.timeout.ms", "1500");
        props.put("controlled.shutdown.enable", Boolean.TRUE.toString());

        // props.put("zookeeper.connect", zkConnectString);
        return new KafkaConfig(props);
        /*
        Properties props = new Properties();
        props.put("port", "9092");
        props.put("broker.id", "1");
        props.put("log.dir", Files.createTempDir());
        props.put("zookeeper.connect",zkConnectString);
        KafkaConfig kafkaConfig = new KafkaConfig(props);
        return kafkaConfig;
        */
    }
}
