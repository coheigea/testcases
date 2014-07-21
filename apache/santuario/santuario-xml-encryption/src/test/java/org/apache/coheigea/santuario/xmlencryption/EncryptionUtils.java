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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.securityEvent.EncryptedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Some utility methods for encrypting/decrypting documents
 */
public final class EncryptionUtils {
    
    static {
        Init.init();
    }
    
    private EncryptionUtils() {
        // complete
    }
    
    /**
     * Encrypt the document using the DOM API of Apache Santuario - XML Security for Java.
     * It encrypts a list of QNames that it finds in the Document via XPath. If a wrappingKey
     * is supplied, this is used to encrypt the encryptingKey + place it in an EncryptedKey
     * structure.
     */
    public static void encryptUsingDOM(
        Document document,
        List<QName> namesToEncrypt,
        String algorithm,
        Key encryptingKey,
        String keyTransportAlgorithm,
        PublicKey wrappingKey,
        boolean content
    ) throws Exception {
        XMLCipher cipher = XMLCipher.getInstance(algorithm);
        cipher.init(XMLCipher.ENCRYPT_MODE, encryptingKey);
        
        if (wrappingKey != null) {
            XMLCipher newCipher = XMLCipher.getInstance(keyTransportAlgorithm);
            newCipher.init(XMLCipher.WRAP_MODE, wrappingKey);
            
            EncryptedKey encryptedKey = newCipher.encryptKey(document, encryptingKey);
            // Create a KeyInfo for the EncryptedKey
            KeyInfo encryptedKeyKeyInfo = encryptedKey.getKeyInfo();
            if (encryptedKeyKeyInfo == null) {
                encryptedKeyKeyInfo = new KeyInfo(document);
                encryptedKeyKeyInfo.getElement().setAttributeNS(
                    "http://www.w3.org/2000/xmlns/", "xmlns:dsig", "http://www.w3.org/2000/09/xmldsig#"
                );
                encryptedKey.setKeyInfo(encryptedKeyKeyInfo);
            }
            encryptedKeyKeyInfo.add(wrappingKey);
            
            // Create a KeyInfo for the EncryptedData
            EncryptedData builder = cipher.getEncryptedData();
            KeyInfo builderKeyInfo = builder.getKeyInfo();
            if (builderKeyInfo == null) {
                builderKeyInfo = new KeyInfo(document);
                builderKeyInfo.getElement().setAttributeNS(
                    "http://www.w3.org/2000/xmlns/", "xmlns:dsig", "http://www.w3.org/2000/09/xmldsig#"
                );
                builder.setKeyInfo(builderKeyInfo);
            }

            builderKeyInfo.add(encryptedKey);
        }
        
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        for (QName nameToEncrypt : namesToEncrypt) {
            String expression = "//*[local-name()='" + nameToEncrypt.getLocalPart() + "']";
            NodeList elementsToEncrypt =
                    (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
            for (int i = 0; i < elementsToEncrypt.getLength(); i++) {
                Element elementToEncrypt = (Element)elementsToEncrypt.item(i);
                Assert.assertNotNull(elementToEncrypt);
                document = cipher.doFinal(document, elementToEncrypt, content);
            }
        }

        String expression = "//xenc:EncryptedData[1]";
        Element encElement =
                (Element) xpath.evaluate(expression, document, XPathConstants.NODE);
        Assert.assertNotNull(encElement);
    }
    
    /**
     * Decrypt the document using the DOM API of Apache Santuario - XML Security for Java.
     */
    public static void decryptUsingDOM(
        Document document,
        String algorithm,
        Key privateKey
    ) throws Exception {
        XMLCipher cipher = XMLCipher.getInstance(algorithm);
        cipher.init(XMLCipher.DECRYPT_MODE, null);
        cipher.setKEK(privateKey);
        
        NodeList nodeList = document.getElementsByTagNameNS(
                XMLSecurityConstants.TAG_xenc_EncryptedData.getNamespaceURI(),
                XMLSecurityConstants.TAG_xenc_EncryptedData.getLocalPart()
            );
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element ee = (Element)nodeList.item(i);
            cipher.doFinal(document, ee);
        }
    }

    /**
     * Encrypt the document using the StAX API of Apache Santuario - XML Security for Java. If 
     * a wrappingKey is supplied, this is used to encrypt the encryptingKey + place it in an 
     * EncryptedKey structure.
     */
    public static ByteArrayOutputStream encryptUsingStAX(
        InputStream inputStream,
        List<QName> namesToEncrypt,
        String algorithm,
        Key encryptingKey,
        String keyTransportAlgorithm,
        PublicKey wrappingKey,
        boolean content
    ) throws Exception {
        // Set up the Configuration
        XMLSecurityProperties properties = new XMLSecurityProperties();
        List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
        actions.add(XMLSecurityConstants.ENCRYPT);
        properties.setActions(actions);
        
        properties.setEncryptionSymAlgorithm(algorithm);
        properties.setEncryptionKey(encryptingKey);
        properties.setEncryptionKeyTransportAlgorithm(keyTransportAlgorithm);
        properties.setEncryptionTransportKey(wrappingKey);
        properties.setEncryptionKeyIdentifier(
                SecurityTokenConstants.KeyIdentifier_X509KeyIdentifier);
        
        SecurePart.Modifier modifier = SecurePart.Modifier.Content;
        if (!content) {
            modifier = SecurePart.Modifier.Element;
        }
        for (QName nameToEncrypt : namesToEncrypt) {
            SecurePart securePart = new SecurePart(nameToEncrypt, modifier);
            properties.addEncryptionPart(securePart);
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
     * Decrypt the document using the StAX API of Apache Santuario - XML Security for Java.
     */
    public static void decryptUsingStAX(
        InputStream inputStream,
        List<QName> namesToEncrypt,
        Key privateKey
    ) throws Exception {
        // Set up the Configuration
        XMLSecurityProperties properties = new XMLSecurityProperties();
        List<XMLSecurityConstants.Action> actions = new ArrayList<XMLSecurityConstants.Action>();
        actions.add(XMLSecurityConstants.ENCRYPT);
        properties.setActions(actions);
        
        properties.setDecryptionKey(privateKey);
        
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
        
        // Check that what we were expecting to be encrypted was actually encrypted
        List<EncryptedElementSecurityEvent> encryptedElementEvents =
            eventListener.getSecurityEvents(SecurityEventConstants.EncryptedElement);
        Assert.assertNotNull(encryptedElementEvents);
        
        for (QName nameToEncrypt : namesToEncrypt) {
            boolean found = false;
            for (EncryptedElementSecurityEvent encryptedElement : encryptedElementEvents) {
                if (encryptedElement.isEncrypted()
                    && nameToEncrypt.equals(getEncryptedQName(encryptedElement.getElementPath()))) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
    }
    
    private static QName getEncryptedQName(List<QName> qnames) {
        if (qnames == null || qnames.isEmpty()) {
            return null;
        }
        
        return qnames.get(qnames.size() - 1);
    }
}
