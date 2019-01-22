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
package org.apache.coheigea.cxf.jaxrs.sts.authorization;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.sts.common.Number;
import org.apache.coheigea.cxf.jaxrs.sts.common.STSServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This tests using the CXF STS for authorization. The JAX-RS client sends a HTTP/BA request
 * to the service, which sends it to the STS for validation (along with its own UsernameToken
 * for authentication to the STS). The service also requests the roles for the client.
 *
 * The CXF Endpoint has configured the SimpleAuthorizingInterceptor, which requires that a user must
 * have role "boss" to access the "doubleIt" operation ("alice" has this role, "bob" does not).
 */
public class AuthorizationTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    static final String STS_PORT = allocatePort(STSServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(STSServer.class, true)
        );
    }

    @org.junit.Test
    public void testAuthorizedRequest() throws Exception {

        URL busFile = AuthorizationTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + PORT + "/doubleit/services";
        WebClient client =
            WebClient.create(address, "alice", "security", busFile.toString());
        client = client.type("application/xml");

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testUnauthorizedRequest() throws Exception {

        URL busFile = AuthorizationTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + PORT + "/doubleit/services";
        WebClient client =
            WebClient.create(address, "bob", "security", busFile.toString());
        client = client.type("application/xml");

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 500);
    }

}