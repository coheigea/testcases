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
package org.apache.coheigea.cxf.jaxrs.jws;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.json.common.Number;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Test JAX-RS JSON Signature. Only the client -> service request is signed, not the response.
 */
public class JWSSignatureTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String PORT2 = allocatePort(Server.class, 2);
    private static final String PORT3 = allocatePort(Server.class, 3);
    private static final String PORT4 = allocatePort(Server.class, 4);
    private static final String PORT5 = allocatePort(Server.class, 5);
    private static final String PORT6 = allocatePort(Server.class, 6);
    private static final String PORT7 = allocatePort(Server.class, 7);
    private static final String PORT8 = allocatePort(Server.class, 8);

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
    public void testSignatureListProperties() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsJsonWriterInterceptor writer = new JwsJsonWriterInterceptor();
        writer.setUseJwsJsonOutputStream(true);
        providers.add(writer);

        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.signature.out.list.properties", "clientKeystore.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testSignatureCompact() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.signature.out.properties", "clientKeystore.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testSignatureCompactDynamicProperties() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testHMACSignatureCompact() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT3 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");


        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "HMACKey");
        properties.put("rs.security.keystore.file", "jwk.txt");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    @org.junit.Test
    public void testPSSignatureCompact() throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());  

            URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

            List<Object> providers = new ArrayList<Object>();
            providers.add(new JacksonJsonProvider());

            JwsWriterInterceptor writer = new JwsWriterInterceptor();
            providers.add(writer);

            String address = "http://localhost:" + PORT4 + "/doubleit/services";
            WebClient client = 
                WebClient.create(address, providers, busFile.toString());
            client.type("application/json").accept("application/json");

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("rs.security.keystore.type", "jks");
            properties.put("rs.security.keystore.password", "cspass");
            properties.put("rs.security.keystore.alias", "myclientkey");
            properties.put("rs.security.keystore.file", "clientstore.jks");
            properties.put("rs.security.key.password", "ckpass");
            properties.put("rs.security.signature.algorithm", "PS256");
            WebClient.getConfig(client).getRequestContext().putAll(properties);

            Number numberToDouble = new Number();
            numberToDouble.setDescription("This is the number to double");
            numberToDouble.setNumber(25);

            Response response = client.post(numberToDouble);
            assertEquals(response.getStatus(), 200);
            assertEquals(response.readEntity(Number.class).getNumber(), 50);
        } finally {
            Security.removeProvider(BouncyCastleProvider.class.getName());  
        }
    }

    // TODO Signature is not validating for some reason
    @org.junit.Test
    @org.junit.Ignore
    public void testEllipticCurveSignatureCompact() throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());  

            URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

            List<Object> providers = new ArrayList<Object>();
            providers.add(new JacksonJsonProvider());

            JwsWriterInterceptor writer = new JwsWriterInterceptor();
            providers.add(writer);

            String address = "http://localhost:" + PORT5 + "/doubleit/services";
            WebClient client = 
                WebClient.create(address, providers, busFile.toString());
            client.type("application/json").accept("application/json");


            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("rs.security.keystore.type", "jks");
            properties.put("rs.security.keystore.password", "security");
            properties.put("rs.security.keystore.alias", "ECDSA");
            properties.put("rs.security.keystore.file", "ecdsa.jks");
            properties.put("rs.security.key.password", "security");
            properties.put("rs.security.signature.algorithm", "ES256");
            WebClient.getConfig(client).getRequestContext().putAll(properties);

            Number numberToDouble = new Number();
            numberToDouble.setDescription("This is the number to double");
            numberToDouble.setNumber(25);

            Response response = client.post(numberToDouble);
            assertEquals(response.getStatus(), 200);
            assertEquals(response.readEntity(Number.class).getNumber(), 50);
        } finally {
            Security.removeProvider(BouncyCastleProvider.class.getName());  
        }
    }

    @org.junit.Test
    public void testImposterSignature() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "ispass");
        properties.put("rs.security.keystore.alias", "imposter");
        properties.put("rs.security.keystore.file", "imposter.jks");
        properties.put("rs.security.key.password", "ikpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testSigningXMLPayload() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT6 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.signature.out.properties", "clientKeystore.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    // Include the cert in the "x5c" header
    @org.junit.Test
    public void testSignatureCertificateTest() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT7 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put("rs.security.signature.include.cert", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    // Include the cert digest in the "x5t" header
    @org.junit.Test
    public void testSignatureCertificateSha1Test() throws Exception {

        URL busFile = JWSSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());

        JwsWriterInterceptor writer = new JwsWriterInterceptor();
        providers.add(writer);

        String address = "http://localhost:" + PORT8 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put("rs.security.signature.include.cert.sha1", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

}
