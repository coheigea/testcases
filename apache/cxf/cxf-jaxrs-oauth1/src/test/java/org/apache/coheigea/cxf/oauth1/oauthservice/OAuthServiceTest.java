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
package org.apache.coheigea.cxf.oauth1.oauthservice;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the OAuth service
 */
public class OAuthServiceTest extends AbstractBusClientServerTestBase {
    
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
    public void testRequestTokenService() throws Exception {
        URL busFile = OAuthServiceTest.class.getResource("cxf-client.xml");

        Response response = makeRequestTokenInvocation(busFile);
        assertEquals(response.getStatus(), 200);
        
        String responseString = response.readEntity(String.class);
        String requestToken = getSubstring(responseString, "oauth_token");
        assertNotNull(requestToken);
        
        String requestTokenSecret = getSubstring(responseString, "oauth_token_secret");
        assertNotNull(requestTokenSecret);
    }
    
    @org.junit.Test
    public void testAuthorizationService() throws Exception {
        URL busFile = OAuthServiceTest.class.getResource("cxf-client.xml");

        Response response = makeRequestTokenInvocation(busFile);
        assertEquals(response.getStatus(), 200);
        
        // Extract RequestToken + Secret
        String responseString = response.readEntity(String.class);
        String requestToken = getSubstring(responseString, "oauth_token");
        
        requestToken = requestToken.substring(requestToken.indexOf("=") + 1);
        assertNotNull(requestToken);
        
        String requestTokenSecret = getSubstring(responseString, "oauth_token_secret");
        assertNotNull(requestTokenSecret);
        
        makeAuthorizationInvocation(busFile, requestToken, "alice");
    }
    
    @org.junit.Test
    public void testAccessTokenService() throws Exception {
        URL busFile = OAuthServiceTest.class.getResource("cxf-client.xml");

        Response response = makeRequestTokenInvocation(busFile);
        assertEquals(response.getStatus(), 200);
        
        // Extract RequestToken + Secret
        String responseString = response.readEntity(String.class);
        String requestToken = getSubstring(responseString, "oauth_token");
        assertNotNull(requestToken);
        
        String requestTokenSecret = getSubstring(responseString, "oauth_token_secret");
        assertNotNull(requestTokenSecret);
        
        Response authorizationResponse = makeAuthorizationInvocation(busFile, requestToken, "alice");
        
        // Extract verifier
        String location = authorizationResponse.getHeaderString("Location");
        String oauthVerifier = getSubstring(location, "oauth_verifier");
        
        Response accessTokenResponse = 
            makeAccessTokenInvocation(busFile, requestToken, requestTokenSecret, oauthVerifier);
        
        // Extract AccessToken + Secret
        responseString = accessTokenResponse.readEntity(String.class);
        String accessToken = getSubstring(responseString, "oauth_token");
        assertNotNull(accessToken);
        
        String accessTokenSecret = getSubstring(responseString, "oauth_token_secret");
        assertNotNull(accessTokenSecret);
    }
    
    private Response makeRequestTokenInvocation(URL busFile) {
        String address = "https://localhost:" + PORT + "/oauth/initiate";
        WebClient client = WebClient.create(address, busFile.toString());
        
        String nonce = UUID.randomUUID().toString();
        String oAuthHeader = "OAuth "
            + "oauth_consumer_key=\"consumer-id\", "
            + "oauth_signature_method=\"PLAINTEXT\", "
            + "oauth_nonce=\"" + nonce + "\", "
            + "oauth_callback=\"https://localhost:12345/callback\", "
            + "oauth_timestamp=\"" + new Date().getTime() / 1000L + "\", "
            + "oauth_signature=\"" + "this-is-a-secret&\", "
            + "scope=\"get_balance\"";
        client.header("Authorization", oAuthHeader);
        return client.post(null);
    }
    
    private Response makeAuthorizationInvocation(URL busFile, String requestToken, String user) {
        // Make initial invocation to authorization service
        String address = "https://localhost:" + PORT + "/authorization/authorize";
        WebClient client = WebClient.create(address, user, "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        client.accept("application/xml");
        
        client.query("oauth_token", requestToken);
        XMLSource authorizationResponse = client.get(XMLSource.class);
        authorizationResponse.setBuffering();
        
        String authenticityToken = 
            authorizationResponse.getNode("//authenticityToken/text()", String.class);
        assertNotNull(authenticityToken);
        
        String replyTo = 
            authorizationResponse.getNode("//replyTo/text()", String.class);
        assertNotNull(replyTo);
        
        // Now send back the decision
        client.to(replyTo, false);
        client.type("application/x-www-form-urlencoded");
        
        Form form = new Form();
        form.param("session_authenticity_token", authenticityToken);
        form.param("oauthDecision", "allow");
        form.param("oauth_token", requestToken);
        return client.post(form);
    }
    
    private Response makeAccessTokenInvocation(URL busFile, String requestToken, 
                                               String requestTokenSecret, String verifier) {
        String address = "https://localhost:" + PORT + "/oauth/token";
        WebClient client = WebClient.create(address, busFile.toString());
        
        String signature = 
            net.oauth.OAuth.percentEncode("this-is-a-secret") + "&" + requestTokenSecret;
        
        String nonce = UUID.randomUUID().toString();
        String oAuthHeader = "OAuth "
            + "oauth_consumer_key=\"consumer-id\", "
            + "oauth_token=\"" + requestToken + "\", "
            + "oauth_signature_method=\"PLAINTEXT\", "
            + "oauth_signature=\"" + net.oauth.OAuth.percentEncode(signature) + "\", "
            + "oauth_timestamp=\"" + new Date().getTime() / 1000L + "\", "
            + "oauth_nonce=\"" + nonce + "\", "
            + "oauth_verifier=\"" + verifier + "\"";
        client.header("Authorization", oAuthHeader);
        return client.post(null);
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