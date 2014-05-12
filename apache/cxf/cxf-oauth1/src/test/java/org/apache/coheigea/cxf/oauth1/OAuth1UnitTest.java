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
package org.apache.coheigea.cxf.oauth1;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.oauth1.service.BankServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

/**
 * Some unit tests for the services to check everything is working ok
 */
public class OAuth1UnitTest extends AbstractBusClientServerTestBase {
    
    // private static final String PORT = allocatePort(Server.class);
    static final String BANK_SERVICE_PORT = allocatePort(BankServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(BankServer.class, true)
        );
    }
    
    @org.junit.Test
    public void testCustomerBankService() throws Exception {
        URL busFile = OAuth1UnitTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + BANK_SERVICE_PORT + "/bankservice/customers/balance";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/alice");
        Response response =  client.post(25);
        assertEquals(response.getStatus(), 204);
        
        response = client.get();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Integer.class).intValue(), 25);
        
        // Test that "bob" can't read Alice's account
        client = WebClient.create(address, "bob", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/bob");
        response =  client.post(35);
        assertEquals(response.getStatus(), 204);
        
        client.reset();
        client.path("alice");
        response = client.get();
        assertEquals(response.getStatus(), 403);
    }
   
    @org.junit.Test
    public void testPartnerBankService() throws Exception {
        URL busFile = OAuth1UnitTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + BANK_SERVICE_PORT + "/bankservice/partners/balance";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        client.type("text/plain").accept("text/plain");
        
        client.path("/alice");
        Response response =  client.post(25);
        assertEquals(response.getStatus(), 401);
    }
    
}
