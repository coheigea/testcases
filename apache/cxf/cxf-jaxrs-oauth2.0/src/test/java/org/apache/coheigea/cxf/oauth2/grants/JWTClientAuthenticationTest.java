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
package org.apache.coheigea.cxf.oauth2.grants;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oauth2.oauthservice.OAuthServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Test the authorization code grant, where the client authenticates to the access token service using a JWT Token.
 */
public class JWTClientAuthenticationTest extends AbstractBusClientServerTestBase {
    
    static final String PORT = allocatePort(OAuthServer.class);
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(OAuthServer.class, true)
        );
    }
    
    @org.junit.Test
    public void testJWT() throws Exception {
        URL busFile = JWTClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("consumer-id");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 60);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "cspass");
        signingProperties.put("rs.security.keystore.alias", "myclientkey");
        signingProperties.put("rs.security.keystore.file", "clientstore.jks");
        signingProperties.put("rs.security.key.password", "ckpass");
        signingProperties.put("rs.security.signature.algorithm", "RS256");
        
        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        
        JwsSignatureProvider sigProvider = 
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);
        
        String token = jws.signWith(sigProvider);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code, token);
        assertNotNull(accessToken.getTokenKey());
    }
    
    @org.junit.Test
    public void testJWTBadSubjectName() throws Exception {
        URL busFile = JWTClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("consumer2-id");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 60);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "cspass");
        signingProperties.put("rs.security.keystore.alias", "myclientkey");
        signingProperties.put("rs.security.keystore.file", "clientstore.jks");
        signingProperties.put("rs.security.key.password", "ckpass");
        signingProperties.put("rs.security.signature.algorithm", "RS256");
        
        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        
        JwsSignatureProvider sigProvider = 
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);
        
        String token = jws.signWith(sigProvider);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, token);
            fail("Failure expected on a bad subject name");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTUnsigned() throws Exception {
        URL busFile = JWTClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("consumer-id");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 60);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "cspass");
        signingProperties.put("rs.security.keystore.alias", "myclientkey");
        signingProperties.put("rs.security.keystore.file", "clientstore.jks");
        signingProperties.put("rs.security.key.password", "ckpass");
        signingProperties.put("rs.security.signature.algorithm", "RS256");
        
        JwsHeaders jwsHeaders = new JwsHeaders(SignatureAlgorithm.NONE);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        String token = jws.getSignedEncodedJws();
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, token);
            fail("Failure expected on an unsigned token");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTNoIssuer() throws Exception {
        URL busFile = JWTClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("consumer-id");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 60);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "cspass");
        signingProperties.put("rs.security.keystore.alias", "myclientkey");
        signingProperties.put("rs.security.keystore.file", "clientstore.jks");
        signingProperties.put("rs.security.key.password", "ckpass");
        signingProperties.put("rs.security.signature.algorithm", "RS256");
        
        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        
        JwsSignatureProvider sigProvider = 
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);
        
        String token = jws.signWith(sigProvider);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, token);
            fail("Failure expected on no issuer");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testJWTNoExpiry() throws Exception {
        URL busFile = JWTClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setSubject("consumer-id");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        
        // Sign the JWT Token
        Properties signingProperties = new Properties();
        signingProperties.put("rs.security.keystore.type", "jks");
        signingProperties.put("rs.security.keystore.password", "cspass");
        signingProperties.put("rs.security.keystore.alias", "myclientkey");
        signingProperties.put("rs.security.keystore.file", "clientstore.jks");
        signingProperties.put("rs.security.key.password", "ckpass");
        signingProperties.put("rs.security.signature.algorithm", "RS256");
        
        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        
        JwsSignatureProvider sigProvider = 
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);
        
        String token = jws.signWith(sigProvider);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, token);
            fail("Failure expected on no expiry");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private String getAuthorizationCode(WebClient client) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", "consumer-id");
        client.query("redirect_uri", "http://www.b***REMOVED***.apache.org");
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
    
    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code, String token) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"); 
        form.param("client_assertion", token);
        Response response = client.post(form);
        
        return response.readEntity(ClientAccessToken.class);
    }
    
}
