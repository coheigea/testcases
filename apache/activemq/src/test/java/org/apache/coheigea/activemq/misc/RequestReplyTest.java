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
import static org.junit.Assert.assertTrue;

import java.net.ServerSocket;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Basic test for request-reply using a temporary destination
 */
public class RequestReplyTest {
    
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
    public void testRequestReply() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        // Consumer
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        
        MessageConsumer consumer = session.createConsumer(queue);
        ConsumerMessageListener consumerMessageListener = new ConsumerMessageListener(session);
        consumer.setMessageListener(consumerMessageListener);
        
        // Producer
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        Destination tempQueue = session.createTemporaryQueue();
        message.setJMSCorrelationID(UUID.randomUUID().toString());
        message.setJMSReplyTo(tempQueue);
        
        MessageConsumer replyConsumer = session.createConsumer(tempQueue);
        ProducerMessageListener productMessageListener = new ProducerMessageListener();
        replyConsumer.setMessageListener(productMessageListener);
        
        producer.send(message);
        
        Thread.sleep(2 * 1000L);
        
        assertTrue(consumerMessageListener.isMessageReceived());
        assertTrue(productMessageListener.isMessageReceived());
        
        connection.close();
    }
    
    private static class ConsumerMessageListener implements MessageListener {
        
        private final Session session;
        private boolean messageReceived;
        
        public ConsumerMessageListener(Session session) {
            this.session = session;
        }

        @Override
        public void onMessage(Message message) {
            try {
                assertEquals("Some txt", ((TextMessage)message).getText());
                assertEquals("some value", message.getStringProperty("some header"));
                messageReceived = true;
                
                // Return a message
                TextMessage responseMsg = session.createTextMessage("Some reply");
                responseMsg.setJMSCorrelationID(message.getJMSCorrelationID());
                
                MessageProducer producer = session.createProducer(null);
                producer.send(message.getJMSReplyTo(), responseMsg);
            } catch (JMSException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public boolean isMessageReceived() {
            return messageReceived;
        }
    }
    
    private static class ProducerMessageListener implements MessageListener {
        
        private boolean messageReceived;
        
        @Override
        public void onMessage(Message message) {
            try {
                assertEquals("Some reply", ((TextMessage)message).getText());
                messageReceived = true;
            } catch (JMSException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public boolean isMessageReceived() {
            return messageReceived;
        }
    }
    
}
