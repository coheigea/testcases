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

package org.apache.coheigea.bigdata.hive;

import java.util.List;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizerFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrincipal;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilege;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeInfo;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveRoleGrant;

public class CustomHiveAuthorizerFactory implements HiveAuthorizerFactory {

    @Override
    public HiveAuthorizer createHiveAuthorizer(HiveMetastoreClientFactory metastoreClientFactory,
                                               HiveConf conf,
                                               HiveAuthenticationProvider hiveAuthenticator,
                                               HiveAuthzSessionContext sessionContext)
        throws HiveAuthzPluginException {
        return new CustomHiveAuthorizer(hiveAuthenticator);
    }
    
    
    /**
     * A trivial CustomHiveAuthorizer that allows the following:
     * a) The logged in user can do anything
     * b) "bob" can do a select on the tables
     * c) "alice" can do a select only on the "count" column
     */
    private static class CustomHiveAuthorizer implements HiveAuthorizer {
        
        private String remoteUser;
        
        CustomHiveAuthorizer(HiveAuthenticationProvider hiveAuthenticator) {
            remoteUser = hiveAuthenticator.getUserName();
        }

        @Override
        public void applyAuthorizationConfigPolicy(HiveConf arg0) throws HiveAuthzPluginException {
            
        }

        @Override
        public List<HivePrivilegeObject> applyRowFilterAndColumnMasking(HiveAuthzContext arg0,
                                                                        List<HivePrivilegeObject> arg1)
            throws SemanticException {
            return null;
        }

        @Override
        public void checkPrivileges(HiveOperationType hiveOpType, List<HivePrivilegeObject> inputHObjs,
                                    List<HivePrivilegeObject> outputHObjs, HiveAuthzContext context)
            throws HiveAuthzPluginException, HiveAccessControlException {
            // Allow the user running the test to do anything
            if (isLoggedInUser(remoteUser)) {
                return;
            }
            
            if ("bob".equals(remoteUser) && "QUERY".equals(hiveOpType.name())) {
                return;
            }

            if ("alice".equals(remoteUser)) {
                boolean correctColumn = true;
                for (HivePrivilegeObject obj : inputHObjs) {
                    if (!obj.getColumns().contains("count") || obj.getColumns().size() > 1) {
                        correctColumn = false;
                    }
                }
                if (correctColumn) {
                    return;
                }
            }
            
            throw new RuntimeException("Authorization failed for user: " + remoteUser);
        }

        @Override
        public void createRole(String arg0, HivePrincipal arg1)
            throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void dropRole(String arg0) throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public List<HivePrivilegeObject> filterListCmdObjects(List<HivePrivilegeObject> arg0,
                                                              HiveAuthzContext arg1)
            throws HiveAuthzPluginException, HiveAccessControlException {
            return null;
        }

        @Override
        public List<String> getAllRoles() throws HiveAuthzPluginException, HiveAccessControlException {
            return null;
        }

        @Override
        public List<String> getCurrentRoleNames() throws HiveAuthzPluginException {
            return null;
        }

        @Override
        public Object getHiveAuthorizationTranslator() throws HiveAuthzPluginException {
            return null;
        }

        @Override
        public List<HiveRoleGrant> getPrincipalGrantInfoForRole(String arg0)
            throws HiveAuthzPluginException, HiveAccessControlException {
            return null;
        }

        @Override
        public List<HiveRoleGrant> getRoleGrantInfoForPrincipal(HivePrincipal arg0)
            throws HiveAuthzPluginException, HiveAccessControlException {
            return null;
        }

        @Override
        public VERSION getVersion() {
            return null;
        }

        @Override
        public void grantPrivileges(List<HivePrincipal> arg0, List<HivePrivilege> arg1,
                                    HivePrivilegeObject arg2, HivePrincipal arg3, boolean arg4)
            throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void grantRole(List<HivePrincipal> arg0, List<String> arg1, boolean arg2, HivePrincipal arg3)
            throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean needTransform() {
            return false;
        }

        @Override
        public void revokePrivileges(List<HivePrincipal> arg0, List<HivePrivilege> arg1,
                                     HivePrivilegeObject arg2, HivePrincipal arg3, boolean arg4)
            throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");   
        }

        @Override
        public void revokeRole(List<HivePrincipal> arg0, List<String> arg1, boolean arg2, HivePrincipal arg3)
            throws HiveAuthzPluginException, HiveAccessControlException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setCurrentRole(String arg0) throws HiveAccessControlException, HiveAuthzPluginException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public List<HivePrivilegeInfo> showPrivileges(HivePrincipal arg0, HivePrivilegeObject arg1)
            throws HiveAuthzPluginException, HiveAccessControlException {
            return null;
        }
        
        private boolean isLoggedInUser(String remoteUser) {
            return remoteUser != null && remoteUser.equals(System.getProperty("user.name"));
        }
        
    }
    
}
