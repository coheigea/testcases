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
package org.apache.coheigea.cxf.failover.feature;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.clustering.circuitbreaker.CircuitBreakerFailoverFeature;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

import org.apache.coheigea.cxf.failover.common.Number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test for CXF using the failover feature. 
 */
public class FailoverTest extends AbstractBusClientServerTestBase {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    static final String PORT1 = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String PORT3 = allocatePort(Server.class, 3);
    static final String PORT4 = allocatePort(Server.class, 4);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
    }

    // The service is designed to fail (404) every second iteration.
    // The first invocation on PORT1 works fine. The second fails, but the client fails over to PORT2
    // automatically, and the invocation succeeds.
    @org.junit.Test
    public void testFailoverFeature() throws Exception {

        URL busFile = FailoverTest.class.getResource("cxf-client.xml");

        FailoverFeature feature = new FailoverFeature();
        SequentialStrategy strategy = new SequentialStrategy();
        List<String> addresses = new ArrayList<>();
        addresses.add("http://localhost:" + PORT2 + "/doubleit/services");
        strategy.setAlternateAddresses(addresses);
        feature.setStrategy(strategy);

        String address = "http://localhost:" + PORT1 + "/doubleit/services";
        WebClient client = WebClient.create(address, null,
                                            Collections.singletonList(feature), busFile.toString());
        client = client.type("application/xml");

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        // First call is successful to PORT1
        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);

        // Second call fails over to PORT2
        response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }
    
    // This call will fail on the second attempt, as the service sleeps for 30s, and the
    // client has been configured with a receive timeout of 20s. It should fail over
    // successfully to the second endpoint
    @org.junit.Test
    public void testFailoverFeatureJAXWS() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FailoverTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = FailoverTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT3);
        
        // Make a successful call
        doubleIt(port, 25);
        
        // Failover
        doubleIt(port, 30);
    }

    // Here we configure a custom failover target selector. It won't failover on a SocketTimeoutException
    // so failover fails
    @org.junit.Test
    public void testFailoverFeatureJAXWSCustomTargetSelector() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FailoverTest.class.getResource("cxf-client-custom-failover.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = FailoverTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT3);
        
        // Make a successful call
        doubleIt(port, 25);
        
        try {
            doubleIt(port, 30);
            fail("Failure expected on a SocketTimeoutException");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
    @org.junit.Test
    public void testCircuitBreaker() throws Exception {

        URL busFile = FailoverTest.class.getResource("cxf-client.xml");

        CircuitBreakerFailoverFeature feature = new CircuitBreakerFailoverFeature();
        feature.setThreshold(2);
        SequentialStrategy strategy = new SequentialStrategy();
        strategy.setDelayBetweenRetries(3000L);
        List<String> addresses = new ArrayList<>();
        addresses.add("http://localhost:" + PORT2 + "/doubleit/services");
        strategy.setAlternateAddresses(addresses);
        feature.setStrategy(strategy);

        String address = "http://localhost:" + PORT1 + "/doubleit/services";
        WebClient client = WebClient.create(address, null,
                                            Collections.singletonList(feature), busFile.toString());
        client = client.type("application/xml");

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        // First call is successful to PORT1
        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);

        // Second call fails over to PORT2
        response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

}