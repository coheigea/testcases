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
package org.apache.coheigea.cxf.timing.benchmark.serializers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.encryption.Serializer;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.XMLUtils;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author $Author: $
 * @version $Revision: $ $Date: $
 */
public abstract class AbstractPerformanceTest {

    private static Key encryptionSymKey;
    protected XMLInputFactory xmlInputFactory;
    protected Key key;
    protected X509Certificate cert;

    @BeforeClass
    public static void genKey() throws Exception {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256);
        encryptionSymKey = keygen.generateKey();
    }

    @Before
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
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");
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
            switch (xmlStreamReader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    i++;
                    break;
            }
        }
        xmlStreamReader.close();
        fileInputStream.close();
        return i;
    }

    protected abstract File getTmpFilePath();

    protected File doDOMEncryptionOutbound(File file, int tagCount, Serializer serializer) throws Exception {

        Document document = StaxUtils.read(file);

        XMLCipher cipher = XMLCipher.getInstance("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        cipher.init(XMLCipher.ENCRYPT_MODE, encryptionSymKey);
        cipher.setSerializer(serializer);
        document = cipher.doFinal(document, document.getDocumentElement());
        
        final File encryptedFile = new File(getTmpFilePath(), "encryption-dom-" + serializer.toString() + "-" + tagCount + ".xml");
        OutputStream outputStream = new FileOutputStream(encryptedFile);

        XMLUtils.outputDOM(document, new BufferedOutputStream(outputStream));
        outputStream.close();
        
        return encryptedFile;
    }

    protected void doDOMDecryptionInbound(File file, int tagCount, Serializer serializer) throws Exception {

        Document document = StaxUtils.read(file);

        XMLCipher cipher = XMLCipher.getInstance("http://www.w3.org/2001/04/xmlenc#aes256-cbc");
        cipher.init(XMLCipher.DECRYPT_MODE, encryptionSymKey);
        cipher.setSerializer(serializer);
        cipher.doFinal(document, document.getDocumentElement());
    }
}
