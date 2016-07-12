/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.coheigea.bigdata.solr.ranger;

import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.ranger.authorization.solr.authorizer.RangerSolrAuthorizer;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.AuthorizationContext.RequestType;
import org.apache.solr.security.AuthorizationResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * Some mock tests for the RangerSolrAuthorizer.
 */
public class SolrAuthorizationMockTest extends org.junit.Assert {
    
    @Test
    public void testRangerAuthorization() throws Exception {
        
        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("userPrincipal", "alice");
        requestParameters.put("collectionRequests", "docs");
        requestParameters.put("requestType", RequestType.READ);
        
        AuthorizationContext context = new MockAuthorizationContext(requestParameters);
        RangerSolrAuthorizer plugin = new RangerSolrAuthorizer();
        plugin.init(null);
        
        AuthorizationResponse authResp = plugin.authorize(context);
        Assert.assertEquals(200, authResp.statusCode);
        
        plugin.close();
    }

    private static class MockAuthorizationContext extends AuthorizationContext {
        private final Map<String,Object> values;

        private MockAuthorizationContext(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public SolrParams getParams() {
            SolrParams params = (SolrParams) values.get("params");
            return params == null ?  new MapSolrParams(new HashMap<String, String>()) : params;
        }

        @Override
        public Principal getUserPrincipal() {
            Object userPrincipal = values.get("userPrincipal");
            return userPrincipal == null ? null : new BasicUserPrincipal(String.valueOf(userPrincipal));
        }

        @Override
        public String getHttpHeader(String header) {
            return null;
        }

        @Override
        public Enumeration<?> getHeaderNames() {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public List<CollectionRequest> getCollectionRequests() {
            Object collectionRequests = values.get("collectionRequests");
            if (collectionRequests instanceof String) {
                return Collections.singletonList(new CollectionRequest((String)collectionRequests));
            }
            return (List<CollectionRequest>) collectionRequests;
        }

        @Override
        public RequestType getRequestType() {
            return (RequestType) values.get("requestType");
        }

        @Override
        public String getHttpMethod() {
            return (String) values.get("httpMethod");
        }

        @Override
        public String getResource() {
            return (String) values.get("resource");
        }
    }


}
