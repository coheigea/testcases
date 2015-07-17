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

package org.apache.coheigea.cxf.openaz.xacml3;

import java.security.Principal;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.xacml.CXFMessageParser;
import org.apache.cxf.rt.security.saml.xacml.XACMLConstants;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.RequestAttributes;
import org.apache.openaz.xacml.std.IdentifierImpl;
import org.apache.openaz.xacml.std.StdAttributeValue;
import org.apache.openaz.xacml.std.StdMutableAttribute;
import org.apache.openaz.xacml.std.StdMutableRequest;
import org.apache.openaz.xacml.std.StdMutableRequestAttributes;
import org.joda.time.DateTime;

/**
 * This class constructs an XACML 3.0 Request given a Principal, list of roles and MessageContext, 
 * following the SAML 2.0 profile of XACML 3.0. The principal name is inserted as the Subject ID,
 * and the list of roles associated with that principal are inserted as Subject roles. The action
 * to send defaults to "execute". 
 * 
 * For a SOAP Service, the resource-id Attribute refers to the 
 * "{serviceNamespace}serviceName#{operationNamespace}operationName" String (shortened to
 * "{serviceNamespace}serviceName#operationName" if the namespaces are identical). The 
 * "{serviceNamespace}serviceName", "{operationNamespace}operationName" and resource URI are also
 * sent to simplify processing at the PDP side.
 * 
 * For a REST service the request URL is the resource. You can also configure the ability to 
 * send the truncated request URI instead for a SOAP or REST service. The current DateTime is 
 * also sent in an Environment, however this can be disabled via configuration.
 */
public class DefaultXACML3RequestBuilder implements XACML3RequestBuilder {

    private boolean sendDateTime = true;
    private String action = "execute";
    private boolean sendFullRequestURL = true;

    /**
     * Create an XACML Request given a Principal, list of roles and Message.
     */
    public Request createRequest(Principal principal, List<String> roles, Message message)
        throws Exception {
        CXFMessageParser messageParser = new CXFMessageParser(message);
        String issuer = messageParser.getIssuer();
        
        String actionToUse = messageParser.getAction(action);

        RequestAttributes subject = createSubject(principal, roles, issuer);
        RequestAttributes resource = createResource(messageParser);
        RequestAttributes action = createAction(actionToUse);
        RequestAttributes environment = createEnvironment();

        StdMutableRequest request = new StdMutableRequest();
        request.add(subject);
        request.add(resource);
        request.add(action);
        request.add(environment);
        
        return request;
    }

    private RequestAttributes createResource(CXFMessageParser messageParser) {
        StdMutableRequestAttributes resourceAttributes = new StdMutableRequestAttributes();
        resourceAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
        
        // Resource-id
        String resourceId = null;
        boolean isSoapService = messageParser.isSOAPService();
        if (isSoapService) {
            QName serviceName = messageParser.getWSDLService();
            QName operationName = messageParser.getWSDLOperation();
            
            if (serviceName != null) {
                resourceId = serviceName.toString() + "#";
                if (serviceName.getNamespaceURI() != null 
                    && serviceName.getNamespaceURI().equals(operationName.getNamespaceURI())) {
                    resourceId += operationName.getLocalPart();
                } else {
                    resourceId += operationName.toString();
                }
            } else {
                resourceId = operationName.toString();
            }
        } else {
            resourceId = messageParser.getResourceURI(sendFullRequestURL);
        }
        
        StdMutableAttribute resourceAttribute = new StdMutableAttribute();
        resourceAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
        resourceAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.RESOURCE_ID));
        StdAttributeValue<String> resourceAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), resourceId);
        resourceAttribute.addValue(resourceAttributeValue);
        
        resourceAttributes.add(resourceAttribute);
        
        if (isSoapService) {
            // WSDL Service
            QName wsdlService = messageParser.getWSDLService();
            if (wsdlService != null) {
                StdMutableAttribute wsdlServiceAttribute = new StdMutableAttribute();
                wsdlServiceAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
                wsdlServiceAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.RESOURCE_WSDL_SERVICE_ID));
                StdAttributeValue<String> wsdlServiceAttributeValue = 
                    new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), wsdlService.toString());
                wsdlServiceAttribute.addValue(wsdlServiceAttributeValue);
                
                resourceAttributes.add(wsdlServiceAttribute);
            }
            
            // WSDL Operation
            QName wsdlOperation = messageParser.getWSDLOperation();
            StdMutableAttribute wsdlOperationAttribute = new StdMutableAttribute();
            wsdlOperationAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
            wsdlOperationAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.RESOURCE_WSDL_OPERATION_ID));
            StdAttributeValue<String> wsdlOperationAttributeValue = 
                new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), wsdlOperation.toString());
            wsdlOperationAttribute.addValue(wsdlOperationAttributeValue);
            
            resourceAttributes.add(wsdlOperationAttribute);
            
            // WSDL Endpoint
            String endpointURI = messageParser.getResourceURI(sendFullRequestURL);
            StdMutableAttribute wsdlEndpointAttribute = new StdMutableAttribute();
            wsdlEndpointAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
            wsdlEndpointAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.RESOURCE_WSDL_ENDPOINT));
            StdAttributeValue<String> wsdlEndpointAttributeValue = 
                new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), endpointURI);
            wsdlEndpointAttribute.addValue(wsdlEndpointAttributeValue);
            
            resourceAttributes.add(wsdlEndpointAttribute);
        }
        
        return resourceAttributes;
    }

    private RequestAttributes createEnvironment() {
        if (sendDateTime) {
            StdMutableRequestAttributes environmentAttributes = new StdMutableRequestAttributes();
            environmentAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:environment"));
            
            StdMutableAttribute environmentAttribute = new StdMutableAttribute();
            environmentAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.CURRENT_DATETIME));
            StdAttributeValue<String> environmentAttributeValue = 
                new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_DATETIME), new DateTime().toString());
            environmentAttribute.addValue(environmentAttributeValue);
            
            environmentAttributes.add(environmentAttribute);
            
            return environmentAttributes;
        }
        return null;
    }

    private RequestAttributes createSubject(Principal principal, List<String> roles, String issuer) {
        // Add Subject
        StdMutableRequestAttributes subjectRequestAttributes = new StdMutableRequestAttributes();
        subjectRequestAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        
        // Subject Id
        StdMutableAttribute subjectIdAttribute = new StdMutableAttribute();
        subjectIdAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        subjectIdAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.SUBJECT_ID));
        StdAttributeValue<String> subjectIdAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), principal.getName());
        subjectIdAttribute.setIssuer(issuer);
        subjectIdAttribute.addValue(subjectIdAttributeValue);
        subjectRequestAttributes.add(subjectIdAttribute);
        
        // Subject role
        if (roles != null) {
            StdMutableAttribute subjectRoleAttribute = new StdMutableAttribute();
            subjectRoleAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
            subjectRoleAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.SUBJECT_ROLE));
            subjectRoleAttribute.setIssuer(issuer);
            for (String role : roles) {
                StdAttributeValue<String> subjectRoleAttributeValue = 
                    new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_ANY_URI), role);
                subjectRoleAttribute.addValue(subjectRoleAttributeValue);
            }
            subjectRequestAttributes.add(subjectRoleAttribute);
        }
        
        return subjectRequestAttributes;
    }
    
    private RequestAttributes createAction(String actionToUse) {
        StdMutableRequestAttributes actionAttributes = new StdMutableRequestAttributes();
        actionAttributes.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:action"));
        
        StdMutableAttribute actionAttribute = new StdMutableAttribute();
        actionAttribute.setCategory(new IdentifierImpl("urn:oasis:names:tc:xacml:3.0:attribute-category:action"));
        actionAttribute.setAttributeId(new IdentifierImpl(XACMLConstants.ACTION_ID));
        StdAttributeValue<String> actionAttributeValue = 
            new StdAttributeValue<String>(new IdentifierImpl(XACMLConstants.XS_STRING), actionToUse);
        actionAttribute.addValue(actionAttributeValue);
        
        actionAttributes.add(actionAttribute);
        
        return actionAttributes;
    }

    /**
     * Set a new Action String to use
     */
    public void setAction(String action) {
        this.action = action;
    }

    public void setSendDateTime(boolean sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    /**
     * Whether to send the full Request URL as the resource or not. If set to true,
     * the full Request URL will be sent for both a JAX-WS and JAX-RS service. If set
     * to false (the default), a JAX-WS service will send the "{namespace}operation" QName,
     * and a JAX-RS service will send the RequestURI (i.e. minus the initial https:<ip> prefix).
     */
    public void setSendFullRequestURL(boolean sendFullRequestURL) {
        this.sendFullRequestURL = sendFullRequestURL;
    }

}
