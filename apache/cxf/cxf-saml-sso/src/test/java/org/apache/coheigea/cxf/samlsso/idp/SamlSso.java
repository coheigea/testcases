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

package org.apache.coheigea.cxf.samlsso.idp;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectConfirmationDataBean;
import org.apache.ws.security.util.DOM2Writer;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A mock IdP for SAML SSO. It checks a SAMLRequest Issuer + RACS against known values + creates
 * a response. The user is already authenticated via HTTP/BA.
 */
@Path("/samlsso")
public class SamlSso {
    private List<ServiceProvider> serviceProviders;
    private final DocumentBuilderFactory docBuilderFactory;
    private String issuer;
    private MessageContext messageContext;
    
    public SamlSso() {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
    }
    
    @GET
    public javax.ws.rs.core.Response login(@QueryParam("SAMLRequest") String samlRequest,
            @QueryParam("RelayState") String relayState) throws Exception {
        
        byte[] deflatedToken = Base64Utility.decode(samlRequest);
        InputStream tokenStream = new DeflateEncoderDecoder().inflateToken(deflatedToken);
        
        Document responseDoc = DOMUtils.readXml(new InputStreamReader(tokenStream, "UTF-8"));
        AuthnRequest request = 
            (AuthnRequest)OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        System.out.println(DOM2Writer.nodeToString(responseDoc));

        String racs = request.getAssertionConsumerServiceURL();
        String requestIssuer = request.getIssuer().getValue();
        
        // Match the RACS + Issuer against known values
        boolean match = false;
        if (serviceProviders != null) {
            for (ServiceProvider sp : serviceProviders) {
                if (sp.getIssuer() != null && sp.getIssuer().equals(requestIssuer)
                        && ((sp.getRacs() != null && sp.getRacs().equals(racs))
                            || sp.getRacs() == null)) {
                    match = true;
                }
            }
        }
        
        if (!match) {
            throw new BadRequestException();
        }
        
        // Create the response
        Element response = createResponse(request.getID(), racs, requestIssuer);
        String responseStr = encodeResponse(response);
        
        // Perform Redirect to RACS
        UriBuilder ub = UriBuilder.fromUri(racs);
        ub.queryParam("SAMLResponse", responseStr);
        ub.queryParam("RelayState", relayState);
        
        return javax.ws.rs.core.Response.seeOther(ub.build()).build();
    }
    
    @Context 
    public void setMessageContext(MessageContext mc) {
        this.messageContext = mc;
    }

    public List<ServiceProvider> getServiceProviders() {
        return serviceProviders;
    }

    public void setServiceProviders(List<ServiceProvider> serviceProviders) {
        this.serviceProviders = serviceProviders;
    }
    
    private Element createResponse(String requestID, String racs, String requestIssuer) throws Exception {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Status status = 
            SAML2PResponseComponentBuilder.createStatus(
                "urn:oasis:names:tc:SAML:2.0:status:Success", null
            );
        Response response = 
            SAML2PResponseComponentBuilder.createSAMLResponse(requestID, issuer, status);
        
        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setIssuer(issuer);
        String user = messageContext.getSecurityContext().getUserPrincipal().getName();
        callbackHandler.setSubjectName(user);
        
        // Subject Confirmation Data
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress(messageContext.getHttpServletRequest().getRemoteAddr());
        subjectConfirmationData.setInResponseTo(requestID);
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient(racs);
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);
        
        // Audience Restriction
        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        conditions.setAudienceURI(requestIssuer);
        callbackHandler.setConditions(conditions);
        
        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(callbackHandler);
        AssertionWrapper assertion = new AssertionWrapper(samlParms);
        
        Crypto issuerCrypto = CryptoFactory.getInstance("stsKeystore.properties");
        assertion.signAssertion("mystskey", "stskpass", issuerCrypto, false);
        
        response.getAssertions().add(assertion.getSaml2());
        
        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        
        return policyElement;
    }
    
    private String encodeResponse(Element response) throws IOException {
        String responseMessage = DOM2Writer.nodeToString(response);
        System.out.println("RESP: " + responseMessage);

        DeflateEncoderDecoder encoder = new DeflateEncoderDecoder();
        byte[] deflatedBytes = encoder.deflateToken(responseMessage.getBytes("UTF-8"));

        return Base64Utility.encode(deflatedBytes);
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}


