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
package org.apache.coheigea.cxf.kerberos.jwtjaxrs;

import java.io.File;
import java.net.URI;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.kerby.kerberos.kerb.KrbRuntime;
import org.apache.kerby.kerberos.kerb.ccache.Credential;
import org.apache.kerby.kerberos.kerb.ccache.CredentialCache;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.client.KrbTokenClient;
import org.apache.kerby.kerberos.kerb.type.base.AuthToken;
import org.apache.kerby.kerberos.kerb.type.base.KrbToken;
import org.apache.kerby.kerberos.kerb.type.base.TokenFormat;
import org.apache.kerby.kerberos.kerb.type.ticket.SgtTicket;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.apache.kerby.kerberos.provider.token.JwtAuthToken;
import org.apache.kerby.kerberos.provider.token.JwtTokenProvider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

/**
 * A Custom HttpAuthSupplier implementation that uses the Kerby API and passes through a received JWT Token
 * when getting a SGT.
 */
public class KerbyHttpAuthSupplier implements HttpAuthSupplier {
    
    private int kdcPort;
    private String kdcRealm;
    private String jwtToken;

    @Override
    public String getAuthorization(AuthorizationPolicy arg0, URI arg1, Message arg2, String arg3) {
        try {
            // Get a TGT
            KrbClient client = new KrbClient();
    
            client.setKdcHost("localhost");
            client.setKdcTcpPort(kdcPort);
            client.setAllowUdp(false);
    
            client.setKdcRealm(kdcRealm);
            client.init();
    
            TgtTicket tgt = client.requestTgt("alice@service.ws.apache.org", "alice");
    
            // Write to cache
            Credential credential = new Credential(tgt);
            CredentialCache cCache = new CredentialCache();
            cCache.addCredential(credential);
            cCache.setPrimaryPrincipal(tgt.getClientPrincipal());
    
            File cCacheFile = File.createTempFile("krb5_alice@service.ws.apache.org", "cc");
            cCache.store(cCacheFile);
    
            KrbTokenClient tokenClient = new KrbTokenClient(client);
    
            tokenClient.setKdcHost("localhost");
            tokenClient.setKdcTcpPort(kdcPort);
            tokenClient.setAllowUdp(false);
    
            tokenClient.setKdcRealm(kdcRealm);
            tokenClient.init();

            // Parse JWT token into a format we can use with Kerby
            KrbRuntime.setTokenProvider(new JwtTokenProvider());
            JWT jwt = JWTParser.parse(jwtToken);
            AuthToken authToken = new JwtAuthToken(jwt.getJWTClaimsSet());
            
            KrbToken krbToken = new KrbToken(authToken, TokenFormat.JWT);
            krbToken.setTokenValue(jwtToken.getBytes());
    
            // Now get a SGT using the JWT
            SgtTicket tkt = tokenClient.requestSgt(krbToken, "bob/service.ws.apache.org@service.ws.apache.org", cCacheFile.getPath());
    
            cCacheFile.delete();
            
            return HttpAuthHeader.AUTH_TYPE_NEGOTIATE + " " + Base64Utility.encode(tkt.getTicket().encode());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean requiresRequestCaching() {
        // TODO Auto-generated method stub
        return false;
    }

    public int getKdcPort() {
        return kdcPort;
    }

    public void setKdcPort(int kdcPort) {
        this.kdcPort = kdcPort;
    }

    public String getKdcRealm() {
        return kdcRealm;
    }

    public void setKdcRealm(String kdcRealm) {
        this.kdcRealm = kdcRealm;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

}
