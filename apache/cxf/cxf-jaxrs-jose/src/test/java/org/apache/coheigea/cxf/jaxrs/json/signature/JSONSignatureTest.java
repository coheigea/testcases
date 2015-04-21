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
package org.apache.coheigea.cxf.jaxrs.json.signature;

import java.net.URL;
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
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.BeforeClass;

/**
 * Test JAX-RS JSON Signature. Only the client -> service request is signed, not the response.
 */
public class JSONSignatureTest extends AbstractBusClientServerTestBase {
    
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
    }
    
    @org.junit.Test
    public void testSignatureListProperties() throws Exception {

        URL busFile = JSONSignatureTest.class.getResource("cxf-client.xml");

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
    @org.junit.Ignore
    public void testSignatureDynamicProperties() throws Exception {

        URL busFile = JSONSignatureTest.class.getResource("cxf-client.xml");

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
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.jws.content.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        
        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);
        
        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }
    
    
    @org.junit.Test
    @org.junit.Ignore
    public void testSignatureCompact() throws Exception {

        URL busFile = JSONSignatureTest.class.getResource("cxf-client.xml");

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
    
}
