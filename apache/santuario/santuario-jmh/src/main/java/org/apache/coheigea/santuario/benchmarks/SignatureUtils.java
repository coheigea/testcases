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
package org.apache.coheigea.santuario.benchmarks;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.apache.xml.security.Init;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
     * Sign the (enveloped) document using the JSR-105 API, which is included in the JDK
     */
    public static void signUsingJSR105(
        Document document,
        String algorithm,
        Key signingKey,
        X509Certificate signingCert
    ) throws Exception {
        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
        CanonicalizationMethod c14nMethod =
            signatureFactory.newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", (C14NMethodParameterSpec)null);

        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(signingCert));
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

        List<javax.xml.crypto.dsig.Reference> referenceList = new ArrayList<>();
        Element elementToSign = document.getDocumentElement();
        String id = UUID.randomUUID().toString();
        elementToSign.setAttributeNS(null, "Id", id);
        elementToSign.setIdAttributeNS(null, "Id", true);

        javax.xml.crypto.dsig.Transform envelopedTransform =
        		signatureFactory.newTransform(
        				"http://www.w3.org/2000/09/xmldsig#enveloped-signature", (TransformParameterSpec)null
        				);

        DigestMethod digestMethod = signatureFactory.newDigestMethod("http://www.w3.org/2000/09/xmldsig#sha1", null);
        javax.xml.crypto.dsig.Reference reference =
        		signatureFactory.newReference(
        				"#" + id,
        				digestMethod,
        				Arrays.asList(envelopedTransform),
        				null,
        				null
        				);
        referenceList.add(reference);

        SignatureMethod signatureMethod =
        		signatureFactory.newSignatureMethod(algorithm, null);
        SignedInfo signedInfo =
            signatureFactory.newSignedInfo(c14nMethod, signatureMethod, referenceList);

        javax.xml.crypto.dsig.XMLSignature sig = signatureFactory.newXMLSignature(
                signedInfo,
                keyInfo,
                null,
                null,
                null);

        XMLSignContext signContext = new DOMSignContext(signingKey, document.getDocumentElement());
        sig.sign(signContext);
    }

    /**
     * Verify the (enveloped) document using the JSR-105 API, which is included in the JDK
     */
    public static void verifyUsingJSR105(
        Document document,
        X509Certificate cert
    ) throws Exception {
        // Find the Signature Element
        Element sigElement = 
        		(Element)document.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", 
        				"Signature").item(0);
        document.getDocumentElement().setIdAttributeNS(null, "Id", true);

        XMLValidateContext context = new DOMValidateContext(cert.getPublicKey(), sigElement);
        context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
        context.setProperty("org.apache.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

        XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
        javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(context);

        // Check the Signature value
        if (!xmlSignature.validate(context)) {
        	throw new Exception("Failure to validate the signature");
        }
    }

}
