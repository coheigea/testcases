/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.jwt;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;
import org.apache.cxf.rs.security.jose.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.JWTSSOProvider;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A JWT validation implementation which is used to validate tokens issued by the CXF STS.
 */
public class STSJWTSSOProvider implements JWTSSOProvider {

    public static final String ISSUER = "STSIssuer";

    private final JwsSignatureVerifier delegate;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AuthDataAccessor authDataAccessor;

    public STSJWTSSOProvider() throws Exception {
        // Load verification cert
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("sts_ver.jks", this.getClass()),
                      "stsspass".toCharArray());
        X509Certificate cert = (X509Certificate)keyStore.getCertificate("mykey");

        delegate = new PublicKeyJwsSignatureVerifier(cert, SignatureAlgorithm.RS256);
    }

    @Override
    public String getIssuer() {
        return ISSUER;
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public boolean verify(final JwsHeaders headers, final String unsignedText, final byte[] signature) {
        return delegate.verify(headers, unsignedText, signature);
    }

    @Override
    public JwsVerificationSignature createJwsVerificationSignature(final JwsHeaders headers) {
        return delegate.createJwsVerificationSignature(headers);
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<User, Set<SyncopeGrantedAuthority>> resolve(final JwtClaims jwtClaims) {

        User user = userDAO.findByUsername(jwtClaims.getSubject());
        if (user != null) {
            Set<SyncopeGrantedAuthority> authorities = authDataAccessor.getAuthorities(user.getUsername(), null);

            return Pair.of(user, authorities);
        }

        return null;
    }

}
