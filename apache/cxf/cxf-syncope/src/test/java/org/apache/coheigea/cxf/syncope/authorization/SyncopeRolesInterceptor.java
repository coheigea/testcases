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
package org.apache.coheigea.cxf.syncope.authorization;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Take the received usernametoken and authenticate the username/password credential. Then read the
 * user from Syncope and get the roles. Store these in a new Subject that can be authorized by a 
 * downstream authorizing interceptor.
 */
public class SyncopeRolesInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static org.apache.commons.logging.Log log = 
            org.apache.commons.logging.LogFactory.getLog(SyncopeRolesInterceptor.class);
    
    private String address;

    public SyncopeRolesInterceptor() {
        super(Phase.PRE_INVOKE);
        super.addBefore(SimpleAuthorizingInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) throws Fault {
        SecurityContext context = message.get(SecurityContext.class);
        if (context == null) {
            return;
        }
        Principal principal = context.getUserPrincipal();
        UsernameToken usernameToken = (UsernameToken)message.get(SecurityToken.class);
        if (principal == null || usernameToken == null
            || !principal.getName().equals(usernameToken.getName())) {
            return;
        }
        
        // Read the user from Syncope and get the roles
        WebClient client = 
            WebClient.create(address, Collections.singletonList(new JacksonJsonProvider()));
        
        String authorizationHeader = 
            "Basic " + Base64Utility.encode(
                (usernameToken.getName() + ":" + usernameToken.getPassword()).getBytes()
            );
        
        client.header("Authorization", authorizationHeader);
        
        client = client.path("users/self");
        UserTO user = null;
        try {
            user = client.get(UserTO.class);
            if (user == null) {
                Exception exception = new Exception("Authentication failed");
                throw new Fault(exception);
            }
        } catch (RuntimeException ex) {
            if (log.isDebugEnabled()) {
                log.debug(ex.getMessage(), ex);
            }
            throw new Fault(ex);
        }
        
        // Now get the roles
        List<MembershipTO> membershipList = user.getMemberships();
        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        for (MembershipTO membership : membershipList) {
            String roleName = membership.getRoleName();
            subject.getPrincipals().add(new SimpleGroup(roleName, usernameToken.getName()));
        }
        subject.setReadOnly();

        message.put(SecurityContext.class, new DefaultSecurityContext(principal, subject));
    }
    
    public void setAddress(String newAddress) {
        address = newAddress;
    }
    
    public String getAddress() {
        return address;
    }
    
}
