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
package org.apache.coheigea.cxf.sts.xacml.pdp.xacml3;

import java.util.Properties;

import org.apache.coheigea.cxf.sts.xacml.authorization.xacml3.PolicyDecisionPoint;
import org.apache.openaz.xacml.api.Decision;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.Response;
import org.apache.openaz.xacml.api.pdp.PDPEngine;
import org.apache.openaz.xacml.api.pdp.PDPEngineFactory;
import org.apache.openaz.xacml.std.StdMutableResponse;
import org.apache.openaz.xacml.std.StdMutableResult;
import org.apache.openaz.xacml.std.json.JSONRequest;
import org.apache.openaz.xacml.std.json.JSONResponse;

/**
 * A PDP implementation based on the OpenAZ PDP engine. It accepts a JSON XACML Request.
 */
public class OpenAZPolicyDecisionPoint implements PolicyDecisionPoint {
    
    private final PDPEngine pdpEngine;
    
    public OpenAZPolicyDecisionPoint() throws Exception {
        // Load policies + PDP
        Properties properties = new Properties();
        properties.put("xacml.rootPolicies", "manager");
        properties.put("xacml.referencedPolicies", "doubleit");
        properties.put("manager.file", 
                       "src/test/resources/org/apache/coheigea/cxf/sts/xacml/pdp/xacml3/boss_role_policy.xml");
        properties.put("doubleit.file", 
            "src/test/resources/org/apache/coheigea/cxf/sts/xacml/pdp/xacml3/boss_permission_policy.xml");
        
        PDPEngineFactory engineFactory = PDPEngineFactory.newInstance();
        pdpEngine = engineFactory.newEngine(properties);
    }

    public String evaluate(String requestString) {
        try {
            // Convert into a XACML Request
            Request request = JSONRequest.load(requestString);
            
            // Evaluate request
            Response response = pdpEngine.decide(request);
            
            // Convert back to Source + return
            return JSONResponse.toString(response);
        } catch (Exception ex) {
            StdMutableResponse response = new StdMutableResponse();
            StdMutableResult result = new StdMutableResult();
            result.setDecision(Decision.NOTAPPLICABLE);
            response.add(result);
            try {
                return JSONResponse.toString(response);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

}
