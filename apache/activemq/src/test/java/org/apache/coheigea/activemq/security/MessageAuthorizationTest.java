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
import static org.junit.Assert.assertNull;

import java.net.ServerSocket;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.Message;
import org.apache.activemq.jaas.GroupPrincipal;
import org.apache.activemq.security.AuthenticationUser;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.activemq.security.SimpleAuthenticationPlugin;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Some tests for Message Authorization. alice and bob can read a message, but dave can't.
 */
public class MessageAuthorizationTest {
    
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
        
        broker.setPlugins(new BrokerPlugin[] {authenticationPlugin});
        broker.setMessageAuthorizationPolicy(new CustomMessageAuthorizationPolicy());
        
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
    public void testDaveCantConsume() throws Exception {
        
        // Now log on and try to produce + consume
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection("dave", "security");
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        producer.send(message);
        
        MessageConsumer consumer = session.createConsumer(queue);
        TextMessage receivedMessage = (TextMessage)consumer.receive(1000L);
        assertNull(receivedMessage);
        
        connection.close();
    }
    

    private static class CustomMessageAuthorizationPolicy implements MessageAuthorizationPolicy {

        @Override
        public boolean isAllowedToConsume(ConnectionContext context, Message message) {
            Set<Principal> allowedPrincipals = new HashSet<>();
            allowedPrincipals.add(new GroupPrincipal("producer"));
            allowedPrincipals.add(new GroupPrincipal("consumer"));
            return context.getSecurityContext().isInOneOf(allowedPrincipals);
        }
        
    }
    
}
