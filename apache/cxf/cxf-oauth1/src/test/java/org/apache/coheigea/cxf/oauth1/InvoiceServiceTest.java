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

import org.apache.coheigea.cxf.oauth1.balanceservice.BankServer;
import org.apache.coheigea.cxf.oauth1.invoiceservice.InvoiceServer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

/**
 * An invoice service test
 */
public class InvoiceServiceTest extends AbstractBusClientServerTestBase {
    
    static final String BANK_SERVICE_PORT = allocatePort(BankServer.class);
    static final String OAUTH_SERVICE_PORT = allocatePort(BankServer.class, 2);
    static final String INVOICE_SERVICE_PORT = allocatePort(InvoiceServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(BankServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(InvoiceServer.class, true)
        );
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testBrowser() throws Exception {
        System.out.println("Sleeping...");
        Thread.sleep(120 * 1000);
        // TODO No message body writer has been found for response class OAuthAuthorizationData.
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testInvoiceService() throws Exception {
        URL busFile = InvoiceServiceTest.class.getResource("cxf-client.xml");

        String address = "https://localhost:" + INVOICE_SERVICE_PORT + "/invoiceservice/";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());
        WebClient.getConfig(client).getHttpConduit().getClient().setAutoRedirect(true);
        WebClient.getConfig(client).getRequestContext().put(
            org.apache.cxf.message.Message.MAINTAIN_SESSION, Boolean.TRUE);
        client.accept("application/xml");
        
        client.path("/alice");
        client.query("address", "Dublin,Ireland");
        // Response response = client.get();
        
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
        form.set("session_authenticity_token", authenticityToken);
        form.set("oauthDecision", "allow");
        // TODO form.set("oauth_token", requestToken);
        client.post(form);
    }
    
    
}
