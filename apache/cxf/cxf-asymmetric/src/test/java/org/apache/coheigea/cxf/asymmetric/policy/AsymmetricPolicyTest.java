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
package org.apache.coheigea.cxf.asymmetric.policy;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * Three test-cases for a CXF endpoint using an Asymmetric binding. In all cases, the SOAP
 * Body is signed and encrypted. The client also includes the signing token in the security
 * header of the request, which the server uses to encrypt the response to the client.
 * 
 *  - testAsymmetricOAEP: This uses RSA OAEP + AES-CBC
 *  - testAsymmetricPKCS12: This uses RSA PKCS12 + AES-CBC
 *  - testAsymmetricGCM: This uses RSA OAEP + AES-GCM
 */
public class AsymmetricPolicyTest extends AbstractBusClientServerTestBase {
    
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
    // @org.junit.Ignore
    public void testAsymmetricOAEP() throws Exception {

        // Endpoint is at:
        // http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricoaep
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricPolicyTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AsymmetricPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricOAEPPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    // @org.junit.Ignore
    public void testAsymmetricPKCS12() throws Exception {

        // Endpoint is at:
        // http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricpkcs12
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricPolicyTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AsymmetricPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPKCS12Port");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    // @org.junit.Ignore
    public void testAsymmetricGCM() throws Exception {

        // Endpoint is at:
        // http://localhost:" + PORT + "/doubleit/services/doubleitasymmetricgcm
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricPolicyTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AsymmetricPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricGCMPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}
