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

package org.apache.coheigea.cxf.crls;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignatureCRLUnitTest extends org.junit.Assert {

    @org.junit.Test
    public void testCRLRevocation() throws Exception {
        
        System.setProperty("java.security.debug", "all");
        
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        
        // Signing Cert
        InputStream certInputStream = 
            loadInputStream(this.getClass().getClassLoader(), "keys/wss40rev.jks");
        assertNotNull(certInputStream);
        KeyStore certKeyStore = KeyStore.getInstance("JKS");
        certKeyStore.load(certInputStream, "security".toCharArray());
        Certificate[] certs = certKeyStore.getCertificateChain("wss40rev");
        assertNotNull(certs);
        assertEquals(certs.length, 2);
        
        //List<Certificate> certList = Arrays.asList(certs[0]); // WORKS
        List<Certificate> certList = Arrays.asList(certs); // DOESN'T WORK!
        CertPath path = certificateFactory.generateCertPath(certList);
        
        // CA cert
        InputStream caInputStream = 
            loadInputStream(this.getClass().getClassLoader(), "keys/wss40CA.jks");
        assertNotNull(caInputStream);
        KeyStore caKeyStore = KeyStore.getInstance("JKS");
        caKeyStore.load(caInputStream, "security".toCharArray());
        X509Certificate caCert = (X509Certificate)caKeyStore.getCertificate("wss40CA");
        assertNotNull(caCert);
        
        Set<TrustAnchor> set = new HashSet<TrustAnchor>();
        TrustAnchor anchor = new TrustAnchor(caCert, null);
        set.add(anchor);
        
        // Load CRL
        InputStream crlInputStream = 
            loadInputStream(this.getClass().getClassLoader(), "keys/wss40CACRL.pem");
        assertNotNull(crlInputStream);
        X509CRL crl = (X509CRL)certificateFactory.generateCRL(crlInputStream);
        crlInputStream.close();
        assertNotNull(crl);
        
        // Construct PKIXParameters
        PKIXParameters param = new PKIXParameters(set);
        param.setRevocationEnabled(true);
        param.addCertStore(CertStore.getInstance("Collection",
            new CollectionCertStoreParameters(
                Collections.singletonList(crl))));
        
        // Validate the Cert Path
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        
        try {
            validator.validate(path, param);
            fail("Failure expected on a revoked certificate");
        } catch (CertPathValidatorException ex) {
            assertTrue(ex.getMessage().contains("revoked")
              || ex.getMessage().contains("revocation"));
        }
    }
    
    private static InputStream loadInputStream(ClassLoader loader, String location) 
        throws IOException {
        InputStream is = null;
        if (location != null) {
            is = loader.getResourceAsStream(location);
    
            //
            // If we don't find it, then look on the file system.
            //
            if (is == null) {
                is = new FileInputStream(location);
            }
        }
        return is;
    }
    
}
