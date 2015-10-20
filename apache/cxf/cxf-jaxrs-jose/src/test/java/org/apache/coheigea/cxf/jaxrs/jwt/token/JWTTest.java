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
package org.apache.coheigea.cxf.jaxrs.jwt.token;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.json.common.Number;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jaxrs.JwtAuthenticationClientFilter;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Test JAX-RS JWT unsigned tokens, including various properties.
 */
public class JWTTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String PORT2 = allocatePort(Server.class, 2);
    private static final String PORT3 = allocatePort(Server.class, 3);
    private static final String PORT4 = allocatePort(Server.class, 4);

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
    public void testExpiredToken() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the expiry date to be yesterday
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testFutureToken() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the issued date to be in the future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        claims.setIssuedAt(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testNearFutureTokenFailure() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the issued date to be in the near future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 30);
        claims.setIssuedAt(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testNearFutureTokenSuccess() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the issued date to be in the near future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 30);
        claims.setIssuedAt(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testNotBeforeFailure() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the issued date to be in the near future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 30);
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setNotBefore(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testNotBeforeSuccess() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        
        // Set the issued date to be in the near future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 30);
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setNotBefore(cal.getTimeInMillis() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.RS256);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testUnsignedTokenSuccess() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT3 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.NONE);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testUnsignedTokenFailure() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.NONE);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testUnsignedTokenSuccessButNoSecurityContext() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT4 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        
        JwsHeaders headers = new JwsHeaders();
        headers.setType(JoseType.JWT);
        headers.setSignatureAlgorithm(SignatureAlgorithm.NONE);
        
        JwtToken token = new JwtToken(headers, claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testSetClaimsDirectly() throws Exception {

        URL busFile = JWTTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_CLAIMS, claims);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testWrongSignatureAlgorithm() throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());  
            URL busFile = JWTTest.class.getResource("cxf-client.xml");
    
            List<Object> providers = new ArrayList<Object>();
            providers.add(new JacksonJsonProvider());
            providers.add(new JwtAuthenticationClientFilter());
            
            String address = "http://localhost:" + PORT + "/doubleit/services";
            WebClient client = 
                WebClient.create(address, providers, busFile.toString());
            client.type("application/json").accept("application/json");
            
            // Create the JWT Token
            JwtClaims claims = new JwtClaims();
            claims.setSubject("alice");
            claims.setIssuer("DoubleItSTSIssuer");
            claims.setIssuedAt(new Date().getTime() / 1000L);
            
            JwsHeaders headers = new JwsHeaders();
            headers.setType(JoseType.JWT);
            headers.setSignatureAlgorithm(SignatureAlgorithm.PS256);
            
            JwtToken token = new JwtToken(headers, claims);
    
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("rs.security.keystore.type", "jks");
            properties.put("rs.security.keystore.password", "cspass");
            properties.put("rs.security.keystore.alias", "myclientkey");
            properties.put("rs.security.keystore.file", "clientstore.jks");
            properties.put("rs.security.key.password", "ckpass");
            properties.put("rs.security.signature.algorithm", "PS256");
            properties.put(JwtConstants.JWT_TOKEN, token);
            
            WebClient.getConfig(client).getRequestContext().putAll(properties);
    
            Number numberToDouble = new Number();
            numberToDouble.setDescription("This is the number to double");
            numberToDouble.setNumber(25);
    
            Response response = client.post(numberToDouble);
            assertNotEquals(response.getStatus(), 200);
        } finally {
            Security.removeProvider(BouncyCastleProvider.class.getName());  
        }
    }

}
