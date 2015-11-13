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


/**
 * This class holds the relevant information about the URLs to use to contact the OAuth service, 
 * etc.
 */
public class OAuthConsumer {
    private String requestURL;
    private String accessURL;
    private String authorizationURL;
    private String consumerKey;
    private String consumerId;
    private String consumerCallback;
    
    public String getRequestURL() {
        return requestURL;
    }
    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }
    public String getAccessURL() {
        return accessURL;
    }
    public void setAccessURL(String accessURL) {
        this.accessURL = accessURL;
    }
    public String getAuthorizationURL() {
        return authorizationURL;
    }
    public void setAuthorizationURL(String authorizationURL) {
        this.authorizationURL = authorizationURL;
    }
    public String getConsumerKey() {
        return consumerKey;
    }
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }
    public String getConsumerId() {
        return consumerId;
    }
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
    public String getConsumerCallback() {
        return consumerCallback;
    }
    public void setConsumerCallback(String consumerCallback) {
        this.consumerCallback = consumerCallback;
    }
    
}


