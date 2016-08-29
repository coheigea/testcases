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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.TestingServer;

import kafka.admin.AdminUtils;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import kafka.utils.TestUtils;
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
        ZkClient zkClient = new ZkClient(zkServer.getConnectString(), 30000, 30000);
        AdminUtils.createTopic(zkClient, "test", 1, 1, new Properties());
        
        Properties props = new Properties();
        props.put("metadata.broker.list", zkServer.getConnectString());
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //props.put("producer.type", "async");
        props.put("batch.size", "1");
        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);
        producer.send(new KeyedMessage<String, String>("test","b***REMOVED***2"));
    }
    
    private static KafkaConfig getKafkaConfig(final String zkConnectString) {
        
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

        props.put("zookeeper.connect", zkConnectString);
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
