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
package org.apache.coheigea.activemq.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.filter.DestinationMapEntry;
import org.apache.activemq.security.AuthenticationUser;
import org.apache.activemq.security.AuthorizationEntry;
import org.apache.activemq.security.AuthorizationMap;
import org.apache.activemq.security.AuthorizationPlugin;
import org.apache.activemq.security.DefaultAuthorizationMap;
import org.apache.activemq.security.SimpleAuthenticationPlugin;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Test for receiving messages from multiple destinations using wildcards
 */
public class WildcardAuthorizationTest {
    
    private static BrokerService broker;
    private static String brokerAddress;
    
    @org.junit.BeforeClass
    public static void startBroker() throws Exception {
        
        broker = new BrokerService();
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.setDataDirectory("target/activemq-data");
        
        // Add authentication data
        AuthenticationUser alice = new AuthenticationUser("alice", "password", "producer,consumer,guest");
        AuthenticationUser bob = new AuthenticationUser("bob", "security", "consumer,guest");
        SimpleAuthenticationPlugin authenticationPlugin = new SimpleAuthenticationPlugin();
        authenticationPlugin.setUsers(Arrays.asList(alice, bob));
        
        // Add authorization data
        AuthorizationEntry fooAuthorizationEntry = new AuthorizationEntry();
        fooAuthorizationEntry.setAdmin("guest");
        fooAuthorizationEntry.setRead("consumer");
        fooAuthorizationEntry.setWrite("producer");
        fooAuthorizationEntry.setQueue("testqueue.foo");
        
        // Here only the producer can read testqueue.bar
        AuthorizationEntry barAuthorizationEntry = new AuthorizationEntry();
        barAuthorizationEntry.setAdmin("guest");
        barAuthorizationEntry.setRead("producer");
        barAuthorizationEntry.setWrite("producer");
        barAuthorizationEntry.setQueue("testqueue.bar");
        
        AuthorizationEntry advisoryEntry = new AuthorizationEntry();
        advisoryEntry.setAdmin("guest");
        advisoryEntry.setRead("guest");
        advisoryEntry.setWrite("guest");
        advisoryEntry.setTopic("ActiveMQ.Advisory.>");
        
        List<DestinationMapEntry> authzEntryList = Arrays.asList(fooAuthorizationEntry, barAuthorizationEntry, advisoryEntry);
        AuthorizationMap authorizationMap = new DefaultAuthorizationMap(authzEntryList);
        AuthorizationPlugin authorizationPlugin = new AuthorizationPlugin(authorizationMap);
        
        broker.setPlugins(new BrokerPlugin[] { authenticationPlugin, authorizationPlugin });
        
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
    public void testAliceCanConsume() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("alice", "password");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        sendMessage(session, "testqueue.foo");
        sendMessage(session, "testqueue.bar");
        
        Connection consumerConnection = factory.createConnection("alice", "password");
        consumerConnection.start();
        
        Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        receiveMessage(consumerSession, "testqueue.*");
        
        connection.close();
        consumerConnection.close();
    }
    
    @org.junit.Test
    public void testBobCantConsume() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("alice", "password");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        sendMessage(session, "testqueue.foo");
        sendMessage(session, "testqueue.bar");
        
        Connection consumerConnection = factory.createConnection("bob", "security");
        consumerConnection.start();
        
        Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            receiveMessage(consumerSession, "testqueue.*");
            fail("Failure expected when trying to read testqueue.bar");
        } catch (NullPointerException ex) {
            // expected
        }
        
        connection.close();
        consumerConnection.close();
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
