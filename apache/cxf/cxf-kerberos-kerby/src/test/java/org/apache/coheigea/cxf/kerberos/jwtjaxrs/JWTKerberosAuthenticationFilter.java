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

import java.util.List;

import org.apache.cxf.jaxrs.security.KerberosAuthenticationFilter;
import org.apache.cxf.security.SecurityContext;
import org.apache.kerby.kerberos.kerb.type.ad.AdToken;
import org.apache.kerby.kerberos.kerb.type.base.KrbToken;
import org.ietf.jgss.GSSContext;

/**
 * This class extends the CXF KerberosAuthenticationFilter, using the GSSContext to get the AuthorizationData and
 * extract the role from the embedded token.
 */
public class JWTKerberosAuthenticationFilter extends KerberosAuthenticationFilter {

    @Override
    protected SecurityContext createSecurityContext(String simpleUserName, String complexUserName,
                                                    GSSContext gssContext) {
        return new JWTKerberosSecurityContext(new KerberosPrincipal(simpleUserName, complexUserName), gssContext);
    }


    private static class JWTKerberosSecurityContext extends KerberosSecurityContext {

        private static final String ROLE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

        private Object roles;

        public JWTKerberosSecurityContext(KerberosPrincipal principal, GSSContext context) {
            super(principal, context);

            try {
                com.sun.security.jgss.ExtendedGSSContext extendedContext =
                    (com.sun.security.jgss.ExtendedGSSContext) context;
                com.sun.security.jgss.AuthorizationDataEntry[] authzDataEntries =
                    (com.sun.security.jgss.AuthorizationDataEntry[])
                    extendedContext.inquireSecContext(com.sun.security.jgss.InquireType.KRB5_GET_AUTHZ_DATA);
                if (authzDataEntries != null && authzDataEntries.length > 0) {
                    byte[] data = authzDataEntries[0].getData();
                    AdToken adToken = new AdToken();
                    adToken.decode(data);
                    KrbToken receivedAccessToken = adToken.getToken();
                    if (receivedAccessToken.getAttributes().containsKey(ROLE)) {
                        roles = receivedAccessToken.getAttributes().get(ROLE);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public boolean isUserInRole(String role) {
            if (this.roles instanceof String) {
                return this.roles.equals(role);
            } else if (this.roles instanceof List<?>) {
                return ((List<?>) this.roles).contains(role);
            }
            return false;
        }
    }
}
