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
package org.apache.coheigea.cxf.oauth2.oauthservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AbstractCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

public class OAuthDataProviderImpl extends AbstractCodeDataProvider {
    
    private Map<String, Client> clients = new HashMap<>();
    private Map<String, ServerAccessToken> accessTokens = new HashMap<>();
    private Map<String, ServerAuthorizationCodeGrant> codeGrants = new HashMap<>();
    private Map<String, RefreshToken> refreshTokens = new HashMap<>();
    
    
    public OAuthDataProviderImpl() {
    }

    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        if (accessTokens.containsKey(accessToken)) {
            return accessTokens.get(accessToken);
        }
        
        return null;
    }

    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        if (clients.containsKey(clientId)) {
            return clients.get(clientId);
        }
        
        return null;
    }

    @Override
    protected boolean revokeAccessToken(String accessTokenKey) {
        if (accessTokens.containsKey(accessTokenKey)) {
            accessTokens.remove(accessTokenKey);
            return true;
        }
        return false;
    }

    @Override
    protected RefreshToken revokeRefreshToken(Client client, String refreshTokenKey) {
        if (refreshTokens.containsKey(client.getClientId())) {
            return refreshTokens.remove(client.getClientId());
        }
        return null;
    }

    @Override
    protected void saveAccessToken(ServerAccessToken serverToken) {
        if (serverToken != null) {
            accessTokens.put(serverToken.getTokenKey(), serverToken);
        }
    }

    @Override
    protected void saveRefreshToken(ServerAccessToken at, RefreshToken refreshToken) {
        Client client = at.getClient();
        refreshTokens.put(client.getClientId(), refreshToken);
    }
    
    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }

    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        if (codeGrants.containsKey(code)) {
            return codeGrants.remove(code);
        }
        return null;
    }

    @Override
    protected void saveCodeGrant(ServerAuthorizationCodeGrant grant) {
        if (grant != null) {
            codeGrants.put(grant.getCode(), grant);
        }
        
    }
    
    @Override
    protected boolean isRefreshTokenSupported(List<String> theScopes) {
        return true;
    }

}
