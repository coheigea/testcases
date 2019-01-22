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
package org.apache.coheigea.cxf.kms.symmetric;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Two test-cases for a CXF endpoint using symmetric encryption (AES-128 + AES-256). Each test-case asks a CallbackHandler for the secret
 * key to encrypt the request. In this case, the CallbackHandler asks the Amazon KMS (Key Management Service) for a data encryption key,
 * using the master key for "alice".
 * 
 * See the README for the prerequisites for running this test.
 */

@org.junit.Ignore
public class SymmetricActionTest extends AbstractBusClientServerTestBase {

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
    public void testSymmetricAES128() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SymmetricActionTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SymmetricActionTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testSymmetricAES256() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SymmetricActionTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SymmetricActionTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric256Port");
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