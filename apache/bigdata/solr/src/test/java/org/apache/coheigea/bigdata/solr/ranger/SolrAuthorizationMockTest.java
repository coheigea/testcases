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
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.security.UserGroupInformation;
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
 * Some mock tests for the RangerSolrAuthorizer using the RangerSolrAuthorizer. The policies are as follows:
 * 
 * a) "bob" has all privileges on the "docs" collection
 * b) "alice" and the "IT" group can only query the "docs" collection
 * c) The "Legal" group can only query the "docs" collection from the IP 127.0.0.*
 */
public class SolrAuthorizationMockTest extends org.junit.Assert {
    
    private final static RangerSolrAuthorizer plugin = new RangerSolrAuthorizer();
    
    public SolrAuthorizationMockTest() {
        plugin.init(null);
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        plugin.close();
    }
    
    @Test
    public void testReadPrivilege() throws Exception {
        performTest(200, "alice", RequestType.READ);
        performTest(200, "bob", RequestType.READ);
        performTest(403, "eve", RequestType.READ);
        performTest(200, "frank", "IT", RequestType.READ);
        performTest(403, "helen", "finance", RequestType.READ);
        
        performTest(200, "irene", "Legal", RequestType.READ, "127.0.0.5");
        performTest(403, "jack", "Legal", RequestType.READ, "127.0.1.5");
    }
    
    @Test
    public void testWritePrivilege() throws Exception {
        performTest(403, "alice", RequestType.WRITE);
        performTest(200, "bob", RequestType.WRITE);
        performTest(403, "eve", RequestType.WRITE);
        performTest(403, "frank", "IT", RequestType.WRITE);
    }
    
    @Test
    public void testOtherPrivilege() throws Exception {
        performTest(403, "alice", RequestType.UNKNOWN);
        performTest(200, "bob", RequestType.UNKNOWN);
        performTest(403, "eve", RequestType.UNKNOWN);
        performTest(403, "frank", "IT", RequestType.UNKNOWN);
    }
    
    @Test
    public void testAdminPrivilege() throws Exception {
        performTest(403, "alice", RequestType.ADMIN);
        performTest(200, "bob", RequestType.ADMIN);
        performTest(403, "eve", RequestType.ADMIN);
        performTest(403, "frank", "IT", RequestType.ADMIN);
    }
    
    private void performTest(int expectedStatus, String user, RequestType requestType) throws Exception {
        performTest(expectedStatus, user, null, requestType, null);
    }
    
    private void performTest(final int expectedStatus, String user, String group, RequestType requestType) throws Exception {
        performTest(expectedStatus, user, group, requestType, null);
    }
    
    private void performTest(final int expectedStatus, String user, String group, RequestType requestType, String ipAddress) throws Exception {
        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("userPrincipal", user);
        requestParameters.put("collectionRequests", "docs");
        requestParameters.put("requestType", requestType);
        if (ipAddress != null) {
            requestParameters.put("ipAddress", ipAddress);
        }
        
        final AuthorizationContext context = new MockAuthorizationContext(requestParameters);
        
        if (group != null) {
            UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {group});
            ugi.doAs(new PrivilegedExceptionAction<Void>() {
                public Void run() throws Exception {
                    AuthorizationResponse authResp = plugin.authorize(context);
                    Assert.assertEquals(expectedStatus, authResp.statusCode);
                    return null;
                }
            });
        } else {
            AuthorizationResponse authResp = plugin.authorize(context);
            Assert.assertEquals(expectedStatus, authResp.statusCode);
        }
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
            if ("REMOTE_ADDR".equals(header)) {
                return (String) values.get("ipAddress");
            }
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

        public Object getHandler() {
            // TODO Auto-generated method stub
            return null;
        }
    }


}
