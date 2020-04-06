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
package org.apache.coheigea.serializer.performance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.ws.security.wss4j.StaxSerializer;
import org.apache.xml.security.encryption.DocumentSerializer;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.OutboundXMLSec;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.utils.XMLUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Document;

/**
 */
public abstract class AbstractPerformanceTest {

    private static Key encryptionSymKey;
    protected XMLInputFactory xmlInputFactory;
    protected Key key;
    protected X509Certificate cert;
    private OutboundXMLSec outboundEncryptionXMLSec;
    private InboundXMLSec inboundDecryptionXMLSec;

    @BeforeAll
    public static void genKey() throws Exception {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256);
        encryptionSymKey = keygen.generateKey();
    }

    @BeforeEach
    public void setUp() throws Exception {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        org.apache.xml.security.Init.init();

        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
                this.getClass().getClassLoader().getResource("transmitter.jks").openStream(),
                "default".toCharArray()
        );
        key = keyStore.getKey("transmitter", "default".toCharArray());
        cert = (X509Certificate) keyStore.getCertificate("transmitter");

        setUpOutboundEncryptionXMLSec();
        setUpInboundEncryptionXMLSec();
    }

    protected File generateLargeXMLFile(int factor) throws Exception {
        File path = getTmpFilePath();
        path.mkdirs();
        File target = new File(path, "tmp.xml");
        FileWriter fileWriter = new FileWriter(target, false);
        fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<test xmlns=\"http://www.example.com\">");
        fileWriter.close();
        FileOutputStream fileOutputStream = new FileOutputStream(target, true);
        for (int i = 0; i < factor; i++) {
            int read = 0;
            byte[] buffer = new byte[4096];
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(
                    "plaintext.xml");
            while ((read = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }
            inputStream.close();
        }
        fileWriter = new FileWriter(target, true);
        fileWriter.write("</test>");
        fileWriter.close();

        fileOutputStream.close();

        return target;
    }

    protected int countXMLStartTags(File file) throws Exception {
        int i = 0;
        FileInputStream fileInputStream = new FileInputStream(file);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(fileInputStream);
        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next();
            if (XMLStreamConstants.START_ELEMENT == xmlStreamReader.getEventType()) {
                i++;
            }
        }
        xmlStreamReader.close();
        fileInputStream.close();
        return i;
    }

    protected abstract File getTmpFilePath();

    protected void setUpOutboundEncryptionXMLSec() throws XMLSecurityException {
        XMLSecurityProperties xmlSecurityProperties = new XMLSecurityProperties();
        List<XMLSecurityConstants.Action> actions = new ArrayList<>();
        actions.add(XMLSecurityConstants.ENCRYPT);
        xmlSecurityProperties.setActions(actions);
        xmlSecurityProperties.setEncryptionKey(encryptionSymKey);
        xmlSecurityProperties.setEncryptionSymAlgorithm("http://www.w3.org/2001/04/xmlenc#aes256-cbc");

        SecurePart securePart = new SecurePart(
                new QName("http://www.example.com", "test"),
                SecurePart.Modifier.Element
        );
        xmlSecurityProperties.addEncryptionPart(securePart);

        outboundEncryptionXMLSec = XMLSec.getOutboundXMLSec(xmlSecurityProperties);
    }

    protected void setUpInboundEncryptionXMLSec() throws XMLSecurityException {
        XMLSecurityProperties inboundProperties = new XMLSecurityProperties();
        inboundProperties.setDecryptionKey(encryptionSymKey);
        inboundDecryptionXMLSec = XMLSec.getInboundWSSec(inboundProperties);
    }

    protected File doStreamingEncryptionOutbound(File file, int tagCount) throws Exception {

        final File signedFile = new File(getTmpFilePath(), "encryption-stax-" + tagCount + ".xml");
        OutputStream outputStream = new FileOutputStream(signedFile);
        XMLStreamWriter xmlStreamWriter = outboundEncryptionXMLSec.processOutMessage(outputStream, StandardCharsets.UTF_8.name());

        InputStream inputStream = new FileInputStream(file);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);

        XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
        xmlStreamWriter.close();
        outputStream.close();
        xmlStreamReader.close();
        inputStream.close();
        return signedFile;
    }

    protected void doStreamingDecryptionInbound(File file, int tagCount) throws Exception {

        InputStream inputStream = new FileInputStream(file);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);
        XMLStreamReader securityStreamReader = inboundDecryptionXMLSec.processInMessage(xmlStreamReader);

        while (securityStreamReader.hasNext()) {
            securityStreamReader.next();
        }
        xmlStreamReader.close();
        inputStream.close();
        securityStreamReader.close();
    }

    protected void doDOMEncryptionOutbound(File file, int tagCount) throws Exception {

        Document document = XMLUtils.read(new FileInputStream(file), false);

        XMLCipher cipher = getXMLCipher();
        cipher.init(XMLCipher.ENCRYPT_MODE, encryptionSymKey);
        document = cipher.doFinal(document, document.getDocumentElement());

        XMLUtils.outputDOM(document, new BufferedOutputStream(new FileOutputStream(new File(getTmpFilePath(), "encryption-dom-" + tagCount + ".xml"))));
    }

    protected void doDOMDecryptionInbound(File file, int tagCount) throws Exception {

        Document document = XMLUtils.read(new FileInputStream(file), false);

        XMLCipher cipher = getXMLCipher();
        cipher.init(XMLCipher.DECRYPT_MODE, encryptionSymKey);
        cipher.doFinal(document, document.getDocumentElement());
    }

    private XMLCipher getXMLCipher() throws Exception {
        // return XMLCipher.getInstance("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        // return XMLCipher.getInstance("http://www.w3.org/2001/04/xmlenc#aes256-cbc", new StaxSerializer());
        return XMLCipher.getInstance("http://www.w3.org/2001/04/xmlenc#aes256-cbc", new DocumentSerializer(true));
    }

}
