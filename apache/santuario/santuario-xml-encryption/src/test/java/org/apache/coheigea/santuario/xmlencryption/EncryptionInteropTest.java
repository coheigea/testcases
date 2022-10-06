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
package org.apache.coheigea.santuario.xmlencryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.utils.XMLUtils;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Some interop tests between the different XML Encryption stacks.
 */
public class EncryptionInteropTest extends org.junit.Assert {
    
    // Encrypt using StAX and decrypt using DOM
    @org.junit.Test
    public void testEncryptionInteropStAXDOMTest() throws Exception {
        
        // Read in plaintext document
        InputStream sourceDocument = 
                this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");
        
        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("servicestore.jks").openStream(), 
            "sspass".toCharArray()
        );
        X509Certificate cert = (X509Certificate)keyStore.getCertificate("myservicekey");
        
        // Set up the secret Key
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        SecretKey secretKey = keygen.generateKey();
        
        // Encrypt using StAX
        List<QName> namesToEncrypt = new ArrayList<QName>();
        namesToEncrypt.add(new QName("urn:example:po", "PaymentInfo"));
        ByteArrayOutputStream baos = EncryptionUtils.encryptUsingStAX(
            sourceDocument, namesToEncrypt, "http://www.w3.org/2001/04/xmlenc#aes128-cbc", secretKey,
            "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", cert.getPublicKey(), false
        );
        
        // Decrypt using DOM
        Document document = XMLUtils.read(new ByteArrayInputStream(baos.toByteArray()), true);
        XMLUtils.outputDOM(document, System.out);
        
        // Check the CreditCard encrypted ok
        NodeList nodeList = document.getElementsByTagNameNS("urn:example:po", "CreditCard");
        Assert.assertEquals(nodeList.getLength(), 0);
        
        Key privateKey = keyStore.getKey("myservicekey", "skpass".toCharArray());
        EncryptionUtils.decryptUsingDOM(document, 
            "http://www.w3.org/2001/04/xmlenc#aes128-cbc", privateKey);
        
        // Check the CreditCard decrypted ok
        nodeList = document.getElementsByTagNameNS("urn:example:po", "CreditCard");
        Assert.assertEquals(nodeList.getLength(), 1);
    }
    
    // Encrypt using DOM and decrypt using StAX
    @org.junit.Test
    public void testEncryptionInteropDOMStAXTest() throws Exception {
        
        // Read in plaintext document
        InputStream sourceDocument = 
                this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");
        Document document = XMLUtils.read(sourceDocument, true);
        
        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("servicestore.jks").openStream(), 
            "sspass".toCharArray()
        );
        X509Certificate cert = (X509Certificate)keyStore.getCertificate("myservicekey");
        
        // Set up the secret Key
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        SecretKey secretKey = keygen.generateKey();
        
        // Encrypt using DOM
        List<QName> namesToEncrypt = new ArrayList<QName>();
        namesToEncrypt.add(new QName("urn:example:po", "PaymentInfo"));
        EncryptionUtils.encryptUsingDOM(
            document, namesToEncrypt, "http://www.w3.org/2001/04/xmlenc#aes128-cbc", secretKey,
            "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", cert.getPublicKey(), false
        );
        
        XMLUtils.outputDOM(document, System.out);
        
        // Check the CreditCard encrypted ok
        NodeList nodeList = document.getElementsByTagNameNS("urn:example:po", "CreditCard");
        Assert.assertEquals(nodeList.getLength(), 0);
        
        // Decrypt using StAX
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(baos));
        
        Key privateKey = keyStore.getKey("myservicekey", "skpass".toCharArray());
        EncryptionUtils.decryptUsingStAX(
            new ByteArrayInputStream(baos.toByteArray()), namesToEncrypt, privateKey);
    }
    
    
}
