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
package org.apache.coheigea.misc.jarsigner;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jdk.security.jarsigner.JarSigner;

/**
 * A test to read in a file, create a zip file with it, and then sign it using the Java 9 JarSigner API
 */
public class JarSignerTest extends org.junit.Assert {

    @org.junit.Test
    public void testJarSigner() throws Exception {

        // Read in plaintext document
        InputStream sourceDocument =
                this.getClass().getClassLoader().getResourceAsStream("plaintext.xml");

        // Create a zip file and add the plaintext.xml content to it
        try (FileOutputStream fout = new FileOutputStream("target/plaintext.zip");
            ZipOutputStream zout = new ZipOutputStream(fout)) {
            ZipEntry ze = new ZipEntry("plaintext.xml");
            zout.putNextEntry(ze);
            sourceDocument.transferTo(zout);
            zout.closeEntry();
        }

        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("clientstore.jks").openStream(),
            "cspass".toCharArray()
        );

        KeyStore.ProtectionParameter protParam =
            new KeyStore.PasswordProtection("ckpass".toCharArray());
        KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry)
            keyStore.getEntry("myclientkey", protParam);

        // Now sign the zip file
        JarSigner jarsigner = new JarSigner.Builder(pkEntry).build();
        try (ZipFile in = new ZipFile("target/plaintext.zip");
            FileOutputStream out = new FileOutputStream("target/signed.zip")) {
            jarsigner.sign(in, out);
        }

    }

}
