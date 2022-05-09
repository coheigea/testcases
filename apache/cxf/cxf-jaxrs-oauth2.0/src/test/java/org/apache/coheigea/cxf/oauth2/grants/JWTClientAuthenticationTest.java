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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oauth2.balanceservice.BankServer;
import org.apache.coheigea.cxf.oauth2.balanceservice.PartnerServer;
import org.apache.coheigea.cxf.oauth2.oauthservice.OAuthServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the authorization code grant, where the client authenticates to the access token service using a JWT Token.
 */
public class JWTClientAuthenticationTest extends AbstractBusClientServerTestBase {

    static final String PORT = allocatePort(OAuthServer.class);
    static final String PARTNER_PORT = allocatePort(PartnerServer.class);
    static final String BANK_PORT = allocatePort(BankServer.class);

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

        String address = "https://localhost:" + PORT + "/services/";
        WebClient client = WebClient.create(address, setupProviders(), "alice", "security", busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Get Authorization Code
        String code = getAuthorizationCode(client);
        assertNotNull(code);

        // Now get the access token
        String jwtAddress = "https://localhost:" + PORT + "/jwtservices/";
        client = WebClient.create(jwtAddress, setupProviders(), busFile.toString());
        // Save the Cookie for the second request...
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);

        // Create the JWT Token
        String token = createToken("DoubleItSTSIssuer", "consumer-id",
                                   jwtAddress + "token", true, true);

        ClientAccessToken accessToken = getAccessTokenWithAuthorizationCode(client, code, token);
        assertNotNull(accessToken.getTokenKey());
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
        return getSubstring(location, "code");
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

    private ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code, String token) {
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.param("client_assertion", token);
        form.param("redirect_uri", "http://www.blah.apache.org");
        Response response = client.post(form);

        return response.readEntity(ClientAccessToken.class);
    }

    private String createToken(String issuer, String subject, String audience,
                               boolean expiry, boolean sign) {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        claims.setIssuedAt(new Date().getTime() / 1000L);
        if (expiry) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 60);
            claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        }
        if (audience != null) {
            claims.setAudiences(Collections.singletonList(audience));
        }

        if (sign) {
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

            return jws.signWith(sigProvider);
        }

        JwsHeaders jwsHeaders = new JwsHeaders(SignatureAlgorithm.NONE);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        return jws.getSignedEncodedJws();
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