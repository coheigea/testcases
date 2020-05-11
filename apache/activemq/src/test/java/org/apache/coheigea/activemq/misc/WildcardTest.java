/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.coheigea.activemq.misc;

import static org.junit.Assert.assertEquals;

import java.net.ServerSocket;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Test for receiving messages from multiple destinations using wildcards
 */
public class WildcardTest {
    
    private static BrokerService broker;
    private static String brokerAddress;
    
    @org.junit.BeforeClass
    public static void startBroker() throws Exception {
        
        broker = new BrokerService();
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.setDataDirectory("target/activemq-data");
        
        ServerSocket serverSocket = new ServerSocket(0);
        int brokerPort = serverSocket.getLocalPort();
        serverSocket.close();
        
        brokerAddress = "tcp://localhost:" + brokerPort;
        broker.addConnector(brokerAddress);
        broker.start();
    }
    
    @org.junit.AfterClass
    public static void stopBroker() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }
    
    @org.junit.Test
    public void testWildcardSubscription() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        sendMessage(session, "testqueue.foo");
        sendMessage(session, "testqueue.bar");
        
        receiveMessage(session, "testqueue.*");
        
        connection.close();
    }
    
    private void sendMessage(Session session, String queueName) throws JMSException {
        Destination queue = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        producer.send(message);
    }
    
    private void receiveMessage(Session session, String queueName) throws JMSException {
        Destination queue = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(queue);
        TextMessage receivedMessage = (TextMessage)consumer.receive(1000L);
        assertEquals("Some txt", receivedMessage.getText());
        assertEquals("some value", receivedMessage.getStringProperty("some header"));
        
        receivedMessage = (TextMessage)consumer.receive(1000L);
        assertEquals("Some txt", receivedMessage.getText());
        assertEquals("some value", receivedMessage.getStringProperty("some header"));
    }
    
}
