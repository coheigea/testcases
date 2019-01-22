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
package org.apache.coheigea.cxf.kms.asymmetric;

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
 * A test-case for a CXF based web service using Asymmetric encryption via WS-SecurityPolicy. The keystore password for both signature and
 * encryption is stored encrypted in the crypto properties files using the AWS KMS (Key Management Service) via the KMSPasswordEncryptor class.
 * 
 * See the README for the prerequisites for running this test.
 */

@org.junit.Ignore
public class AsymmetricTest extends AbstractBusClientServerTestBase {

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
    @org.junit.Ignore
    public void testEncryptedPasswords() throws Exception {
        KMSPasswordEncryptor passwordEncryptor = new KMSPasswordEncryptor();
        passwordEncryptor.setEndpoint("https://kms.eu-west-1.amazonaws.com");
        passwordEncryptor.setAccessKey("<access key>");
        passwordEncryptor.setSecretKey("<secret key>");
        passwordEncryptor.setMasterKeyId("<master key id>");
        
        String encryptedPassword = passwordEncryptor.encrypt("cspass");
        System.out.println("ENC: " + encryptedPassword);
        
        String decryptedPassword = passwordEncryptor.decrypt(encryptedPassword);
        System.out.println("DEC: " + decryptedPassword);
    }

    @org.junit.Test
    public void testAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AsymmetricTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = AsymmetricTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
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