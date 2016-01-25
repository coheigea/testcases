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
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.coheigea.cxf.kerberos.common.KerbyServer;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.type.ticket.SgtTicket;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
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

        System.setProperty("sun.security.krb5.debug", "true");
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
        
        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.createPrincipal("krbtgt/service.ws.apache.org@service.ws.apache.org", "krbtgt");
        kerbyServer.start();
    }

    @AfterClass
    public static void tearDown() throws KrbException {
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
        // content = content.replaceAll("port", KDC_PORT);
        content = content.replaceAll("port", KDC_UDP_PORT);

        File f2 = new File(basedir + "/target/test-classes/kerberos/krb5.conf");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        System.setProperty("java.security.krb5.conf", f2.getPath());
    }
/*
    @org.junit.Test
    public void launchKDCTest() throws Exception {
        System.out.println("Sleeping...");
        Thread.sleep(3 * 60 * 1000);
    }
*/
    @org.junit.Test
    public void unitTest() throws Exception {
        KrbClient client = new KrbClient();

        client.setKdcHost("localhost");
        client.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        client.setAllowUdp(false);
        // client.setKdcUdpPort(Integer.parseInt(KDC_UDP_PORT));

        client.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();

        TgtTicket tgt;
        SgtTicket tkt;

        try {
            tgt = client.requestTgt("alice@service.ws.apache.org", "alice");
            assertTrue(tgt != null);

            tkt = client.requestSgt(tgt, "bob/service.ws.apache.org@service.ws.apache.org");
            assertTrue(tkt != null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @org.junit.Test
    public void unitGSSTest() throws Exception {
        LoginContext loginContext = new LoginContext("alice", new KerberosCallbackHandler());
        loginContext.login();

        Subject clientSubject = loginContext.getSubject();
        Set<Principal> clientPrincipals = clientSubject.getPrincipals();
        assertFalse(clientPrincipals.isEmpty());

        // Get the TGT
        Set<KerberosTicket> privateCredentials = 
            clientSubject.getPrivateCredentials(KerberosTicket.class);
        assertFalse(privateCredentials.isEmpty());
        KerberosTicket tgt = privateCredentials.iterator().next();
        assertNotNull(tgt);

        // Get the service ticket
        KerberosClientExceptionAction action =
            new KerberosClientExceptionAction(clientPrincipals.iterator().next(), "bob@service.ws.apache.org");
        byte[] ticket = (byte[]) Subject.doAs(clientSubject, action);
        assertNotNull(ticket);

        loginContext.logout();

        validateServiceTicket(ticket);
    }

    private void validateServiceTicket(byte[] ticket) throws Exception {
        // Get the TGT for the service
        LoginContext loginContext = new LoginContext("bob", new KerberosCallbackHandler());
        loginContext.login();

        Subject serviceSubject = loginContext.getSubject();
        Set<Principal> servicePrincipals = serviceSubject.getPrincipals();
        assertFalse(servicePrincipals.isEmpty());

        // Handle the service ticket
        KerberosServiceExceptionAction serviceAction = 
            new KerberosServiceExceptionAction(ticket, "bob@service.ws.apache.org");

        Subject.doAs(serviceSubject, serviceAction);
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

    /**
     * This class represents a PrivilegedExceptionAction implementation to obtain a service ticket from a Kerberos
     * Key Distribution Center.
     */
    private static class KerberosClientExceptionAction implements PrivilegedExceptionAction<byte[]> {

        private static final String JGSS_KERBEROS_TICKET_OID = "1.2.840.113554.1.2.2";

        private Principal clientPrincipal;
        private String serviceName;

        public KerberosClientExceptionAction(Principal clientPrincipal, String serviceName) { 
            this.clientPrincipal = clientPrincipal;
            this.serviceName = serviceName;
        }

        public byte[] run() throws GSSException {
            GSSManager gssManager = GSSManager.getInstance();

            GSSName gssService = gssManager.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);
            Oid oid = new Oid(JGSS_KERBEROS_TICKET_OID);
            GSSName gssClient = gssManager.createName(clientPrincipal.getName(), GSSName.NT_USER_NAME);
            GSSCredential credentials = 
                gssManager.createCredential(
                                            gssClient, GSSCredential.DEFAULT_LIFETIME, oid, GSSCredential.INITIATE_ONLY
                    );

            GSSContext secContext =
                gssManager.createContext(
                                         gssService, oid, credentials, GSSContext.DEFAULT_LIFETIME
                    );

            secContext.requestMutualAuth(false);
            secContext.requestCredDeleg(false);

            byte[] token = new byte[0];
            byte[] returnedToken = secContext.initSecContext(token, 0, token.length);

            secContext.dispose();

            return returnedToken;
        }
    }

    private static class KerberosServiceExceptionAction implements PrivilegedExceptionAction<byte[]> {

        private static final String JGSS_KERBEROS_TICKET_OID = "1.2.840.113554.1.2.2";

        private byte[] ticket;
        private String serviceName;

        public KerberosServiceExceptionAction(byte[] ticket, String serviceName) {
            this.ticket = ticket;
            this.serviceName = serviceName;
        }

        public byte[] run() throws GSSException {

            GSSManager gssManager = GSSManager.getInstance();

            GSSContext secContext = null;
            GSSName gssService = gssManager.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);

            Oid oid = new Oid(JGSS_KERBEROS_TICKET_OID);
            GSSCredential credentials = 
                gssManager.createCredential(
                                            gssService, GSSCredential.DEFAULT_LIFETIME, oid, GSSCredential.ACCEPT_ONLY
                    );
            secContext = gssManager.createContext(credentials);

            try {
                return secContext.acceptSecContext(ticket, 0, ticket.length);
            } finally {
                if (null != secContext) {
                    secContext.dispose();    
                }
            }               
        }

    }
}
