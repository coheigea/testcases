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
package org.apache.coheigea.cxf.failover.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.failover.common.Number;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.junit.BeforeClass;

/**
 * A test for CXF using the failover feature using TLS
 */
public class FailoverTLSTest extends AbstractBusClientServerTestBase {

    static final String PORT1 = allocatePort(TLSServer.class);
    static final String PORT2 = allocatePort(TLSServer.class, 2);
    static final String PORT3 = allocatePort(TLSServer.class, 3);
    static final String PORT4 = allocatePort(TLSServer.class, 4);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(TLSServer.class, true)
        );
    }

    // The service is designed to fail (404) every second iteration.
    // The first invocation on PORT1 works fine. The second fails, but the client fails over to PORT2
    // automatically, and the invocation succeeds.
    @org.junit.Test
    public void testFailoverFeature() throws Exception {

        Bus bus = BusFactory.getDefaultBus(true);

        FailoverFeature feature = new FailoverFeature();
        SequentialStrategy strategy = new SequentialStrategy();
        List<String> addresses = new ArrayList<>();
        addresses.add("https://localhost:" + PORT2 + "/doubleit/services");
        strategy.setAlternateAddresses(addresses);
        feature.setStrategy(strategy);

        String address = "https://localhost:" + PORT1 + "/doubleit/services";
        WebClient client = WebClient.create(address, null, Collections.singletonList(feature), null);
        client = client.type("application/xml");
        
        // Configure TLS
        final TLSClientParameters _tlsParams = new TLSClientParameters();

        KeyStore _truststore = null;
        try (InputStream _is = ClassLoaderUtils.getResourceAsStream("truststore.jks", this.getClass()))
        {
          _truststore = KeyStore.getInstance("JKS");
          _truststore.load(_is, "security".toCharArray());
        }

        final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(_truststore);

        final TrustManager[] _tm = trustFactory.getTrustManagers();
        _tlsParams.setTrustManagers(_tm);
        _tlsParams.setDisableCNCheck(true);
        
        //WebClient.getConfig(client).getHttpConduit().setTlsClientParameters(_tlsParams);
        
        // Make sure the TLS settings are also set on the failover conduit
        HTTPConduitConfigurer httpConduitConfigurer = new HTTPConduitConfigurer() {
            public void configure(String name, String address, HTTPConduit c) {
                c.setTlsClientParameters(_tlsParams);
            }
        };
             
        bus.setExtension(httpConduitConfigurer, HTTPConduitConfigurer.class);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        // First call is successful to PORT1
        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);

        // Second call fails over to PORT2
        response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
        
    }
    

}