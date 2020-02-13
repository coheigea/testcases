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
package org.apache.coheigea.activemq.transports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.Arrays;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.SslContext;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

/**
 * Basic test for message producing + consumer using the SSL connector with client authentication enabled
 */
public class SSLClientAuthTest {
    
    private static BrokerService broker;
    private static String brokerAddress;
    
    @org.junit.BeforeClass
    public static void startBroker() throws Exception {
        
        broker = new BrokerService();
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.setDataDirectory("target/activemq-data");
        
        // Configure TLS on the broker
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream istream = SSLClientAuthTest.class.getClassLoader().getResourceAsStream("servicestore.jks");
        keyStore.load(istream, "sspass".toCharArray());
        keyManagerFactory.init(keyStore, "skpass".toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream trustItream = SSLClientAuthTest.class.getClassLoader().getResourceAsStream("truststore.jks");
        trustStore.load(trustItream, "security".toCharArray());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        
        SslContext sslContext = new SslContext();
        sslContext.setKeyManagers(Arrays.asList(keyManagers));
        sslContext.setTrustManagers(Arrays.asList(trustManagers));
        broker.setSslContext(sslContext);
        
        ServerSocket serverSocket = new ServerSocket(0);
        int brokerPort = serverSocket.getLocalPort();
        serverSocket.close();
        
        brokerAddress = "ssl://localhost:" + brokerPort;
        broker.addConnector(brokerAddress + "?transport.needClientAuth=true");
        broker.start();
    }
    
    @org.junit.AfterClass
    public static void stopBroker() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }
    
    @org.junit.Test
    public void testBasicTextMessage() throws Exception {
        ActiveMQSslConnectionFactory factory = new ActiveMQSslConnectionFactory(brokerAddress + "?socket.verifyHostName=false");
        
        // Configure TLS for the client
        factory.setTrustStore("truststore.jks");
        factory.setTrustStorePassword("security");
        factory.setKeyStore("servicestore.jks");
        factory.setKeyStorePassword("sspass");
        factory.setKeyStoreKeyPassword("skpass");
        
        Connection connection = factory.createConnection();
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
    public void testMissingKeyStore() throws Exception {
        ActiveMQSslConnectionFactory factory = new ActiveMQSslConnectionFactory(brokerAddress + "?socket.verifyHostName=false");
        
        // Configure TLS for the client
        factory.setTrustStore("truststore.jks");
        factory.setTrustStorePassword("security");
        
        try {
            factory.createConnection();
            fail("Failure expected");
        } catch (Exception ex) {
            // expected failure
        }
    }
    
    @org.junit.Test
    public void testUntrustedClientKey() throws Exception {
        ActiveMQSslConnectionFactory factory = new ActiveMQSslConnectionFactory(brokerAddress + "?socket.verifyHostName=false");
        
        // Configure TLS for the client
        factory.setTrustStore("truststore.jks");
        factory.setTrustStorePassword("security");
        factory.setKeyStore("imposter.jks");
        factory.setKeyStorePassword("ispass");
        factory.setKeyStoreKeyPassword("ikpass");
        
        try {
            factory.createConnection();
            fail("Failure expected");
        } catch (Exception ex) {
            // expected failure
        }
    }
    
}
