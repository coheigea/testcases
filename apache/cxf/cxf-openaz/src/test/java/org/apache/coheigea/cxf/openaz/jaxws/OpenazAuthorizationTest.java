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
package org.apache.coheigea.cxf.openaz.jaxws;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.coheigea.cxf.openaz.common.SamlRoleCallbackHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * An authorization test for a JAX-WS service using XACML and the OpenAZ project.
 * A "double-it" client creates a signed SAML Token containing a given role. This is sent to the
 * service, which validates the SAML Token + then invokes a PEP which creates a XACML Request.
 * A PDP based on OpenAZ is colocated with the PEP + makes an authorization decision based on
 * a set of policies. Finally the PEP enforces the decision of the PDP.
 */
public class OpenazAuthorizationTest extends AbstractBusClientServerTestBase {
    
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
        URL busFile = OpenazAuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = OpenazAuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        SamlRoleCallbackHandler roleCallbackHandler = new SamlRoleCallbackHandler();
        roleCallbackHandler.setSignAssertion(true);
        roleCallbackHandler.setRoleName("manager");
        ((BindingProvider)transportPort).getRequestContext().put(
            "ws-security.saml-callback-handler", roleCallbackHandler
        );

        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthorizedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = OpenazAuthorizationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = OpenazAuthorizationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        SamlRoleCallbackHandler roleCallbackHandler = new SamlRoleCallbackHandler();
        roleCallbackHandler.setSignAssertion(true);
        roleCallbackHandler.setRoleName("employee");
        ((BindingProvider)transportPort).getRequestContext().put(
            "ws-security.saml-callback-handler", roleCallbackHandler
        );
        
        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on employee role");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}
