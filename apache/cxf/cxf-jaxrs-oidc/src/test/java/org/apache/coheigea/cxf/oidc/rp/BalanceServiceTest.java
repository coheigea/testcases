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
package org.apache.coheigea.cxf.oidc.rp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the Balance Service. The BalanceService has a method to check the balance of a given user using GET. It is secured with OIDC - the client
 * is redirected to the IdP for authentication, and then back to the service, where the username is checked.
 */
public class BalanceServiceTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(BankServer.class);
    static final String OAUTH_PORT = allocatePort(OIDCProviderServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(BankServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(OIDCProviderServer.class, true)
           );
    }

    @org.junit.Test
    public void testAccessBalanceService() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Make an invocation on the bank + get back the redirection to the OIDC IdP
        String address = "https://localhost:" + PORT + "/bankservice/balance";
        WebClient client = WebClient.create(address, setupProviders(), busFile.toString());
        client.type("text/plain").accept("text/plain").accept("application/json");

        WebClient.getConfig(client).getRequestContext().put(
                org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        client.path("/bob");
        Response response = client.get();

        String location = response.getHeaderString("Location");
        // Now make an invocation on the OIDC IdP using another WebClient instance

        WebClient idpClient = WebClient.create(location, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(idpClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code + State
        String authzCodeLocation = makeAuthorizationCodeInvocation(idpClient);
        String state = getSubstring(authzCodeLocation, "state");
        assertNotNull(state);
        String code = getSubstring(authzCodeLocation, "code");
        assertNotNull(code);

        // Add Referer
        String referer = "https://localhost:" + OAUTH_PORT + "/services/authorize";
        client.header("Referer", referer);

        // Now invoke back on the service using the authorization code
        client.query("code", code);
        client.query("state", state);

        Response serviceResponse = client.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 1000);
    }

    private String makeAuthorizationCodeInvocation(WebClient client) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        Response response = client.get();

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);

        // Now call "decision" to get the authorization code grant
        client.path("decision");
        client.type("application/x-www-form-urlencoded");

        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        if (authzData.getProposedScope() != null) {
            form.param("scope", authzData.getProposedScope());
        }
        form.param("state", authzData.getState());
        form.param("oauthDecision", "allow");

        response = client.post(form);
        return response.getHeaderString("Location");
    }

    private String getSubstring(String parentString, String substringName) {
        String foundString =
            parentString.substring(parentString.indexOf(substringName + "=") + (substringName + "=").length());
        int ampersandIndex = foundString.indexOf('&');
        if (ampersandIndex < 1) {
            ampersandIndex = foundString.length();
        }
        return foundString.substring(0, ampersandIndex);
    }

    private static List<Object> setupProviders() {
        List<Object> providers = new ArrayList<Object>();
        JSONProvider<OAuthAuthorizationData> jsonP = new JSONProvider<OAuthAuthorizationData>();
        jsonP.setNamespaceMap(Collections.singletonMap("http://org.apache.cxf.rs.security.oauth",
                                                       "ns2"));
        providers.add(jsonP);
        providers.add(new OAuthJSONProvider());

        return providers;
    }
}