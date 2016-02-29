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
package org.apache.coheigea.cxf.oidc.flows;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oidc.provider.OIDCProviderServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Test the OIDC Keys Service + test verifying the signature of an IdToken by retrieving a key from the KeyService
 */
public class KeysTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = allocatePort(OIDCProviderServer.class);
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(OIDCProviderServer.class, true)
        );
    }
    
 
    @org.junit.Test
    public void testGetKeys() throws Exception {
        URL busFile = KeysTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JsonWebKeysProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        client.accept("application/json");
        
        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);
        
        assertEquals(1, jsonWebKeys.getKeys().size());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithKey() throws Exception {
        URL busFile = KeysTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client, "openid");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address, providers, "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        
        // Now get the key to validate the token
        List<Object> jsonKeyProviders = new ArrayList<Object>();
        jsonKeyProviders.add(new JsonWebKeysProvider());
        
        client = WebClient.create(address, jsonKeyProviders, "alice", "security", busFile.toString());
        client.accept("application/json");
        
        client.path("keys/");
        Response response = client.get();
        JsonWebKeys jsonWebKeys = response.readEntity(JsonWebKeys.class);
        
        Assert.assertTrue(jwtConsumer.verifySignatureWith(jsonWebKeys.getKeys().get(0),
                                                          SignatureAlgorithm.RS256));
    }
    
    private String getAuthorizationCode(WebClient client, String scope) {
        return getAuthorizationCode(client, scope, null, null, "consumer-id");
    }
    
    private String getAuthorizationCode(WebClient client, String scope,
                                        String nonce, String state, String consumerId) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", consumerId);
        client.query("redirect_uri", "http://www.b***REMOVED***.apache.org");
        client.query("response_type", "code");
        if (scope != null) {
            client.query("scope", scope);
        }
        if (nonce != null) {
            client.query("nonce", nonce);
        }
        if (state != null) {
            client.query("state", state);
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
        if (authzData.getNonce() != null) {
            form.param("nonce", authzData.getNonce());
        }
        if (authzData.getProposedScope() != null) {
            form.param("scope", authzData.getProposedScope());
        }
        if (authzData.getState() != null) {
            form.param("state", authzData.getState());
        }
        form.param("oauthDecision", "allow");
        
        response = client.post(form);
        String location = response.getHeaderString("Location"); 
        if (state != null) {
            Assert.assertTrue(location.contains("state=" + state));
        }
        
        return getSubstring(location, "code");
    }
    
    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code) {
        return getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null);
    }
    
    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, 
                                                                  String code,
                                                                  String consumerId,
                                                                  String audience) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", consumerId);
        if (audience != null) {
            form.param("audience", audience);
        }
        Response response = client.post(form);
        
        return response.readEntity(ClientAccessToken.class);
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
}