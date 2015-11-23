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
package org.apache.coheigea.cxf.oauth2.unit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Some unit tests OAuth 2.0
 */
public class OAuth2UnitTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = allocatePort(Server.class);
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
    }
    
    @org.junit.Test
    public void testAuthorizationCodeGrant() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client);
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address, providers, "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeGrantRefresh() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client);
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address, providers, "bob", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
        
        // Refresh the access token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        
        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }
    
    @org.junit.Test
    public void testImplicitCodeGrant() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Access Token
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", "token");
        client.path("authorize-implicit/");
        Response response = client.get();
        
        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        
        // Now call "decision" to get the access token
        client.path("decision");
        client.type("application/x-www-form-urlencoded");
        
        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        form.param("oauthDecision", "allow");
        
        response = client.post(form);
        
        String location = response.getHeaderString("Location"); 
        String accessToken = location.substring(location.indexOf("access_token=") + "access_token=".length());
        accessToken = accessToken.substring(0, accessToken.indexOf('&'));
        assertNotNull(accessToken);
    }
    
    private String getAuthorizationCode(WebClient client) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", "code");
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
        form.param("oauthDecision", "allow");
        
        response = client.post(form);
        String location = response.getHeaderString("Location"); 
        return location.substring(location.indexOf("code=") + "code=".length());
    }
    
    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", "consumer-id");
        Response response = client.post(form);
        
        return response.readEntity(ClientAccessToken.class);
    }
    
    
    /*
    @org.junit.Test
    public void testCustomerBankService() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + BANK_SERVICE_PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/alice");
        Response response =  client.post(25);
        assertEquals(response.getStatus(), 204);
        
        response = client.get();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Integer.class).intValue(), 25);
        
        // Test that "bob" can't read Alice's account
        client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/bob");
        response = client.post(35);
        assertEquals(response.getStatus(), 204);
        
        client.reset();
        client.path("alice");
        response = client.get();
        assertEquals(response.getStatus(), 403);
    }
   
    @org.junit.Test
    public void testPartnerBankService() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + BANK_SERVICE_PORT + "/bankservice/partners/balance";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/alice");
        Response response =  client.post(25);
        assertEquals(response.getStatus(), 401);
    }
    
    @org.junit.Test
    public void testPartnerServiceWithToken() throws Exception {
        URL busFile = OAuth2UnitTest.class.getResource("cxf-client.xml");
        
        // Create an initial account at the bank
        String address = "https://localhost:" + BANK_SERVICE_PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/bob");
        client.post(40);

        Response response = makeRequestTokenInvocation(busFile);
        assertEquals(response.getStatus(), 200);
        
        // Extract RequestToken + Secret
        String responseString = response.readEntity(String.class);
        String requestToken = 
            responseString.substring(responseString.indexOf("oauth_token="),
                                     responseString.indexOf("&oauth_token_secret"));
        requestToken = requestToken.substring(requestToken.indexOf("=") + 1);
        assertNotNull(requestToken);
        
        String requestTokenSecret = 
            responseString.substring(responseString.indexOf("oauth_token_secret="));
        requestTokenSecret = requestTokenSecret.substring(requestTokenSecret.indexOf("=") + 1);
        assertNotNull(requestTokenSecret);
        
        Response authorizationResponse = makeAuthorizationInvocation(busFile, requestToken, "bob");
        
        // Extract verifier
        String location = authorizationResponse.getHeaderString("Location");
        String oauthVerifier = location.substring(location.indexOf("oauth_verifier="));
        oauthVerifier = oauthVerifier.substring(oauthVerifier.indexOf("=") + 1);
        
        Response accessTokenResponse = 
            makeAccessTokenInvocation(busFile, requestToken, requestTokenSecret, oauthVerifier);
        
        // Extract AccessToken + Secret
        responseString = accessTokenResponse.readEntity(String.class);
        String accessToken = 
            responseString.substring(responseString.indexOf("oauth_token="),
                                     responseString.indexOf("&oauth_token_secret"));
        accessToken = accessToken.substring(accessToken.indexOf("=") + 1);
        assertNotNull(accessToken);
        
        String accessTokenSecret = 
            responseString.substring(responseString.indexOf("oauth_token_secret="));
        accessTokenSecret = accessTokenSecret.substring(accessTokenSecret.indexOf("=") + 1);
        assertNotNull(accessTokenSecret);
        
        // Now make a service invocation with the access token
        Response serviceResponse = 
            makeServiceInvocation(busFile, accessToken, accessTokenSecret, "bob");
        assertEquals(serviceResponse.getStatus(), 200);
        assertEquals(serviceResponse.readEntity(Integer.class).intValue(), 40);
    }
    
    */
}
