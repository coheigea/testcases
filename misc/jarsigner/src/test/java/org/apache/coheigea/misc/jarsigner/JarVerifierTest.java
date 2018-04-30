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

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Some tests to read in some signed jars and verify them - including some negative tests.
 * Works with JDK 1.8.
 */
public class JarVerifierTest extends org.junit.Assert {

    @org.junit.Test
    public void testVerifyingSignedJars() throws Exception {

        // Set up the Key
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(
            this.getClass().getClassLoader().getResource("clientstore.jks").openStream(),
            "cspass".toCharArray()
        );

        Certificate[] certChain = keyStore.getCertificateChain("myclientkey");

        // Set up the trust anchor
        TrustAnchor anchor = new TrustAnchor((X509Certificate)certChain[1], null);
        PKIXParameters param = new PKIXParameters(Collections.singleton(anchor));
        param.setRevocationEnabled(false);

        // Now verify the signed zip file
        assertTrue(validateSignedZip("src/test/resources/signed.zip", param));

        // Some negative tests...

        // Here the XML inside the zip is modified
        try {
            validateSignedZip("src/test/resources/modified_file.zip", param);
            fail("Failure expected on a bad signature");
        } catch (SecurityException ex) {
            assertTrue(ex.getMessage().contains("SHA-256 digest error for plaintext.xml"));
        }

        // Here the digest corresponding to plaintext.xml is modified in the manifest
        try {
            validateSignedZip("src/test/resources/modified_manifest.zip", param);
            fail("Failure expected on a bad signature");
        } catch (SecurityException ex) {
            assertTrue(ex.getMessage().contains("invalid SHA-256 signature file digest for plaintext.xml"));
        }

        // Here the signer file is modified
        try {
            validateSignedZip("src/test/resources/modified_signer_digest.zip", param);
            fail("Failure expected on a bad signature");
        } catch (SecurityException ex) {
            assertTrue(ex.getMessage().contains("cannot verify signature block file META-INF/SIGNER"));
        }

        // Here the jar is unsigned
        assertFalse(validateSignedZip("src/test/resources/plaintext.zip", param));
    }

    private static boolean validateSignedZip(String fileName, PKIXParameters param) throws IOException, CertPathValidatorException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException {

        // Now verify the signed zip file
        try (JarFile jar = new JarFile(fileName)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Read the inputstream corresponding to the JAR entry
                try (InputStream inputStream = jar.getInputStream(entry)) {
                    byte[] bytes = new byte[4096];
                    InputStream is = jar.getInputStream(entry);
                    while ((is.read(bytes, 0, bytes.length)) != -1) {
                        // A SecurityException is thrown here if the digest is incorrect.
                    }
                }

                // SIGNER.SF + SIGNER.RSA don't have code signers  - verify every other file is signed
                // Note - might be better to match the exact names here instead of doing a reg-ex
                if (!(entry.getName().matches("META-INF/.*.SF") || entry.getName().matches("META-INF/.*.RSA"))) {

                    // We need to call "getCodeSigners()" *after* the input stream has been fully read
                    if (entry.getCodeSigners() == null || entry.getCodeSigners().length == 0) {
                        // The entry was not signed
                        return false;
                    }

                    // Verify the CertPath
                    for (CodeSigner cs : entry.getCodeSigners()) {
                        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
                        validator.validate(cs.getSignerCertPath(), param);
                    }
                }
            }
        }

        return true;
    }



}
