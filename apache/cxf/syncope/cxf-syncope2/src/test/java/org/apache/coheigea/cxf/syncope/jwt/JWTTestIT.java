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
package org.apache.coheigea.cxf.syncope.jwt;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.syncope.common.SyncopeDeployer;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * This tests invoking on the Syncope REST API using a third-party JWT token obtained from the REST interface of the
 * CXF STS.
 */
public class JWTTestIT extends AbstractBusClientServerTestBase {

    private static final String STS_PORT = allocatePort(STSServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
        WSSConfig.init();

        SyncopeDeployer deployer = new SyncopeDeployer();
        String syncopePort = System.getProperty("syncope.port");
        assertNotNull(syncopePort);
        deployer.setAddress("http://localhost:" + syncopePort + "/syncope/rest/");
        deployer.deployUserData();
    }


    @org.junit.Test
    public void testAuthenticatedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JWTTestIT.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        // 1. Get a JWT Token from the STS via the REST interface for "alice"
        String address = "https://localhost:" + STS_PORT + "/SecurityTokenService/token";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());

        client.accept("text/plain");
        client.path("jwt");
        //sclient.query("appliesTo", "bob/service.ws.apache.org@service.ws.apache.org");

        Response response = client.get();
        String jwtToken = response.readEntity(String.class);
        assertNotNull(jwtToken);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(jwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));

        // 2. Now use the JWT Token to authenticate to Syncope.
        String syncopePort = System.getProperty("syncope.port");
        SyncopeClientFactoryBean clientFactory =
            new SyncopeClientFactoryBean().setAddress("http://localhost:" + syncopePort + "/syncope/rest/");
        SyncopeClient syncopeClient = clientFactory.create(jwtToken);

        syncopeClient.self();
    }

    @org.junit.Test
    @org.junit.Ignore
    public void testThirdParty() throws Exception {
        System.out.println("STS PORT: " + STS_PORT);
        System.out.println("Sleeping...");
        Thread.sleep(2 * 60 * 1000);
    }

}
