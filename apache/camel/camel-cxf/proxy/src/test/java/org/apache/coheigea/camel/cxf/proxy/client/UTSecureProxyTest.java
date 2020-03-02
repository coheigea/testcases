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
package org.apache.coheigea.camel.cxf.proxy.client;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.camel.spring.Main;
import org.apache.coheigea.camel.cxf.proxy.service.Server;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

/**
 * A secure proxy test. A CXF JAX-WS "double-it" service requires TLS with client
 * authentication. A CXF proxy service also requires TLS, but with a UsernameToken
 * instead of client authentication. A Camel route is configured to route messages
 * from the proxy to the service. A client can invoke on the proxy with a UsernameToken
 * over TLS, the proxy authenticates the UsernameToken, and passes the call + response
 * to the backend service. In this way, we are delegating the security requirements
 * to the proxy (and ensuring the service is secure against external calls by requiring
 * 2-way TLS). 
 */
public class UTSecureProxyTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = allocatePort(Server.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PROXY_PORT = allocatePort(Server.class, 2);
    
    private static Main main;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        
        // Start up the Camel route
        main = new Main();
        main.setApplicationContextUri("cxf-proxy.xml");
        main.start();
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        main.stop();
    }
   
    @org.junit.Test
    public void testUTSecureProxy() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UTSecureProxyTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = UTSecureProxyTest.class.getResource("../proxyservice/DoubleItProxy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PROXY_PORT);
        
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "security");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUTSecureProxyWrongPassword() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UTSecureProxyTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = UTSecureProxyTest.class.getResource("../proxyservice/DoubleItProxy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PROXY_PORT);
        
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)transportPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "bad-security");
        
        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on a bad password");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}
