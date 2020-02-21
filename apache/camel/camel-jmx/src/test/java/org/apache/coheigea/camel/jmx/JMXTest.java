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
package org.apache.coheigea.camel.jmx;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.management.DefaultManagementAgent;
import org.apache.camel.spi.ManagementAgent;

/**
 */
public class JMXTest extends org.junit.Assert {

    @org.junit.Test
    public void testJMX() throws Exception {
        CamelContext context = new DefaultCamelContext();
        ManagementAgent managementAgent = context.getManagementStrategy().getManagementAgent();
        managementAgent.setCreateConnector(true);
        managementAgent.setRegistryPort(5555);
        managementAgent.setServiceUrlPath("jmxrmi");
        //managementAgent.setConnectorPort(1616);
        
        Map<String,String> env = new HashMap<>();
        env.put("jmx.remote.x.access.file", "src/test/resources/jmx.access");
        env.put("jmx.remote.x.password.file", "src/test/resources/jmx.password");
        ((DefaultManagementAgent)managementAgent).setEnvironment(env);
        context.start();
        
        // Sleep to allow time to connect to JMX etc.
        System.out.println("Sleeping...");
        Thread.sleep(60 * 1000);
    }

}
