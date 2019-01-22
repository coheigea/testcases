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
import java.net.URL;
import java.security.Provider;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.gss.KerbyGssProvider;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.ietf.jgss.GSSName;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This is a test-case that shows how to use Kerberos and JWT tokens with a JAX-RS service.
 *
 * The JAX-RS client first obtains a JWT token from the CXF STS via the REST API and uses this token to get a Kerberos ticket
 * from the KDC to invoke on the service. The service authenticates the Kerberos ticket and uses the embedded JWT token
 * to ensure that only users with role "boss" can access the "doubleIt" operation ("alice" has this role, "dave" does not)
 */
public class JWTJAXRSAuthenticationTest extends org.junit.Assert {

    private static final String PORT = TestUtil.getPortNumber(Server.class);
    private static final String ROLE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    static final String STS_PORT = TestUtil.getPortNumber(STSRESTServer.class);

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        Provider provider = new KerbyGssProvider();
        java.security.Security.insertProviderAt(provider, 1);

        WSSConfig.init();

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setAllowUdp(true);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));
        kerbyServer.getKdcConfig().setString(KdcConfigKey.TOKEN_ISSUERS, "DoubleItSTSIssuer");
        kerbyServer.getKdcConfig().setString(KdcConfigKey.TOKEN_VERIFY_KEYS, "mysts.cer");
        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String dave = "dave@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(dave, "dave");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.start();

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");
        System.setProperty("java.security.krb5.conf", basedir + "/target/krb5.conf");

        assertTrue(
                          "Server failed to launch",
                          // run the server in the same process
                          // set this to false to fork
                          AbstractBusClientServerTestBase.launchServer(Server.class, true)
        );
        assertTrue(
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

    @org.junit.Test
    public void testJWTKerberosAccessToken() throws Exception {

        URL busFile = JWTJAXRSAuthenticationTest.class.getResource("cxf-client.xml");

        // 1. Get a JWT Token from the STS via the REST interface for "alice"
        String jwtToken = getJWTTokenFromSTS(busFile);
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(jwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertTrue(((List<?>)jwt.getClaim(ROLE)).contains("boss"));

        // 2. Now call on the service using a custom HttpAuthSupplier
        String address = "https://localhost:" + PORT + "/doubleit/services";
        WebClient client = WebClient.create(address, busFile.toString()).type("application/xml");

        Map<String, Object> requestContext = WebClient.getConfig(client).getRequestContext();
        requestContext.put("auth.spnego.useKerberosOid", "true");

        KerbyHttpAuthSupplier authSupplier = new KerbyHttpAuthSupplier();
        authSupplier.setServicePrincipalName("bob/service.ws.apache.org@service.ws.apache.org");
        authSupplier.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);
        authSupplier.setJwtToken(jwtToken);
        WebClient.getConfig(client).getHttpConduit().setAuthSupplier(authSupplier);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testJWTKerberosAccessTokenFailingAuthz() throws Exception {

        URL busFile = JWTJAXRSAuthenticationTest.class.getResource("cxf-client-dave.xml");

        // 1. Get a JWT Token from the STS via the REST interface for "alice"
        String jwtToken = getJWTTokenFromSTS(busFile);
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(jwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals("dave", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        assertTrue((jwt.getClaim(ROLE)).equals("employee"));

        // 2. Now call on the service using a custom HttpAuthSupplier
        String address = "https://localhost:" + PORT + "/doubleit/services";
        WebClient client = WebClient.create(address, busFile.toString()).type("application/xml");

        Map<String, Object> requestContext = WebClient.getConfig(client).getRequestContext();
        requestContext.put("auth.spnego.useKerberosOid", "true");

        KerbyHttpAuthSupplier authSupplier = new KerbyHttpAuthSupplier();
        authSupplier.setServicePrincipalName("bob/service.ws.apache.org@service.ws.apache.org");
        authSupplier.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);
        authSupplier.setJwtToken(jwtToken);
        WebClient.getConfig(client).getHttpConduit().setAuthSupplier(authSupplier);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 500);
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
        client.query("appliesTo", "bob/service.ws.apache.org@service.ws.apache.org");

        client.query("claim", ROLE);

        Response response = client.get();
        String token = response.readEntity(String.class);
        assertNotNull(token);
        return token;
    }

}