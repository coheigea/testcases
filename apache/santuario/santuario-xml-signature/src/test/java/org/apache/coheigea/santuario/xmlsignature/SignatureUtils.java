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
package org.apache.coheigea.santuario.xmlsignature;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.xml.security.Init;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.impl.securityToken.X509SecurityToken;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SignedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.X509TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.transforms.Transforms;
import org.junit.Assert;

/**
 * Some utility methods for signing/verifying documents
 */
public final class SignatureUtils {
    
    static {
        Init.init();
    }
    
    private SignatureUtils() {
        // complete
    }
    
    /**
     * Sign the document using the DOM API of Apache Santuario - XML Security for Java.
     * It signs a list of QNames that it finds in the Document via XPath.
     */
    public static void signUsingDOM(
        Document document,
        List<QName> namesToSign,
        String algorithm,
        Key signingKey,
        X509Certificate signingCert
    ) throws Exception {
        XMLSignature sig = 
            new XMLSignature(document, "", algorithm, "http://www.w3.org/2001/10/xml-exc-c14n#");
        Element root = document.getDocumentElement();
        root.appendChild(sig.getElement());

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        for (QName nameToSign : namesToSign) {
            String expression = "//*[local-name()='" + nameToSign.getLocalPart() + "']";
            NodeList elementsToSign =
                    (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
            for (int i = 0; i < elementsToSign.getLength(); i++) {
                Element elementToSign = (Element)elementsToSign.item(i);
                Assert.assertNotNull(elementToSign);
                String id = UUID.randomUUID().toString();
                elementToSign.setAttributeNS(null, "Id", id);
                elementToSign.setIdAttributeNS(null, "Id", true);

                Transforms transforms = new Transforms(document);
                transforms.addTransform("http://www.w3.org/2001/10/xml-exc-c14n#");
                sig.addDocument("#" + id, transforms, "http://www.w3.org/2000/09/xmldsig#sha1");
            }
        }

        sig.sign(signingKey);

        String expression = "//ds:Signature[1]";
        Element sigElement =
                (Element) xpath.evaluate(expression, document, XPathConstants.NODE);
        Assert.assertNotNull(sigElement);
        
        if (signingCert != null) {
            sig.addKeyInfo(signingCert);
        }
    }
    
    /**
     * Verify the document using the DOM API of Apache Santuario - XML Security for Java.
     * It finds a list of QNames via XPath and uses the DOM API to mark them as having an 
     * "Id".
     */
    public static void verifyUsingDOM(
        Document document,
        List<QName> namesToSign,
        X509Certificate cert
    ) throws Exception {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        // Find the Signature Element
        String expression = "//dsig:Signature[1]";
        Element sigElement =
                (Element) xpath.evaluate(expression, document, XPathConstants.NODE);
        Assert.assertNotNull(sigElement);

        for (QName nameToSign : namesToSign) {
            expression = "//*[local-name()='" + nameToSign.getLocalPart() + "']";
            Element signedElement =
                (Element) xpath.evaluate(expression, document, XPathConstants.NODE);
            Assert.assertNotNull(signedElement);
            signedElement.setIdAttributeNS(null, "Id", true);
        }

        XMLSignature signature = new XMLSignature(sigElement, "");
        
        // Check we have a KeyInfo
        KeyInfo ki = signature.getKeyInfo();
        Assert.assertNotNull(ki);

        // Check the Signature value
        Assert.assertTrue(signature.checkSignatureValue(cert));
    }

    /**
     * Sign the document using the StAX API of Apache Santuario - XML Security for Java.
     */
    public static ByteArrayOutputStream signUsingStAX(
        InputStream inputStream,
        List<QName> namesToSign,
        String algorithm,
        Key signingKey,
        X509Certificate signingCert
    ) throws Exception {
        // Set up the Configuration
        XMLSecurityProperties properties = new XMLSecurityProperties();
        List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        
        properties.setSignatureAlgorithm(algorithm);
        properties.setSignatureCerts(new X509Certificate[]{signingCert});
        properties.setSignatureKey(signingKey);
        properties.setSignatureKeyIdentifier(
                SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier);
        
        for (QName nameToSign : namesToSign) {
            SecurePart securePart = new SecurePart(nameToSign, SecurePart.Modifier.Content);
            properties.addSignaturePart(securePart);
        }

        OutboundXMLSec outboundXMLSec = XMLSec.getOutboundXMLSec(properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter xmlStreamWriter = outboundXMLSec.processOutMessage(baos, "UTF-8");

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlStreamReader = 
                xmlInputFactory.createXMLStreamReader(inputStream);

        XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
        xmlStreamWriter.close();
        
        return baos;
    }
    
    /**
     * Verify the document using the StAX API of Apache Santuario - XML Security for Java.
     */
    public static void verifyUsingStAX(
        InputStream inputStream,
        List<QName> namesToSign,
        X509Certificate cert
    ) throws Exception {
        // Set up the Configuration
        XMLSecurityProperties properties = new XMLSecurityProperties();
        List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
        actions.add(XMLSecurityConstants.SIGNATURE);
        properties.setActions(actions);
        
        properties.setSignatureVerificationKey(cert.getPublicKey());
        
        InboundXMLSec inboundXMLSec = XMLSec.getInboundWSSec(properties);
        
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
        
        TestSecurityEventListener eventListener = new TestSecurityEventListener();
        XMLStreamReader securityStreamReader = 
            inboundXMLSec.processInMessage(xmlStreamReader, null, eventListener);
        
        while (securityStreamReader.hasNext()) {
            securityStreamReader.next();
        }
        xmlStreamReader.close();
        inputStream.close();
        
        // Check that what we were expecting to be signed was actually signed
        List<SignedElementSecurityEvent> signedElementEvents =
            eventListener.getSecurityEvents(SecurityEventConstants.SignedElement);
        Assert.assertNotNull(signedElementEvents);
        
        for (QName nameToSign : namesToSign) {
            boolean found = false;
            for (SignedElementSecurityEvent signedElement : signedElementEvents) {
                if (signedElement.isSigned()
                    && nameToSign.equals(getSignedQName(signedElement.getElementPath()))) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
        
        // Check Signing cert
        X509TokenSecurityEvent tokenEvent =
            (X509TokenSecurityEvent)eventListener.getSecurityEvent(SecurityEventConstants.X509Token);
        Assert.assertNotNull(tokenEvent);
        
        Assert.assertTrue(tokenEvent.getSecurityToken() instanceof X509SecurityToken);
        X509SecurityToken x509SecurityToken = (X509SecurityToken)tokenEvent.getSecurityToken();
        Assert.assertEquals(x509SecurityToken.getX509Certificates()[0], cert);
    }
    
    private static QName getSignedQName(List<QName> qnames) {
        if (qnames == null || qnames.isEmpty()) {
            return null;
        }
        
        return qnames.get(qnames.size() - 1);
    }
}
