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
package org.apache.coheigea.cxf.openaz.unit;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.openaz.xacml.api.Decision;
import org.apache.openaz.xacml.api.IdReferenceMatch;
import org.apache.openaz.xacml.api.Identifier;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.Response;
import org.apache.openaz.xacml.api.VersionMatch;
import org.apache.openaz.xacml.api.pdp.PDPEngine;
import org.apache.openaz.xacml.api.pdp.PDPEngineFactory;
import org.apache.openaz.xacml.pdp.policy.PolicyDef;
import org.apache.openaz.xacml.pdp.policy.PolicyFinder;
import org.apache.openaz.xacml.pdp.policy.PolicyFinderFactory;
import org.apache.openaz.xacml.pdp.policy.PolicyFinderResult;
import org.apache.openaz.xacml.pdp.policy.PolicySet;
import org.apache.openaz.xacml.pdp.policy.dom.DOMPolicyDef;
import org.apache.openaz.xacml.std.IdentifierImpl;
import org.apache.openaz.xacml.std.StdAttributeValue;
import org.apache.openaz.xacml.std.StdIdReferenceMatch;
import org.apache.openaz.xacml.std.StdMutableAttribute;
import org.apache.openaz.xacml.std.StdMutableRequest;
import org.apache.openaz.xacml.std.StdMutableRequestAttributes;
import org.apache.openaz.xacml.std.StdVersionMatch;
import org.apache.openaz.xacml.std.dom.DOMStructureException;
import org.apache.openaz.xacml.std.json.JSONRequest;

/**
 * Some unit tests for creating requests/policies/etc. using OpenAZ
 */
public class OpenazTest extends AbstractBusClientServerTestBase {
    
    @org.junit.Test
    public void testCreateRequest() throws Exception {
        Request request = createRequest("manager");
        
        // Convert to JSON String
        String jsonRequest = JSONRequest.toString(request, true);
        System.out.println(jsonRequest);
    }
    
    private Request createRequest(String roleName) {
        StdMutableRequest request = new StdMutableRequest();
        
        // Add Subject
        StdMutableRequestAttributes subjectRequestAttributes = new StdMutableRequestAttributes();
        subjectRequestAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        
        // Subject Id
        StdMutableAttribute subjectIdAttribute = new StdMutableAttribute();
        subjectIdAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        subjectIdAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject:subject-id"));
        StdAttributeValue<String> subjectIdAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"), "alice");
        subjectIdAttribute.addValue(subjectIdAttributeValue);
        
        // Subject role
        StdMutableAttribute subjectRoleAttribute = new StdMutableAttribute();
        subjectRoleAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        subjectRoleAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:2.0:subject:role"));
        StdAttributeValue<String> subjectRoleAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#anyURI"), roleName);
        subjectRoleAttribute.addValue(subjectRoleAttributeValue);
        
        subjectRequestAttributes.add(subjectIdAttribute);
        subjectRequestAttributes.add(subjectRoleAttribute);
        request.add(subjectRequestAttributes);
        
        // Add Resource
        StdMutableRequestAttributes resourceAttributes = new StdMutableRequestAttributes();
        resourceAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
        
        StdMutableAttribute resourceAttribute = new StdMutableAttribute();
        resourceAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
        resourceAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:resource:resource-id"));
        StdAttributeValue<String> resourceAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"), 
                "{http://www.example.org/contract/DoubleIt}DoubleItService#DoubleIt");
        resourceAttribute.addValue(resourceAttributeValue);
        
        resourceAttributes.add(resourceAttribute);
        request.add(resourceAttributes);
        
        // Add Action
        StdMutableRequestAttributes actionAttributes = new StdMutableRequestAttributes();
        actionAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:action"));
        
        StdMutableAttribute actionAttribute = new StdMutableAttribute();
        actionAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:action"));
        actionAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:action:action-id"));
        StdAttributeValue<String> actionAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"), "execute");
        actionAttribute.addValue(actionAttributeValue);
        
        actionAttributes.add(actionAttribute);
        request.add(actionAttributes);
        
        // Add Environment
        StdMutableRequestAttributes environmentAttributes = new StdMutableRequestAttributes();
        environmentAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:environment"));
        
        StdMutableAttribute environmentAttribute = new StdMutableAttribute();
        environmentAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"));
        StdAttributeValue<String> environmentAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#dateTime"), 
                "2015-07-14T11:02:01.465+01:00");
        environmentAttribute.addValue(environmentAttributeValue);
        
        environmentAttributes.add(environmentAttribute);
        request.add(environmentAttributes);
        
        return request;
    }
    
    @org.junit.Test
    public void testLoadPolicies() throws IOException, DOMStructureException {
        URL rolePolicy = OpenazTest.class.getResource("manager_role_policy.xml");
        PolicyDef rolePolicyDef = DOMPolicyDef.load(rolePolicy.openStream());
        assertNotNull(rolePolicyDef);
        
        URL permissionPolicy = OpenazTest.class.getResource("manager_permission_policy.xml");
        PolicyDef permissionPolicyDef = DOMPolicyDef.load(permissionPolicy.openStream());
        assertNotNull(permissionPolicyDef);
    }
    
    @org.junit.Test
    public void testFindPoliciesViaProperties() throws Exception {
        Properties properties = new Properties();
        properties.put("xacml.rootPolicies", "manager");
        properties.put("xacml.referencedPolicies", "doubleit");
        properties.put("manager.file", 
                       "src/test/resources/org/apache/coheigea/cxf/openaz/unit/manager_role_policy.xml");
        properties.put("doubleit.file", 
            "src/test/resources/org/apache/coheigea/cxf/openaz/unit/manager_permission_policy.xml");

        PolicyFinderFactory policyFinderFactory = PolicyFinderFactory.newInstance();
        PolicyFinder policyFinder = policyFinderFactory.getPolicyFinder(properties);
        
        Identifier identifer = new IdentifierImpl("org.apache.role.manager");
        VersionMatch version = StdVersionMatch.newInstance("1.0");
        IdReferenceMatch idReferenceMatch = new StdIdReferenceMatch(identifer, version, null, null);
        
        PolicyFinderResult<PolicySet> finderResult = policyFinder.getPolicySet(idReferenceMatch);
        PolicySet managerPolicy = finderResult.getPolicyDef();
        assertNotNull(managerPolicy);
        
        identifer = new IdentifierImpl("org.apache.permissions.doubleit");
        idReferenceMatch = new StdIdReferenceMatch(identifer, version, null, null);
        
        finderResult = policyFinder.getPolicySet(idReferenceMatch);
        PolicySet permissionPolicy = finderResult.getPolicyDef();
        assertNotNull(permissionPolicy);
    }
    
    @org.junit.Test
    public void testEvaluateRequestinPDP() throws Exception {
        Properties properties = new Properties();
        properties.put("xacml.rootPolicies", "manager");
        properties.put("xacml.referencedPolicies", "doubleit");
        properties.put("manager.file", 
                       "src/test/resources/org/apache/coheigea/cxf/openaz/unit/manager_role_policy.xml");
        properties.put("doubleit.file", 
            "src/test/resources/org/apache/coheigea/cxf/openaz/unit/manager_permission_policy.xml");
        
        PDPEngineFactory engineFactory = PDPEngineFactory.newInstance();
        PDPEngine pdpEngine = engineFactory.newEngine(properties);
        
        // Successful request
        Request request = createRequest("manager");
        
        Response response = pdpEngine.decide(request);
        assertFalse(response.getResults().isEmpty());
        assertTrue(response.getResults().iterator().next().getDecision() == Decision.PERMIT);
    
        // Unsuccessful request
        request = createRequest("employee");
        
        response = pdpEngine.decide(request);
        assertFalse(response.getResults().isEmpty());
        assertTrue(response.getResults().iterator().next().getDecision() != Decision.PERMIT);
    }
    
}
