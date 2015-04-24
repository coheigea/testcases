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
package org.apache.coheigea.cxf.kerberos.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.coheigea.cxf.kerberos.common.KerbyServer;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.spec.ticket.ServiceTicket;
import org.apache.kerby.kerberos.kerb.spec.ticket.TgtTicket;
import org.apache.wss4j.dom.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * There are two test-cases covered in this class, one that uses a WS-SecurityPolicy 
 * KerberosToken policy, and the other that uses a SpnegoContextToken policy.
 *
 * Both testcases start up a KDC locally using Apache Kerby. In each case, the service endpoint 
 * has a TransportBinding policy, with a corresponding EndorsingSupportingToken which is either 
 * a KerberosToken or SpnegoContextToken. The client will obtain a service ticket from the KDC
 * and include it in the security header of the service request.
 */
public class AuthenticationTest extends org.junit.Assert {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = TestUtil.getPortNumber(Server.class);
    private static final String KDC_PORT = TestUtil.getPortNumber(Server.class, 2);
    private static final String KDC_UDP_PORT = TestUtil.getPortNumber(Server.class, 3);
    
    private static KerbyServer kerbyServer;
    
    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();
        
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        
        updatePort(basedir);

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/src/test/resources/kerberos/kerberos.jaas");
        
        Assert.assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   AbstractBusClientServerTestBase.launchServer(Server.class, true)
        );
        
        kerbyServer = new KerbyServer();
        
        kerbyServer.setKdcHost("localhost");
        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        kerbyServer.setAllowUdp(true);
        kerbyServer.setKdcUdpPort(Integer.parseInt(KDC_UDP_PORT));
        
        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl());
        kerbyServer.init();
        
        // Need to disable PRE_AUTH ?
        kerbyServer.getSetting().getKdcConfig().setBoolean(KdcConfigKey.PREAUTH_REQUIRED, false);
        kerbyServer.getSetting().getKdcConfig().setString(KdcConfigKey.TGS_PRINCIPAL, 
                                                          "krbtgt/service.ws.apache.org@service.ws.apache.org");
        
        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.createPrincipal("krbtgt/service.ws.apache.org@service.ws.apache.org", "krbtgt");
        kerbyServer.start();
    }
    
    @AfterClass
    public static void tearDown() {
    	if (kerbyServer != null) {
            kerbyServer.stop();
    	}
    }
    
    private static void updatePort(String basedir) throws Exception {

    	// Read in krb5.conf and substitute in the correct port
    	File f = new File(basedir + "/src/test/resources/kerberos/krb5.conf");

    	FileInputStream inputStream = new FileInputStream(f);
    	String content = IOUtils.toString(inputStream, "UTF-8");
    	inputStream.close();
    	content = content.replaceAll("port", KDC_PORT);

    	File f2 = new File(basedir + "/target/test-classes/kerberos/krb5.conf");
    	FileOutputStream outputStream = new FileOutputStream(f2);
    	IOUtils.write(content, outputStream, "UTF-8");
    	outputStream.close();

    	System.setProperty("java.security.krb5.conf", f2.getPath());
    }

    @org.junit.Test
    @org.junit.Ignore
    public void unitTest() throws Exception {
    	KrbClient client = new KrbClient();

    	client.setKdcHost("localhost");
    	client.setKdcTcpPort(Integer.parseInt(KDC_PORT));
    	// client.setAllowUdp(true);
    	// client.setKdcUdpPort(Integer.parseInt(KDC_UDP_PORT));

    	client.setTimeout(5);
    	client.setKdcRealm(kerbyServer.getSetting().getKdcRealm());
        
        File testDir = new File(System.getProperty("test.dir", "target"));
        File testConfDir = new File(testDir, "conf");
        client.setConfDir(testConfDir);
        client.init();

        TgtTicket tgt;
        ServiceTicket tkt;

        try {
            tgt = client.requestTgtWithPassword("alice@service.ws.apache.org", "alice");
            assertTrue(tgt != null);

            tkt = client.requestServiceTicketWithTgt(tgt, "bob/service.ws.apache.org@service.ws.apache.org");
            assertTrue(tkt != null);
        } catch (Exception e) {
        	e.printStackTrace();
            Assert.fail();
        }
    }
    
    @org.junit.Test
    public void testKerberos() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
    
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testSpnego() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSpnegoTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        Assert.assertEquals(numToDouble * 2 , resp);
    }
    
}
