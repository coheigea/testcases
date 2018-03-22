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

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignatureProperties;
import javax.xml.crypto.dsig.SignatureProperty;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.xml.security.Init;
import org.apache.xml.security.utils.XMLUtils;
import org.junit.Assert;

/**
 * This tests using JSR-105 API for XML Signature to create an enveloped Signature.
 */
public class SignatureJSR105EnvelopedTest extends org.junit.Assert {

	static {
		Init.init();
	}

    // Sign + Verify an XML Document using the JSR-105 API
    @org.junit.Test
    public void testSignatureUsingJSR105() throws Exception {

        // Read in plaintext document
        InputStream sourceDocument =
                this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");
        DocumentBuilder builder = XMLUtils.createDocumentBuilder(false);
        Document document = builder.parse(sourceDocument);

        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("clientstore.jks").openStream(),
            "cspass".toCharArray()
        );
        Key key = keyStore.getKey("myclientkey", "ckpass".toCharArray());
        X509Certificate cert = (X509Certificate)keyStore.getCertificate("myclientkey");

        String signatureId = "_" + UUID.randomUUID().toString();
        String signaturePropertyId = "_" + UUID.randomUUID().toString();

        // Sign using DOM
        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
        CanonicalizationMethod c14nMethod =
            signatureFactory.newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", (C14NMethodParameterSpec)null);

        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

        SignatureMethod signatureMethod =
                signatureFactory.newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);

        Transform transform =
        		signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec)null);

        DigestMethod digestMethod = signatureFactory.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
        Reference reference =
        		signatureFactory.newReference(
        				"",
        				digestMethod,
        				Collections.singletonList(transform),
        				null,
        				null);
        Transform objectTransform =
            signatureFactory.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec)null);

        Reference objectReference =
            signatureFactory.newReference(
                            "#" + signaturePropertyId,
                            digestMethod,
                            Collections.singletonList(objectTransform),
                            "http://www.w3.org/2000/09/xmldsig#SignatureProperties",
                            null);
        List<Reference> references = new ArrayList<>();
        references.add(reference);
        references.add(objectReference);
        SignedInfo signedInfo =
        		signatureFactory.newSignedInfo(c14nMethod, signatureMethod, references);

        // Add a SignatureProperty containing a Timestamp
        Element timestamp = document.createElementNS(null, "Timestamp");
        timestamp.setTextContent(new Date().toString());
        XMLStructure content = new DOMStructure(timestamp);
        SignatureProperty signatureProperty =
            signatureFactory.newSignatureProperty(Collections.singletonList(content), "#" + signatureId, signaturePropertyId);
        SignatureProperties signatureProperties =
            signatureFactory.newSignatureProperties(Collections.singletonList(signatureProperty), null);
        XMLObject object = signatureFactory.newXMLObject(Collections.singletonList(signatureProperties), null, null, null);

        XMLSignature sig = signatureFactory.newXMLSignature(
        		signedInfo,
        		keyInfo,
        		Collections.singletonList(object),
        		signatureId,
        		null);

        XMLSignContext signContext = new DOMSignContext(key, document.getDocumentElement());
        sig.sign(signContext);

        // XMLUtils.outputDOM(document, System.out);

        // Verify using JSR-105

        // Find the Signature Element
        Element sigElement = SignatureUtils.getSignatureElement(document);
        Assert.assertNotNull(sigElement);

        XMLValidateContext context = new DOMValidateContext(cert.getPublicKey(), sigElement);
        context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
        context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

        signatureFactory = XMLSignatureFactory.getInstance("DOM");
        XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(context);

        // Check the Signature value
        Assert.assertTrue(xmlSignature.validate(context));

        // First find the Timestamp
        SignatureProperty timestampSignatureProperty = getTimestampSignatureProperty(xmlSignature);
        assertNotNull(timestampSignatureProperty);

        // Check that what was signed is what we expected to be signed.
        boolean foundEnvelopedSig = false;
        boolean foundSignedTimestamp = false;
        for (Object refObject : signedInfo.getReferences()) {
            Reference ref = (Reference)refObject;
            if ("".equals(ref.getURI())) {
                List<Transform> transforms = (List<Transform>)ref.getTransforms();
                if (transforms != null && transforms.stream().anyMatch(t -> t.getAlgorithm().equals(Transform.ENVELOPED))) {
                    foundEnvelopedSig = true;
                }
            } else if ("http://www.w3.org/2000/09/xmldsig#SignatureProperties".equals(ref.getType())
                && ref.getURI().equals("#" + timestampSignatureProperty.getId())) {
                // Found matching SignatureProperties Object
                // Now validate Timestamp
                validateTimestamp(signatureProperty, cert);
                foundSignedTimestamp = true;
            }
        }
        assertEquals(sigElement.getParentNode(), document.getDocumentElement());
        assertTrue(foundEnvelopedSig);
        assertTrue(foundSignedTimestamp);
    }


    private SignatureProperty getTimestampSignatureProperty(XMLSignature xmlSignature) {
        Iterator<?> objects = xmlSignature.getObjects().iterator();
        while (objects.hasNext()) {
            XMLObject object = (XMLObject)objects.next();
            for (Object objectContent : object.getContent()) {
                if (objectContent instanceof SignatureProperties) {
                    for (Object signaturePropertiesObject : ((SignatureProperties)objectContent).getProperties()) {
                        SignatureProperty signatureProperty = (SignatureProperty)signaturePropertiesObject;
                        if (("#" + xmlSignature.getId()).equals(signatureProperty.getTarget())) {
                            return signatureProperty;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void validateTimestamp(SignatureProperty signatureProperty, X509Certificate signingCert)
        throws CertificateExpiredException, CertificateNotYetValidException {
        boolean foundValidTimestamp = false;
        for (Object xmlStructure : signatureProperty.getContent()) {
            DOMStructure domStructure = (DOMStructure)xmlStructure;
            if (domStructure.getNode() != null && "Timestamp".equals(domStructure.getNode().getNodeName())) {
                String timestampVal = domStructure.getNode().getTextContent();
                signingCert.checkValidity(new Date(timestampVal));
                foundValidTimestamp = true;
            }
        }
        assertTrue(foundValidTimestamp);
    }
}
