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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.ServerSocket;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.scheduler.memory.InMemoryJobSchedulerStore;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Basic test for message scheduling
 */
public class MessageSchedulingTest {
    
    private static BrokerService broker;
    private static String brokerAddress;
    
    @org.junit.BeforeClass
    public static void startBroker() throws Exception {
        
        broker = new BrokerService();
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.setJobSchedulerStore(new InMemoryJobSchedulerStore());
        broker.setDataDirectory("target/activemq-data");
        broker.setSchedulerSupport(true);
        
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
    public void testScheduledDelay() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        // Deliver the message after a 5 second delay
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 5000L);
        
        MessageConsumer consumer = session.createConsumer(queue);
        
        producer.send(message);
        
        // The message won't be received
        TextMessage receivedMessage = (TextMessage)consumer.receive(1000L);
        assertNull(receivedMessage);
        
        // Now sleep for a while and the message should be received
        Thread.sleep(4000L);
        receivedMessage = (TextMessage)consumer.receive(1000L);
        assertEquals("Some txt", receivedMessage.getText());
        
        connection.close();
    }
    
    @org.junit.Test
    public void testNegativeDelay() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        try {
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, -1L);
            fail("Failure expected on a negative value");
        } catch (MessageFormatException ex) {
            assertEquals("AMQ_SCHEDULED_DELAY must not be a negative value", ex.getMessage());
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testNegativeRepeat() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        try {
            message.setIntProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, -1);
            fail("Failure expected on a negative value");
        } catch (MessageFormatException ex) {
            assertEquals("AMQ_SCHEDULED_REPEAT must not be a negative value", ex.getMessage());
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testNegativePeriod() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        try {
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, -1L);
            fail("Failure expected on a negative value");
        } catch (MessageFormatException ex) {
            assertEquals("AMQ_SCHEDULED_PERIOD must not be a negative value", ex.getMessage());
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testScheduledDelayViaCron() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        
        try {
            message.setStringProperty(ScheduledMessage.AMQ_SCHEDULED_CRON, "-1 * * * *");
            fail("Failure expected on a negative value");
        } catch (NumberFormatException ex) {
            // expected
        }
        
        connection.close();
    }
    
    @org.junit.Test
    public void testSendLotsofMessages() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 0L);
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, 0L);
        message.setIntProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, Integer.MAX_VALUE);
        
        try {
            producer.send(message);
            fail("Failure expected on too large a repeat value");
        } catch (MessageFormatException ex) {
            assertEquals("The scheduled repeat value is too large", ex.getMessage());
        }

        connection.close();
    }
    
    @org.junit.Test
    public void testRepeat() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerAddress);
        Connection connection = factory.createConnection();
        connection.start();
        
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue("testqueue");
        MessageProducer producer = session.createProducer(queue);
        
        TextMessage message = session.createTextMessage("Some txt");
        message.setStringProperty("some header", "some value");
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 0L);
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, 0L);
        message.setIntProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, 2);
        
        producer.send(message);

        connection.close();
    }
    
}
