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
package org.apache.coheigea.cxf.oauth2.balanceservice;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oauth2.oauthservice.OAuthServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the Balance Service. The BalanceService has two different implementations, a "customer" balance service where a user can create
 * a balance (if authenticated using Basic Authentication), and a "partner" balance service protected by OAuth, where a partner can check
 * the balance of a given user.
 */
public class BalanceServiceTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(BankServer.class);
    public static final String PARTNER_PORT = allocatePort(PartnerServer.class);
    static final String OAUTH_PORT = allocatePort(OAuthServer.class);

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
                   launchServer(PartnerServer.class, true)
           );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(OAuthServer.class, true)
           );
    }

    @org.junit.Test
    public void testPartnerServiceWithToken() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Create an initial account at the bank
        String address = "https://localhost:" + PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");

        client.path("/bob");
        client.post(40);

        // Get Authorization Code (as "bob")
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = getAuthorizationCode(oauthClient);
        assertNotNull(code);

        // Now get the access token
        oauthClient = WebClient.create(oauthService, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(oauthClient, code, "http://www.b***REMOVED***.apache.org");
        assertNotNull(accessToken.getTokenKey());

        // Now invoke on the service with the access token
        String partnerAddress = "https://localhost:" + PORT + "/bankservice/partners/balance";
        WebClient partnerClient = WebClient.create(partnerAddress, busFile.toString());
        partnerClient.type("text/plain").accept("text/plain");
        partnerClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        partnerClient.path("/bob");
        // Now make a service invocation with the access token
        Response serviceResponse = partnerClient.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }

    @org.junit.Test
    public void testPartnerServiceWithTokenAndScope() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Create an initial account at the bank
        String address = "https://localhost:" + PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");

        client.path("/bob");
        client.post(40);

        // Get Authorization Code (as "bob")
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = getAuthorizationCode(oauthClient, "read_balance");
        assertNotNull(code);

        // Now get the access token
        oauthClient = WebClient.create(oauthService, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(oauthClient, code, "http://www.b***REMOVED***.apache.org");
        assertNotNull(accessToken.getTokenKey());

        // Now invoke on the service with the access token
        String partnerAddress = "https://localhost:" + PORT + "/bankservice/partners/balance";
        WebClient partnerClient = WebClient.create(partnerAddress, busFile.toString());
        partnerClient.type("text/plain").accept("text/plain");
        partnerClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        partnerClient.path("/bob");
        // Now make a service invocation with the access token
        Response serviceResponse = partnerClient.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }

    @org.junit.Test
    public void testPartnerServiceWithMultipleScopes() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Create an initial account at the bank
        String address = "https://localhost:" + PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");

        client.path("/bob");
        client.post(40);

        // Get Authorization Code (as "bob")
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = getAuthorizationCode(oauthClient, "create_balance read_balance read_data");
        assertNotNull(code);

        // Now get the access token
        oauthClient = WebClient.create(oauthService, setupProviders(), "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(oauthClient, code, "http://www.b***REMOVED***.apache.org");
        assertNotNull(accessToken.getTokenKey());

        // Now invoke on the service with the access token
        String partnerAddress = "https://localhost:" + PORT + "/bankservice/partners/balance";
        WebClient partnerClient = WebClient.create(partnerAddress, busFile.toString());
        partnerClient.type("text/plain").accept("text/plain");
        partnerClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        partnerClient.path("/bob");
        // Now make a service invocation with the access token
        Response serviceResponse = partnerClient.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }

    @org.junit.Test
    public void testPartnerServiceWithTokenUsingAudience() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Create an initial account at the bank
        String address = "https://localhost:" + PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");

        client.path("/bob");
        client.post(40);

        // Get Authorization Code (as "bob")
        String oauthService = "https://localhost:" + OAUTH_PORT + "/services/";

        WebClient oauthClient = WebClient.create(oauthService, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String code = getAuthorizationCode(oauthClient, null, "consumer-id-aud");
        assertNotNull(code);

        // Now get the access token
        oauthClient = WebClient.create(oauthService, setupProviders(), "consumer-id-aud", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(oauthClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        String partnerAddress = "https://localhost:" + PORT + "/bankservice/partners/balance";
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(oauthClient, code,
                                                                            "consumer-id-aud",
                                                                            partnerAddress,
                                                                            "http://www.b***REMOVED***.apache.org");
        assertNotNull(accessToken.getTokenKey());

        // Now invoke on the service with the access token
        WebClient partnerClient = WebClient.create(partnerAddress, busFile.toString());
        partnerClient.type("text/plain").accept("text/plain");
        partnerClient.header("Authorization", "Bearer " + accessToken.getTokenKey());

        partnerClient.path("/bob");
        // Now make a service invocation with the access token
        Response serviceResponse = partnerClient.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }

    @org.junit.Test
    public void testPartnerServiceUsingClientCodeRequestFilter() throws Exception {
        URL busFile = BalanceServiceTest.class.getResource("cxf-client.xml");

        // Create an initial account at the bank
        String address = "https://localhost:" + PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");

        client.path("/bob");
        client.post(40);

        // Now invoke on the partner service, which is secured with the ClientCodeRequestFilter
        String partnerService = "https://localhost:" + PARTNER_PORT + "/partnerservice/balance";

        WebClient partnerClient = WebClient.create(partnerService, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(partnerClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        partnerClient.type("text/plain").accept("text/plain").accept("application/json");

        Response response = partnerClient.get();
        // Get the "Location"
        String location = response.getHeaderString("Location");

        // Now make an invocation on the OIDC IdP using another WebClient instance

        WebClient idpClient = WebClient.create(location, setupProviders(), "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(idpClient).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code + State
        String receivedLocation = getLocationUsingAuthorizationCodeGrant(idpClient);
        assertNotNull(receivedLocation);
        String code = getSubstring(receivedLocation, "code");
        String state = getSubstring(receivedLocation, "state");

        // Add Referer
        String referer = "https://localhost:" + OAUTH_PORT + "/services/authorize";
        partnerClient.header("Referer", referer);

        // Now invoke back on the service using the authorization code
        partnerClient.query("code", code);
        partnerClient.query("state", state);

        Response serviceResponse = partnerClient.get();
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }

    private String getAuthorizationCode(WebClient client) {
        return getAuthorizationCode(client, null);
    }

    private String getAuthorizationCode(WebClient client, String scope) {
        return getAuthorizationCode(client, scope, "consumer-id");
    }

    private String getAuthorizationCode(WebClient client, String scope, String consumerId) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", consumerId);
        client.query("redirect_uri", "http://www.b***REMOVED***.apache.org");
        client.query("response_type", "code");
        if (scope != null) {
            client.query("scope", scope);
        }
        client.path("authorize/");
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
        form.param("oauthDecision", "allow");

        response = client.post(form);
        String location = response.getHeaderString("Location");
        return getSubstring(location, "code");
    }

    private String getLocationUsingAuthorizationCodeGrant(WebClient client) {
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

    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code, String redirectUri) {
        return getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null, redirectUri);
    }

    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client,
                                                                  String code,
                                                                  String consumerId,
                                                                  String audience,
                                                                  String redirectUri) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", consumerId);
        if (audience != null) {
            form.param("audience", audience);
        }
        form.param("redirect_uri", redirectUri);
        Response response = client.post(form);

        return response.readEntity(ClientAccessToken.class);
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