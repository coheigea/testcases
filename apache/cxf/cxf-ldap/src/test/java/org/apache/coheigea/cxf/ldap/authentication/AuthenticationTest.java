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
package org.apache.coheigea.cxf.ldap.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This tests using LDAP as for authentication. A cxf client sends a SOAP UsernameToken to a CXF
 * Endpoint. The CXF Endpoint has been configured (see cxf-service.xml) to validate the UsernameToken 
 * via the JAASUsernameTokenValidator, which dispatches it to a LDAP directory for authentication.
 */

@RunWith(FrameworkRunner.class)

//Define the DirectoryService
@CreateDS(name = "AuthenticationTest-class",
  enableAccessControl = false,
  allowAnonAccess = false,
  enableChangeLog = true,
  partitions = {
    @CreatePartition(
        name = "example",
        suffix = "dc=example,dc=com",
        indexes = {
            @CreateIndex(attribute = "objectClass"),
            @CreateIndex(attribute = "dc"),
            @CreateIndex(attribute = "ou")
        } )
    }
)

@CreateLdapServer(
  transports = {
      @CreateTransport(protocol = "LDAP", address = "127.0.0.1")
  }
)

//Inject an file containing entries
@ApplyLdifFiles("ldap.ldif")

public class AuthenticationTest extends AbstractLdapTestUnit {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = TestUtil.getPortNumber(Server.class);
    
    private static boolean portUpdated;
    
    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();
        
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   AbstractBusClientServerTestBase.launchServer(Server.class, true)
        );
    }

    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }
            
            // Read in jaas file and substitute in the correct port
            File f = new File(basedir +
                              "/src/test/resources/org/apache/coheigea/cxf/ldap/authentication/ldap.jaas");
            
            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("portno", "" + super.getLdapServer().getPort());
            
            File f2 = new File(basedir 
                               + "/target/test-classes/org/apache/coheigea/cxf/ldap/authentication/ldap.jaas");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            
            System.setProperty("java.security.auth.login.config", f2.getPath());
            
            portUpdated = true;
        }
    }
   
    @org.junit.Test
    public void testAuthenticatedRequestViaSun() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSunPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthenticatedRequestviaSun() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSunPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        try {
            doubleIt(transportPort, 25);
            Assert.fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testAuthenticatedRequestViaJetty() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportJettyPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthenticatedRequestViaJetty() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportJettyPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        try {
            doubleIt(transportPort, 25);
            Assert.fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testAuthenticatedRequestViaKaraf() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKarafPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthenticatedRequestViaKaraf() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKarafPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        try {
            doubleIt(transportPort, 25);
            Assert.fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}