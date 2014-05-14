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

package org.apache.coheigea.cxf.oauth1.balanceservice;


import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth.data.AuthorizationInput;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.RequestTokenRegistration;
import org.apache.cxf.rs.security.oauth.data.Token;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;
import org.apache.ws.security.util.Base64;

/**
 * A simple implementation of CXF's OAuthDataProvider interface.
 */
public class OAuthDataProviderImpl implements OAuthDataProvider {
    
    private final OAuthPermission getBalancePermission;
    private final OAuthPermission createBalancePermission;
    private final SecureRandom random;
    
    private Map<String, Client> clients = new HashMap<String, Client>();
    private Map<String, RequestToken> requestTokens = new HashMap<String, RequestToken>();
    private Map<String, AccessToken> accessTokens = new HashMap<String, AccessToken>();
    
    public OAuthDataProviderImpl() throws Exception {
        random = SecureRandom.getInstance("SHA1PRNG");
        List<String> permissions = new ArrayList<String>();
        
        // Only customers can create new users with a given balance
        permissions.add("customer");
        createBalancePermission = 
            new OAuthPermission("create_balance", "Permission to create your balance", permissions);
        
        // Customers or partners can read a balance
        permissions = new ArrayList<String>();
        permissions.add("customer");
        permissions.add("partner");
        getBalancePermission = 
            new OAuthPermission("get_balance", "Permission to get your balance", permissions);
    }

    public AccessToken createAccessToken(AccessTokenRegistration reg) throws OAuthServiceException {
        
        // Generate request token + associated secret
        Client client = reg.getRequestToken().getClient();
        String token = UUID.randomUUID().toString();
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        
        AccessToken accessToken = 
            new AccessToken(client, token, Base64.encode(secret), 60L * 5L, 
                            new Date().getTime() / 1000L);
        accessToken.setScopes(reg.getRequestToken().getScopes());
        
        // Remove request token
        requestTokens.remove(reg.getRequestToken().getTokenKey());
        
        // Add access token
        accessTokens.put(token,  accessToken);
        
        return accessToken;
    }

    public RequestToken createRequestToken(RequestTokenRegistration reg) throws OAuthServiceException {
        
        // Generate request token + associated secret
        Client client = reg.getClient();
        String token = UUID.randomUUID().toString();
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        
        RequestToken requestToken = 
            new RequestToken(client, token, Base64.encode(secret), reg.getLifetime(), reg.getIssuedAt());
        
        // Set the permissions/scopes
        List<String> regScopes = reg.getScopes();
        List<OAuthPermission> permissions = new ArrayList<OAuthPermission>();
        for (String regScope : regScopes) {
            if (regScope.equals(getBalancePermission.getPermission())) {
                permissions.add(getBalancePermission);
            } else if (regScope.equals(createBalancePermission.getPermission())) {
                permissions.add(createBalancePermission);
            }
        }
        requestToken.setScopes(permissions);
        requestToken.setCallback(reg.getCallback());
        requestTokens.put(token, requestToken);
        
        return requestToken;
    }

    public String finalizeAuthorization(AuthorizationInput authorizationInput) throws OAuthServiceException {
        RequestToken requestToken = authorizationInput.getToken();
        String verifier = UUID.randomUUID().toString();
        requestToken.setVerifier(verifier);
        
        return verifier;
    }

    public AccessToken getAccessToken(String tokenId) throws OAuthServiceException {
        if (accessTokens.containsKey(tokenId)) {
            return accessTokens.get(tokenId);
        }
        
        return null;
    }

    public Client getClient(String clientId) throws OAuthServiceException {
        if (clients.containsKey(clientId)) {
            return clients.get(clientId);
        }
        
        return null;
    }

    public RequestToken getRequestToken(String tokenId) throws OAuthServiceException {
        if (requestTokens.containsKey(tokenId)) {
            return requestTokens.get(tokenId);
        }
        
        return null;
    }

    public void removeToken(Token token) throws OAuthServiceException {
        if (requestTokens.containsKey(token.getTokenKey())) {
            requestTokens.remove(token.getTokenKey());
        }
        
        if (accessTokens.containsKey(token.getTokenKey())) {
            accessTokens.remove(token.getTokenKey());
        }
    }
    
    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }
    
}


