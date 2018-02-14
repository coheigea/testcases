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
package org.apache.coheigea.cxf.jaxrs.swagger2;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;

/**
 * A simple example of how to generate Swagger 2 / OpenAPI documentation for a JAX-RS endpoint.
 */
@org.junit.Ignore("Remove this annotation to run the test")
public class CXFSwaggerTest extends AbstractBusClientServerTestBase {

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
    public void testSwagger2Browser() throws Exception {
    	String json = "http://localhost:" + PORT + "/swagger.json";
    	System.out.println("Swagger JSON: " + json);

    	String openapi = "http://localhost:" + PORT + "/openapi.json";
    	System.out.println("OpenAPI JSON: " + openapi);

    	String swaggerUI = "http://localhost:" + PORT + "/api-docs?url=/swagger.json";
    	System.out.println("Swagger UI: " + swaggerUI);

    	Thread.sleep(2 * 60 * 1000);
    }

}
