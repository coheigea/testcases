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

package org.apache.coheigea.cxf.oauth1.invoiceservice;


import java.net.URL;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;

/**
 * A "Invoice" service. It takes in a user + address and then attempts to get the user's balance
 * from an OAuth protected "BalanceService". This is then returned to the user in the form of an
 * invoice.
 */
public class InvoiceService {
    private OAuthConsumer oauthConsumer;
    
    @GET
    @Path("/{user}")
    public javax.ws.rs.core.Response createInvoice(@PathParam("user") String user,
                             @QueryParam("address") String address) {
        
        if (user == null || address == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (oauthConsumer == null) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        // Get a Request Token
        Response response = makeRequestTokenInvocation();
        if (response.getStatus() != 200) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        // Extract Request Token + Secret
        String responseString = response.readEntity(String.class);
        String requestToken = 
            responseString.substring(responseString.indexOf("oauth_token="),
                                     responseString.indexOf("&oauth_token_secret"));
        requestToken = requestToken.substring(requestToken.indexOf("=") + 1);
        
        String requestTokenSecret = 
            responseString.substring(responseString.indexOf("oauth_token_secret="));
        requestTokenSecret = requestTokenSecret.substring(requestTokenSecret.indexOf("=") + 1);
        
        // Re-direct end user to the authorization service
        UriBuilder ub = UriBuilder.fromUri(oauthConsumer.getAuthorizationURL());
        ub.queryParam("oauth_token", requestToken);
        
        return javax.ws.rs.core.Response.seeOther(ub.build()).build();
    }

    public OAuthConsumer getOauthConsumer() {
        return oauthConsumer;
    }

    public void setOauthConsumer(OAuthConsumer oauthConsumer) {
        this.oauthConsumer = oauthConsumer;
    }
    
    private Response makeRequestTokenInvocation() {
        WebClient client = WebClient.create(oauthConsumer.getRequestURL());
        
        String nonce = UUID.randomUUID().toString();
        String oAuthHeader = "OAuth "
            + "oauth_consumer_key=\"" + oauthConsumer.getConsumerId() + "\", "
            + "oauth_signature_method=\"PLAINTEXT\", "
            + "oauth_nonce=\"" + nonce + "\", "
            + "oauth_callback=\"" + oauthConsumer.getConsumerCallback() + "\", "
            + "oauth_timestamp=\"" + new Date().getTime() / 1000L + "\", "
            + "oauth_signature=\"" + oauthConsumer.getConsumerKey() + "&\", "
            + "scope=\"get_balance\"";
        client.header("Authorization", oAuthHeader);
        return client.post(null);
    }
}


