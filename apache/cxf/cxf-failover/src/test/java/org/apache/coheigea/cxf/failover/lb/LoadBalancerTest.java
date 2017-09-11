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
package org.apache.coheigea.cxf.failover.lb;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

import org.apache.coheigea.cxf.failover.common.Number;

/**
 * A test for CXF using the LoadDistributorFeature. The client distributes the service calls
 * to both PORT1 and PORT2 sequentially.
 */
public class LoadBalancerTest extends AbstractBusClientServerTestBase {

    static final String PORT1 = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);

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
    public void testLoadBalancerFeature() throws Exception {

        URL busFile = LoadBalancerTest.class.getResource("cxf-client.xml");

        LoadDistributorFeature feature = new LoadDistributorFeature();
        SequentialStrategy strategy = new SequentialStrategy();
        List<String> addresses = new ArrayList<>();
        addresses.add("http://localhost:" + PORT1 + "/doubleit/services");
        addresses.add("http://localhost:" + PORT2 + "/doubleit/services");
        strategy.setAlternateAddresses(addresses);
        feature.setStrategy(strategy);

        String address = "http://localhost:" + PORT1 + "/doubleit/services";
        WebClient client = WebClient.create(address, null,
                                            Collections.singletonList(feature), busFile.toString());
        client = client.type("application/xml");

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        // First call is successful to PORT1
        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);

        // Second call is successful to PORT2
        response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }

}
