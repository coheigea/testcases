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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.apache.xml.security.Init;
import org.apache.xml.security.utils.XMLUtils;

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

        // Sign using DOM
        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
        CanonicalizationMethod c14nMethod =
            signatureFactory.newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", (C14NMethodParameterSpec)null);

        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

        SignatureMethod signatureMethod =
                signatureFactory.newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);

        javax.xml.crypto.dsig.Transform transform =
        		signatureFactory.newTransform(javax.xml.crypto.dsig.Transform.ENVELOPED, (TransformParameterSpec)null);

        DigestMethod digestMethod = signatureFactory.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
        javax.xml.crypto.dsig.Reference reference =
        		signatureFactory.newReference(
        				"",
        				digestMethod,
        				Collections.singletonList(transform),
        				null,
        				null);
        SignedInfo signedInfo =
        		signatureFactory.newSignedInfo(c14nMethod, signatureMethod, Collections.singletonList(reference));

        javax.xml.crypto.dsig.XMLSignature sig = signatureFactory.newXMLSignature(
        		signedInfo,
        		keyInfo,
        		null,
        		null,
        		null);

        XMLSignContext signContext = new DOMSignContext(key, document.getDocumentElement());
        sig.sign(signContext);

        // XMLUtils.outputDOM(document, System.out);

        // Verify using JSR-105
        List<QName> namesToSign = new ArrayList<QName>();
        namesToSign.add(new QName("urn:example:po", "PurchaseOrder"));
        SignatureUtils.verifyUsingJSR105(document, namesToSign, cert);
    }


}
