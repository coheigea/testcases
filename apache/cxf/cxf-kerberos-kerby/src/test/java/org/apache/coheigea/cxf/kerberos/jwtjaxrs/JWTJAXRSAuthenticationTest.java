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
package org.apache.coheigea.cxf.kerberos.jwtjaxrs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.ietf.jgss.GSSName;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * This is a test-case that shows how to use Kerberos and JWT tokens with a JAX-RS service.
 *
 * The JAX-RS client first obtains a JWT token from the CXF STS via the REST API and uses this token to get a Kerberos ticket
 * from the KDC to invoke on the service. The service authenticates the Kerberos ticket and uses the embedded JWT token
 * to ensure that only users with role "boss" can access the "doubleIt" operation ("alice" has this role, "dave" does not)
 */
public class JWTJAXRSAuthenticationTest extends org.junit.Assert {

    private static final String PORT = TestUtil.getPortNumber(Server.class);
    static final String STS_PORT = TestUtil.getPortNumber(STSRESTServer.class);

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setAllowUdp(true);

        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));
        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String dave = "dave@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(dave, "dave");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.start();

        updatePort(basedir);

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");

        Assert.assertTrue(
                          "Server failed to launch",
                          // run the server in the same process
                          // set this to false to fork
                          AbstractBusClientServerTestBase.launchServer(Server.class, true)
        );
        Assert.assertTrue(
                          "Server failed to launch",
                          // run the server in the same process
                          // set this to false to fork
                          AbstractBusClientServerTestBase.launchServer(STSRESTServer.class, true)
        );
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
        content = content.replaceAll("port", "" + kerbyServer.getKdcPort());

        File f2 = new File(basedir + "/target/test-classes/kerberos/krb5.conf");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        System.setProperty("java.security.krb5.conf", f2.getPath());
    }

    // TODO
    @org.junit.Test
    @org.junit.Ignore
    public void testJWTKerberos() throws Exception {

        URL busFile = JWTJAXRSAuthenticationTest.class.getResource("cxf-client.xml");

        // 1. Get a JWT Token from the STS via the REST interface
        String jwtToken = getJWTTokenFromSTS(busFile);

        String address = "https://localhost:" + PORT + "/doubleit/services";
        WebClient client = WebClient.create(address, busFile.toString());

        Map<String, Object> requestContext = WebClient.getConfig(client).getRequestContext();
        requestContext.put("auth.spnego.useKerberosOid", "true");

        SpnegoAuthSupplier authSupplier = new SpnegoAuthSupplier();
        authSupplier.setServicePrincipalName("bob@service.ws.apache.org");
        authSupplier.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);
        WebClient.getConfig(client).getHttpConduit().setAuthSupplier(authSupplier);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    private String getJWTTokenFromSTS(URL busFile) {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        String address = "https://localhost:" + STS_PORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, busFile.toString());

        client.accept("text/plain");
        client.path("jwt");

        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        return token;
    }

}
