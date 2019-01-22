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
package org.apache.coheigea.cxf.sts.xacml.authorization.xacml2;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.coheigea.cxf.sts.xacml.common.STSServer;
import org.apache.coheigea.cxf.sts.xacml.common.TokenTestUtils;
import org.apache.coheigea.cxf.sts.xacml.pdp.xacml2.PdpServer;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The client authenticates to the STS using a username/password, and gets a signed holder-of-key 
 * SAML Assertion in return. This is presented to the service, who verifies proof-of-possession + 
 * the signature of the STS on the assertion. The CXF endpoint extracts roles from the Assertion + 
 * populates the security context. Note that the CXF endpoint requires a "role" Claim via the
 * security policy.
 *
 * The CXF Endpoint has configured the XACMLAuthorizingInterceptor, which creates a XACML 2.0 request 
 * for dispatch to the PDP, and then enforces the PDP's decision. The mocked PDP is a REST service, 
 * that requires that a user must have role "boss" to access the "doubleIt" operation ("alice" has 
 * this role, "bob" does not).
 */
public class XACML2AuthorizationTest extends AbstractBusClientServerTestBase {
    
    public static final String PDP_PORT = allocatePort(PdpServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(Server.class);
    private static final String STS_PORT = allocatePort(STSServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(STSServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(PdpServer.class, true)
        );
    }
    
    @org.junit.Test
    public void testAuthorizedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XACML2AuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = XACML2AuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        TokenTestUtils.updateSTSPort((BindingProvider)transportPort, STS_PORT);
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthorizedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XACML2AuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = XACML2AuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        TokenTestUtils.updateSTSPort((BindingProvider)transportPort, STS_PORT);
        
        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}