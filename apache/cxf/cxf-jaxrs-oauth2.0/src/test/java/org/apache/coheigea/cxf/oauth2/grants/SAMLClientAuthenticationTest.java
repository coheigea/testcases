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
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oauth2.oauthservice.OAuthServer;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Test the authorization code grant, where the client authenticates to the access token service using SAML.
 */
public class SAMLClientAuthenticationTest extends AbstractBusClientServerTestBase {
    
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
    public void testSAML() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        String assertion = createToken(samlAddress + "token", true, true);
        
        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code, assertion);
        assertNotNull(accessToken.getTokenKey());
    }
    
    
    @org.junit.Test
    public void testSAML11() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        String assertion = createToken(samlAddress + "token", false, true);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, assertion);
            fail("Failure expected on a SAML 1.1 assertion");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLAudRestr() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        String assertion = createToken(samlAddress + "token2", true, true);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, assertion);
            fail("Failure expected on a bad audience restriction");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLBadSubjectName() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setSignAssertion(true);
        samlCallbackHandler.setSubjectName("consumer2-id");
        
        ConditionsBean conditions = new ConditionsBean();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(samlAddress + "token"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        samlCallbackHandler.setConditions(conditions);
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        String assertion = samlAssertion.assertionToString();
        try {
            getAccessTokenWithAuthorizationCode(client, code, assertion);
            fail("Failure expected on a bad subject name");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLUnsigned() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        String assertion = createToken(samlAddress + "token", true, false);
        
        try {
            getAccessTokenWithAuthorizationCode(client, code, assertion);
            fail("Failure expected on an unsigned assertion");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testSAMLHolderOfKey() throws Exception {
        URL busFile = SAMLClientAuthenticationTest.class.getResource("cxf-hok-client.xml");
        
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
        String samlAddress = "https://localhost:" + PORT + "/samlservices/";
        client = WebClient.create(samlAddress, providers, busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        samlCallbackHandler.setSignAssertion(true);
        
        ConditionsBean conditions = new ConditionsBean();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(samlAddress + "token"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        samlCallbackHandler.setConditions(conditions);
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        String assertion = samlAssertion.assertionToString();
        try {
            getAccessTokenWithAuthorizationCode(client, code, assertion);
            fail("Failure expected on an incorrect subject confirmation method");
        } catch (Exception ex) {
            // expected
        }
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
    
    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code, String assertion) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");
        
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:saml2-bearer"); 
        form.param("client_assertion", Base64UrlUtility.encode(assertion));
        Response response = client.post(form);
        
        return response.readEntity(ClientAccessToken.class);
    }
    
    private String createToken(String audRestr, boolean saml2, boolean sign) throws WSSecurityException {
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(saml2);
        samlCallbackHandler.setSignAssertion(sign);
        
        ConditionsBean conditions = new ConditionsBean();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(audRestr));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        samlCallbackHandler.setConditions(conditions);
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        return samlAssertion.assertionToString();
    }
}
