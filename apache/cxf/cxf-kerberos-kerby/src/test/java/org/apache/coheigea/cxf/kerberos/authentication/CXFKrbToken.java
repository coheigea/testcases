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
package org.apache.coheigea.cxf.kerberos.authentication;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.kerby.kerberos.kerb.type.base.AuthToken;
import org.apache.kerby.kerberos.kerb.type.base.KrbTokenBase;
import org.apache.kerby.kerberos.kerb.type.base.TokenFormat;
import org.apache.wss4j.common.util.Loader;

/**
 * We need a custom implementation of AuthToken to wrap the JWT token returned by CXF
 */
public class CXFKrbToken extends KrbTokenBase implements AuthToken {
    
    private JwtClaims claims;
    private boolean idToken;
    
    public CXFKrbToken(JwtClaims claims, boolean idToken) {
        this.claims = claims;
        this.idToken = idToken;
        setTokenFormat(TokenFormat.JWT);
    }
    
    public void sign() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(Loader.getResourceAsStream("clientstore.jks"), "cspass".toCharArray());
        
        Properties signingProperties = new Properties();
        signingProperties.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, SignatureAlgorithm.RS256.name());
        signingProperties.put(JoseConstants.RSSEC_KEY_STORE, keystore);
        signingProperties.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "myclientkey");
        signingProperties.put(JoseConstants.RSSEC_KEY_PSWD, "ckpass");

        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);

        JwsSignatureProvider sigProvider =
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);

        String signedToken = jws.signWith(sigProvider);
        
        setTokenValue(signedToken.getBytes());
    }

    @Override
    public String getSubject() {
        return claims.getSubject();
    }

    @Override
    public void setSubject(String sub) {
        claims.setSubject(sub);
    }

    @Override
    public String getIssuer() {
        return claims.getIssuer();
    }

    @Override
    public void setIssuer(String issuer) {
        claims.setIssuer(issuer);
    }

    @Override
    public List<String> getAudiences() {
        return claims.getAudiences();
    }

    @Override
    public void setAudiences(List<String> audiences) {
        claims.setAudiences(audiences);
    }

    @Override
    public boolean isIdToken() {
        return idToken;
    }

    @Override
    public void isIdToken(boolean isIdToken) {
        this.idToken = isIdToken;
    }

    @Override
    public boolean isAcToken() {
        return !idToken;
    }

    @Override
    public void isAcToken(boolean isAcToken) {
        idToken = !isAcToken;
    }

    @Override
    public boolean isBearerToken() {
        return true;
    }

    @Override
    public boolean isHolderOfKeyToken() {
        return false;
    }

    @Override
    public Date getExpiredTime() {
        return new Date(claims.getExpiryTime());
    }

    @Override
    public void setExpirationTime(Date exp) {
        claims.setExpiryTime(exp.getTime());
    }

    @Override
    public Date getNotBeforeTime() {
        return new Date(claims.getNotBefore());
    }

    @Override
    public void setNotBeforeTime(Date nbt) {
        claims.setNotBefore(nbt.getTime());
    }

    @Override
    public Date getIssueTime() {
        return new Date(claims.getIssuedAt());
    }

    @Override
    public void setIssueTime(Date iat) {
        claims.setIssuedAt(iat.getTime());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return claims.asMap();
    }

    @Override
    public void addAttribute(String name, Object value) {
        claims.setProperty(name, value);
    }

}
