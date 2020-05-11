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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.util.HashSet;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * This tests sending ObjectMessages. By default ActiveMQ does not allow custom Objects to be serialized.
 */
public class ObjectMessageTest {
    
    private static BrokerService broker;
    private static String brokerAddress;
    
    @org.junit.BeforeClass
    public static void startBroker() throws Exception {
        
        //System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");
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
    public void testCustomObjectMessageFailedByDefault() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        ObjectMessage message = session.createObjectMessage(new AttackObject());
        message.setStringProperty("some header", "some value");
        
        producer.send(message);
        
        MessageConsumer consumer = session.createConsumer(queue);
        ObjectMessage receivedMessage = (ObjectMessage)consumer.receive(1000L);
        try {
            receivedMessage.getObject();
            fail("Failure expected on a custom Object");
        } catch (Exception ex) {
            // expected
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testAllowCustomObjects() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        factory.setTrustAllPackages(true);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        ObjectMessage message = session.createObjectMessage(new AttackObject());
        message.setStringProperty("some header", "some value");
        
        producer.send(message);
        
        MessageConsumer consumer = session.createConsumer(queue);
        ObjectMessage receivedMessage = (ObjectMessage)consumer.receive(1000L);
        Object receivedObject = receivedMessage.getObject();
        assertNotNull(receivedObject);
        assertTrue(receivedObject instanceof AttackObject);
        assertEquals("some value", receivedMessage.getStringProperty("some header"));
        
        connection.close();
    }
    
    @org.junit.Test
    public void testDenialOfServiceAttack() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        // Ref: https://owasp.org/www-community/vulnerabilities/Deserialization_of_untrusted_data
        HashSet<Object> root = new HashSet<>();
        HashSet<Object> s1 = root;
        HashSet<Object> s2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            HashSet<Object> t1 = new HashSet<>();
            HashSet<Object> t2 = new HashSet<>();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }

        ObjectMessage message = session.createObjectMessage(root);
        message.setStringProperty("some header", "some value");
        
        System.out.println("Sending message");
        producer.send(message);
        System.out.println("Receiving message");
        
        MessageConsumer consumer = session.createConsumer(queue);
        ObjectMessage receivedMessage = (ObjectMessage)consumer.receive(1000L);
        try {
            receivedMessage.getObject();
            fail("Failure expected on a custom Object");
        } catch (Exception ex) {
            // expected
        }
        
        connection.close();
    }
    
    private static class AttackObject implements Serializable {
        private static final long serialVersionUID = -7249059185723713380L;

        private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
//            ProcessBuilder builder = new ProcessBuilder("gnome-calculator");
//            builder.start();
        }
        
    }
    
}
