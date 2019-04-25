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
package org.apache.coheigea.cxf.jaxrs.httpsig;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.httpsig.Number;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.httpsignature.filters.CreateSignatureInterceptor;
import org.apache.cxf.rs.security.httpsignature.filters.VerifySignatureClientFilter;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test JAX-RS HTTP Signature
 */
public class HTTPSignatureTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);

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
    public void testHTTPSignatureUsingProperties() throws Exception {

        URL busFile = HTTPSignatureTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new CreateSignatureInterceptor());
        providers.add(new VerifySignatureClientFilter());
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client = client.type("application/xml");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.signature.out.properties", "client.httpsig.properties");
        properties.put("rs.security.signature.in.properties", "service.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

}
