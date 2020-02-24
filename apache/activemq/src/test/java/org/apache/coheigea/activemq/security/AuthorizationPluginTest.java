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
 * Some tests for the AuthorizationPlugin
 */
public class AuthorizationPluginTest {
    
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
        AuthenticationUser dave = new AuthenticationUser("dave", "security", "guest");
        SimpleAuthenticationPlugin authenticationPlugin = new SimpleAuthenticationPlugin();
        authenticationPlugin.setUsers(Arrays.asList(alice, bob, dave));
        
        // Add authorization data
        AuthorizationEntry authorizationEntry = new AuthorizationEntry();
        authorizationEntry.setAdmin("guest");
        authorizationEntry.setRead("consumer");
        authorizationEntry.setWrite("producer");
        authorizationEntry.setQueue("testqueue");
        
        AuthorizationEntry advisoryEntry = new AuthorizationEntry();
        advisoryEntry.setAdmin("guest");
        advisoryEntry.setRead("guest");
        advisoryEntry.setWrite("guest");
        advisoryEntry.setTopic("ActiveMQ.Advisory.>");
        
        List<DestinationMapEntry> authzEntryList = Arrays.asList(authorizationEntry, advisoryEntry);
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
    public void testAliceCanProduceAndConsume() throws Exception {
        
        // Now log on and try to produce + consume
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("alice", "password");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        producer.send(message);
        
        MessageConsumer consumer = session.createConsumer(queue);
        TextMessage receivedMessage = (TextMessage)consumer.receive(1000L);
        assertEquals("Some txt", receivedMessage.getText());
        assertEquals("some value", receivedMessage.getStringProperty("some header"));
        
        connection.close();
    }
    
    @org.junit.Test
    public void testBobCanConsume() throws Exception {
        
        // Now log on and try to produce + consume
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("bob", "security");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        
        try {
            session.createProducer(queue);
            fail("Expected failure as bob can't produce");
        } catch (Exception ex) {
            // expected
        }
        
        // Now create the message as alice
        Connection aliceConnection = factory.createConnection("alice", "password");
        aliceConnection.start();
        Session aliceSession = aliceConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination aliceQueue = aliceSession.createQueue("testqueue");
        MessageProducer aliceProducer = aliceSession.createProducer(aliceQueue);
        
        TextMessage aliceMessage = aliceSession.createTextMessage("Some txt");
        aliceMessage.setStringProperty("some header", "some value");
        
        aliceProducer.send(aliceMessage);
        aliceConnection.close();
        
        MessageConsumer consumer = session.createConsumer(queue);
        TextMessage receivedMessage = (TextMessage)consumer.receive(1000L);
        assertEquals("Some txt", receivedMessage.getText());
        assertEquals("some value", receivedMessage.getStringProperty("some header"));
        
        connection.close();
    }
    
    @org.junit.Test
    public void testDaveCantConsume() throws Exception {
        
        // Now log on and try to produce + consume
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("dave", "security");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        
        try {
            session.createProducer(queue);
            fail("Expected failure as dave can't produce");
        } catch (Exception ex) {
            // expected
        }
        
        // Now create the message as alice
        Connection aliceConnection = factory.createConnection("alice", "password");
        aliceConnection.start();
        Session aliceSession = aliceConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination aliceQueue = aliceSession.createQueue("testqueue");
        MessageProducer aliceProducer = aliceSession.createProducer(aliceQueue);
        
        TextMessage aliceMessage = aliceSession.createTextMessage("Some txt");
        aliceMessage.setStringProperty("some header", "some value");
        
        aliceProducer.send(aliceMessage);
        aliceConnection.close();
        
        try {
            session.createConsumer(queue);
            fail("Expected failure as dave can't consume");
        } catch (Exception ex) {
            // expected
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testAliceTempDestination() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("alice", "password");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        session.createTemporaryQueue();
        
        connection.close();
    }
    
}
