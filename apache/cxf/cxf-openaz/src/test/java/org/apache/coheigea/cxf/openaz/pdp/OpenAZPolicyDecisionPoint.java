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
package org.apache.coheigea.cxf.openaz.pdp;

import java.io.StringReader;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.apache.cxf.rt.security.xacml.pdp.api.PolicyDecisionPoint;
import org.apache.openaz.xacml.api.Decision;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.Response;
import org.apache.openaz.xacml.api.pdp.PDPEngine;
import org.apache.openaz.xacml.api.pdp.PDPEngineFactory;
import org.apache.openaz.xacml.std.StdMutableResponse;
import org.apache.openaz.xacml.std.StdMutableResult;
import org.apache.openaz.xacml.std.dom.DOMRequest;
import org.apache.openaz.xacml.std.dom.DOMResponse;

/**
 * A PDP implementation based on the OpenAZ PDP engine
 */
public class OpenAZPolicyDecisionPoint implements PolicyDecisionPoint {
    
    private final PDPEngine pdpEngine;
    
    public OpenAZPolicyDecisionPoint() throws Exception {
        // Load policies + PDP
        Properties properties = new Properties();
        properties.put("xacml.rootPolicies", "manager");
        properties.put("xacml.referencedPolicies", "doubleit");
        properties.put("manager.file", 
                       "src/test/resources/org/apache/coheigea/cxf/openaz/pdp/manager_role_policy.xml");
        properties.put("doubleit.file", 
            "src/test/resources/org/apache/coheigea/cxf/openaz/pdp/manager_permission_policy.xml");
        
        PDPEngineFactory engineFactory = PDPEngineFactory.newInstance();
        pdpEngine = engineFactory.newEngine(properties);
    }

    public Source evaluate(Source requestSource) {
        try {
            // Convert Source into a DOM Node
            Node requestNode = convertSourceIntoNode(requestSource);
            
            // Convert into a XACML Request
            Request request = DOMRequest.newInstance(requestNode);
            
            // Evaluate request
            Response response = pdpEngine.decide(request);
            
            // Convert back to Source + return
            return convertResponseIntoSource(response);
        } catch (Exception ex) {
            StdMutableResponse response = new StdMutableResponse();
            StdMutableResult result = new StdMutableResult();
            result.setDecision(Decision.NOTAPPLICABLE);
            response.add(result);
            try {
                return convertResponseIntoSource(response);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    private Node convertSourceIntoNode(Source requestSource) throws Exception {
        // Convert Source into a DOM Node
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        DOMResult res = new DOMResult();
        trans.transform(requestSource, res);
        Node nd = res.getNode();
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        
        return nd;
    } 
    
    private Source convertResponseIntoSource(Response response) throws Exception {
        String responseString = DOMResponse.toString(response);
        
        return new StreamSource(new StringReader(responseString));
    }

    
    

}
