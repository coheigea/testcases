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

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oidc.provider.OIDCProviderServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.util.Loader;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Some unit tests to test the authorization code flow in OpenID Connect.
 */
public class AuthorizationCodeFlowTest extends AbstractBusClientServerTestBase {
    
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
    public void testAuthorizationCodeFlow() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
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
        validateIdToken(idToken, null);
    }
    
    // Just a normal OAuth invocation, check it all works ok
    @org.junit.Test
    public void testAuthorizationCodeOAuth() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client, null, false, null, null);
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address, providers, "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        // We should not have an IdToken here
        String idToken = accessToken.getParameters().get("id_token");
        assertNull(idToken);
        assertFalse(accessToken.getApprovedScope().contains("openid"));
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithNonce() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client, "openid", true, "123456789", null);
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
        validateIdToken(idToken, "123456789");
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithScope() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client, "openid read_balance");
        assertNotNull(code);
        
        // Now get the access token
        client = WebClient.create(address, providers, "consumer-id", "this-is-a-secret", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code);
        assertNotNull(accessToken.getTokenKey());
        assertTrue(accessToken.getApprovedScope().contains("openid"));
        assertTrue(accessToken.getApprovedScope().contains("read_balance"));
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithRefresh() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
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
        assertNotNull(accessToken.getRefreshToken());
        
        String idToken = accessToken.getParameters().get("id_token");
        assertNotNull(idToken);
        validateIdToken(idToken, null);
        
        // Refresh the access token
        client.type("application/x-www-form-urlencoded").accept("application/json");
        
        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("refresh_token", accessToken.getRefreshToken());
        form.param("client_id", "consumer-id");
        form.param("scope", "openid");
        Response response = client.post(form);
        
        accessToken = response.readEntity(ClientAccessToken.class);
        assertNotNull(accessToken.getTokenKey());
        assertNotNull(accessToken.getRefreshToken());
    }
    
    @org.junit.Test
    public void testAuthorizationCodeFlowWithState() throws Exception {
        URL busFile = AuthorizationCodeFlowTest.class.getResource("cxf-client.xml");
        
        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        
        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, providers, "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Get Authorization Code
        String code = getAuthorizationCode(client, "openid", true, null, "123456789");
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
        validateIdToken(idToken, null);
    }
    
    private String getAuthorizationCode(WebClient client, String scope) {
        return getAuthorizationCode(client, scope, true, null, null);
    }
    
    private String getAuthorizationCode(WebClient client, String scope, boolean openId,
                                        String nonce, String state) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
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
            if (openId || !authzData.getProposedScope().contains("openid")) {
                form.param("scope", authzData.getProposedScope());
            } else {
                String proposedScope = authzData.getProposedScope();
                proposedScope = proposedScope.replaceFirst("openid", "");
                proposedScope = proposedScope.replaceAll("  ", " ");
                form.param("scope", proposedScope);
            }
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
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", "consumer-id");
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
    
    private void validateIdToken(String idToken, String nonce) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        // Validate claims
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
        Assert.assertEquals("OIDC IdP", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
        Assert.assertEquals("consumer-id", jwt.getClaim(JwtConstants.CLAIM_AUDIENCE));
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_EXPIRY));
        Assert.assertNotNull(jwt.getClaim(JwtConstants.CLAIM_ISSUED_AT));
        if (nonce != null) {
            Assert.assertEquals(nonce, jwt.getClaim(IdToken.NONCE_CLAIM));
        }
        
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(Loader.getResource("servicestore.jks").openStream(), "sspass".toCharArray());
        Certificate cert = keystore.getCertificate("myservicekey");
        Assert.assertNotNull(cert);
        
        Assert.assertTrue(jwtConsumer.verifySignatureWith((X509Certificate)cert, 
                                                          SignatureAlgorithm.RS256));
    }
}
