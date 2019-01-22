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
package org.apache.coheigea.cxf.shiro.authorization;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

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
 * This tests using Shiro for authorization. A cxf client sends a SOAP UsernameToken to a CXF
 * Endpoint. The CXF Endpoint has been configured (see cxf-service.xml) to validate the UsernameToken 
 * via the ShiroUTValidator. The ShiroUTValidator has also been configured with some roles, that
 * require that a user must have role "boss" to proceed ("alice" has this role, "bob" does not). 
 * 
 * Note that unlike the syncope + ldap demos, we do not use the SimpleAuthorizingInterceptor. This 
 * is because there is no straightforward way to extract roles from a Shiro Subject, only to check 
 * if the Subject has a given role.
 */
public class AuthorizationTest extends AbstractBusClientServerTestBase {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(Server.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
    }
    
    @org.junit.Test
    public void testAuthorizedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthorizedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on harry");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}