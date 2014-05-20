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

package org.apache.coheigea.cxf.sts.xacml.common;

import java.net.URL;

import javax.ws.rs.core.Response;
import javax.xml.transform.Source;

import org.apache.coheigea.cxf.sts.xacml.authorization.AuthorizationTest;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rt.security.xacml.pdp.api.PolicyDecisionPoint;

/**
 * Send the XACML Request to the PDP for evaluation.
 */
public class PolicyDecisionPointImpl implements PolicyDecisionPoint {

    public Source evaluate(Source arg0) {
        URL busFile = 
            PolicyDecisionPointImpl.class.getResource("../authorization/cxf-pdp-client.xml");

        String address = "https://localhost:" + AuthorizationTest.PDP_PORT + "/authorization/pdp";
        WebClient client = WebClient.create(address, "myservicekey", "skpass", busFile.toString());
        client.type("text/xml").accept("text/xml");
        
        Response response = client.post(arg0);
        response.bufferEntity();
        
        return response.readEntity(Source.class);
    }
    
    
}