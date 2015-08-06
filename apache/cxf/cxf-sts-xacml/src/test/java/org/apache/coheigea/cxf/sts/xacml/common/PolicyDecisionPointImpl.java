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
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.coheigea.cxf.sts.xacml.authorization.AuthorizationTest;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rt.security.saml.xacml2.PolicyDecisionPoint;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Send the XACML Request to the PDP for evaluation.
 */
public class PolicyDecisionPointImpl implements PolicyDecisionPoint {

    public ResponseType evaluate(RequestType requestType) {
        // Convert it into a DOM Element
        Document doc = DOMUtils.createDocument();
        try {
            Element requestElement = OpenSAMLUtil.toDom(requestType, doc);
            Source source = new DOMSource(requestElement);
            
            URL busFile = 
                PolicyDecisionPointImpl.class.getResource("../authorization/cxf-pdp-client.xml");
    
            String address = "https://localhost:" + AuthorizationTest.PDP_PORT + "/authorization/pdp";
            WebClient client = WebClient.create(address, "myservicekey", "skpass", busFile.toString());
            client.type("text/xml").accept("text/xml");
            
            Response response = client.post(source);
            response.bufferEntity();
            
            Source responseSource = response.readEntity(Source.class);
            
            // Convert back into OpenSAML
            Document responseDoc = StaxUtils.read(responseSource);
            
            return (ResponseType)OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        } catch (WSSecurityException ex) {
            throw new RuntimeException(ex.getMessage());
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

}