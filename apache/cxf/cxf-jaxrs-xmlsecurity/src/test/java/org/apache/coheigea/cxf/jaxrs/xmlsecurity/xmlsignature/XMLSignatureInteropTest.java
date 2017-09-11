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
package org.apache.coheigea.cxf.jaxrs.xmlsecurity.xmlsignature;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.xmlsecurity.common.Number;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.xml.XmlSecOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * An interop test that tests DOM + StAX clients with DOM + StAX service endpoints.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class XMLSignatureInteropTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String STAX_PORT = allocatePort(StaxServer.class);

    final TestParam test;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(StaxServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {

        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false)},
                                                {new TestParam(STAX_PORT, false)},
                                                {new TestParam(PORT, true)},
                                                {new TestParam(STAX_PORT, true)},
        });
    }

    public XMLSignatureInteropTest(TestParam type) {
        this.test = type;
    }

    @org.junit.Test
    public void testXMLSignature() throws Exception {

        URL busFile = XMLSignatureInteropTest.class.getResource("cxf-client.xml");

        String address = "http://localhost:" + test.port + "/doubleit/services";
        WebClient client = WebClient.create(address, busFile.toString());
        client = client.type("application/xml");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("ws-security.callback-handler",
                       "org.apache.coheigea.cxf.jaxrs.xmlsecurity.common.CommonCallbackHandler");
        properties.put("ws-security.signature.username", "myclientkey");

        properties.put("ws-security.signature.properties", "clientKeystore.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        if (test.streaming) {
            XmlSecOutInterceptor sigInterceptor = new XmlSecOutInterceptor();
            sigInterceptor.setSignRequest(true);
            WebClient.getConfig(client).getOutInterceptors().add(sigInterceptor);
        } else {
            XmlSigOutInterceptor sigInterceptor = new XmlSigOutInterceptor();
            WebClient.getConfig(client).getOutInterceptors().add(sigInterceptor);
        }

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

    private static final class TestParam {
        final String port;
        final boolean streaming;

        public TestParam(String p, boolean b) {
            port = p;
            streaming = b;
        }

        public String toString() {
            return port + ":" + (streaming ? "streaming" : "dom");
        }

    }

}
