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
package org.apache.coheigea.cxf.jaxrs.jwejws;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.json.common.DoubleItService;
import org.apache.coheigea.cxf.jaxrs.json.common.Number;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test JAX-RS JSON Signature and Encryption. Only the client -> service request is 
 * encrypted/signed, not the response.
 */
public class JWEJWSTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String PORT2 = allocatePort(Server.class, 2);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
            );
        Security.addProvider(new BouncyCastleProvider());  
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @org.junit.Test
    public void testEncryptionSignatureListProperties() throws Exception {

        URL busFile = JWEJWSTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());
        
        JwsJsonWriterInterceptor writer = new JwsJsonWriterInterceptor();
        writer.setUseJwsJsonOutputStream(true);
        providers.add(writer);

        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.encryption.properties", "clientEncKeystore.properties");
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
    public void testEncryptionSignatureCompactProperties() throws Exception {

        URL busFile = JWEJWSTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT2 + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.encryption.properties", "clientEncKeystore.properties");
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
    public void testEncryptionSignatureCompactPropertiesSpringConfig() throws Exception {

        URL busFile = JWEJWSTest.class.getResource("cxf-client-config.xml");
        
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] {busFile.toString()});
        DoubleItService proxy = (DoubleItService) ctx.getBean("serviceClient");
        assertNotNull(proxy);
        
        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Number response = proxy.doubleIt(numberToDouble);
        assertEquals(response.getNumber(), 50);
        
        ctx.close();
        
    }

}