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
package org.apache.coheigea.cxf.samlsso.authorization;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.samlsso.authentication.AuthenticationTest;
import org.apache.coheigea.cxf.samlsso.idp.IdpServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

/**
 * Authentication is the same as for the AuthenticationTest. This time however,
 * the IdP is configured to add an AttributeStatement to the SAML Assertion in
 * the Response, containing the roles of the authenticated user. This is used by
 * the endpoint to set up the security context.
 *
 * The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
 * reads the current Subject's roles from the SecurityContext, and requires that
 * a user must have role "boss" to access the "doubleIt" operation ("alice" has
 * this role, "bob" does not).
 */
public class AuthorizationTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = allocatePort(Server.class);
    static final String IDP_PORT = allocatePort(IdpServer.class);
    
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
                launchServer(IdpServer.class, true)
        );
    }
   
    @org.junit.Test
    @org.junit.Ignore
    public void testBrowser() throws Exception {
        // https://localhost:9001/doubleit/services/doubleit-rs/25
        // System.out.println("Sleeping...");
        // Thread.sleep(60 * 1000);
    }
    
    @org.junit.Test
    public void testAuthorizationRequest() throws Exception {

        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        WebClient.getConfig(client).getHttpConduit().getClient().setAutoRedirect(true);
        WebClient.getConfig(client).getRequestContext().put("http.redirect.max.same.uri.count", "2");
        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        client.type("text/plain").accept("text/plain");
        
        client.path("/25");
        Response response = client.get();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Integer.class).intValue(), 50);
    }
    
    @org.junit.Test
    public void testUnauthorizedRequest() throws Exception {
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        WebClient.getConfig(client).getHttpConduit().getClient().setAutoRedirect(true);
        WebClient.getConfig(client).getRequestContext().put("http.redirect.max.same.uri.count", "2");
        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        client.type("text/plain").accept("text/plain");
        
        client.path("/25");
        Response response = client.get();
        assertEquals(response.getStatus(), 500);
    }
    
}
