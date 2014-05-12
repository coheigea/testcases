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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.coheigea.cxf.sts.xacml.authorization.AuthorizationTest;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.xacml.AbstractXACMLAuthorizingInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.w3c.dom.Document;

/**
 * Send the XACML Request to the PDP for evaluation.
 */
public class XACMLAuthorizingInterceptor extends AbstractXACMLAuthorizingInterceptor {

    @Override
    public ResponseType performRequest(RequestType arg0, Message arg1) throws Exception {
        URL busFile = 
            XACMLAuthorizingInterceptor.class.getResource("../authorization/cxf-pdp-client.xml");

        String address = "https://localhost:" + AuthorizationTest.PDP_PORT + "/authorization/pdp";
        WebClient client = WebClient.create(address, "myservicekey", "skpass", busFile.toString());
        client.type("text/xml").accept("text/xml");
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();
        Source source = new DOMSource(OpenSAMLUtil.toDom(arg0, doc));

        Response response = client.post(source);
        response.bufferEntity();
        
        Document responseDoc = StaxUtils.read(response.readEntity(Source.class));

        return (ResponseType)OpenSAMLUtil.fromDom(responseDoc.getDocumentElement()); 
    }
    
    
}