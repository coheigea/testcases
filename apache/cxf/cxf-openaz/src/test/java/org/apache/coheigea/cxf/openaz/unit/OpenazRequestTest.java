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

import java.net.URL;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.openaz.xacml.std.IdentifierImpl;
import org.apache.openaz.xacml.std.StdAttributeValue;
import org.apache.openaz.xacml.std.StdMutableAttribute;
import org.apache.openaz.xacml.std.StdMutableRequest;
import org.apache.openaz.xacml.std.StdMutableRequestAttributes;
import org.apache.openaz.xacml.std.datatypes.DataTypes;
import org.apache.openaz.xacml.std.dom.DOMRequest;
import org.apache.openaz.xacml.std.json.JSONRequest;

/**
 * Some unit tests for creating requests using OpenAZ
 */
public class OpenazRequestTest extends AbstractBusClientServerTestBase {
    
   
    @org.junit.Test
    @org.junit.Ignore
    public void testLoadXMLRequest() throws Exception {
        // URL file = OpenazRequestTest.class.getResource("request.xml");
        // Request request = DOMRequest.load(file.openStream());
    }
    
    @org.junit.Test
    public void testCreateXMLRequest() throws Exception {
        StdMutableRequest request = new StdMutableRequest();
        
        // Add Subject
        StdMutableRequestAttributes subjectRequestAttributes = new StdMutableRequestAttributes();
        subjectRequestAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        
        // Subject Id
        StdMutableAttribute subjectIdAttribute = new StdMutableAttribute();
        subjectIdAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject:subject-id"));
        StdAttributeValue<String> subjectIdAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#string"), "alice");
        subjectIdAttribute.addValue(subjectIdAttributeValue);
        
        // Subject role
        StdMutableAttribute subjectRoleAttribute = new StdMutableAttribute();
        subjectRoleAttribute.setAttributeId(new IdentifierImpl("urn:oasis:names:tc:xacml:2.0:subject:role"));
        StdAttributeValue<String> subjectRoleAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl("http://www.w3.org/2001/XMLSchema#anyURI"), "manager");
        subjectRoleAttribute.addValue(subjectRoleAttributeValue);
        
        subjectRequestAttributes.add(subjectIdAttribute);
        subjectRequestAttributes.add(subjectRoleAttribute);
        request.add(subjectRequestAttributes);
        
        // Add Resource
        StdMutableRequestAttributes resourceAttributes = new StdMutableRequestAttributes();
        resourceAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
        
        StdMutableAttribute resourceAttribute = new StdMutableAttribute();
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
        
        // Convert to JSON String
        String jsonRequest = JSONRequest.toString(request, true);
        System.out.println(jsonRequest);
    }
    
}
