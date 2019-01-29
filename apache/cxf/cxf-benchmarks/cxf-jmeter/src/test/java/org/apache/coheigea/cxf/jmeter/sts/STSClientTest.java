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
package org.apache.coheigea.cxf.jmeter.sts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.junit.BeforeClass;
import org.w3c.dom.Document;

/**
 * Some test-cases for the CXF SecurityTokenService, which requires UsernameToken authentication (but no TLS)
 */
public class STSClientTest extends AbstractBusClientServerTestBase {
    
    private static final String PORT = allocatePort(STSServer.class);
    
    private static final String SAML2_TOKEN_TYPE =
    		"http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String BEARER_KEYTYPE =
    		"http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private static final String DEFAULT_ADDRESS =
    		"https://localhost:8081/doubleit/services/doubleittransportsaml1";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }
   
    @org.junit.Test
    public void testIssueSOAP() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSClientTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        // Get a token
        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertTrue(SAML2_TOKEN_TYPE.equals(token.getTokenType()));
        assertTrue(token.getToken() != null);
    }
    
    @org.junit.Test
    public void testIssueREST() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = STSClientTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        // Get a token
        String address = "http://localhost:" + PORT + "/SecurityTokenServiceREST/token";
        WebClient client = WebClient.create(address, "alice", "security", busFile.toString());

        client.accept("application/xml");
        client.path("saml2.0");

        Response response = client.get();
        Document assertionDoc = response.readEntity(Document.class);
        assertNotNull(assertionDoc);
    }
    
    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Bus bus,
        String endpointAddress
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);

        stsClient.setWsdlLocation("http://localhost:" + PORT + "/SecurityTokenService/STS?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}STS_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
        		SecurityConstants.CALLBACK_HANDLER,
        		"org.apache.coheigea.cxf.jmeter.sts.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.IS_BSP_COMPLIANT, "false");

        stsClient.setEnableAppliesTo(false);
        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);

        return stsClient.requestSecurityToken(endpointAddress);
    }
    
}