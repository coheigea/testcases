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
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.xml.security.utils.XMLUtils;

/**
 * This tests using the DOM API of Apache Santuario - XML Security for Java for XML Signature.
 */
public class SignatureDOMTest extends org.junit.Assert {
    
    // Sign + Verify an XML Document using the DOM API
    @org.junit.Test
    public void testSignatureUsingDOMAPI() throws Exception {
        
        // Read in plaintext document
        InputStream sourceDocument = 
                this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");
        Document document = XMLUtils.read(sourceDocument, true);
        
        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("clientstore.jks").openStream(), 
            "cspass".toCharArray()
        );
        Key key = keyStore.getKey("myclientkey", "ckpass".toCharArray());
        X509Certificate cert = (X509Certificate)keyStore.getCertificate("myclientkey");
        
        // Sign using DOM
        List<QName> namesToSign = new ArrayList<QName>();
        namesToSign.add(new QName("urn:example:po", "PaymentInfo"));
        SignatureUtils.signUsingDOM(
            document, namesToSign, "http://www.w3.org/2000/09/xmldsig#rsa-sha1", key, cert
        );
        
        XMLUtils.outputDOM(document, System.out);
        
        // Verify using DOM
        SignatureUtils.verifyUsingDOM(document, namesToSign, cert);
    }
    
    
}
