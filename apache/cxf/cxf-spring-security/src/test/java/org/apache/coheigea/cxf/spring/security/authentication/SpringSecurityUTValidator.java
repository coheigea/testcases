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

package org.apache.coheigea.cxf.spring.security.authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

/**
 * This is a custom Validator that authenticates via Spring Security.
 */
public class SpringSecurityUTValidator implements Validator {
    
    private static org.apache.commons.logging.Log log = 
            org.apache.commons.logging.LogFactory.getLog(SpringSecurityUTValidator.class);
    private AuthenticationManager authenticationManager;
    private AccessDecisionManager accessDecisionManager;
    private final List<String> requiredRoles = new ArrayList<String>();
    
    public SpringSecurityUTValidator(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    
    public SpringSecurityUTValidator(AuthenticationManager authenticationManager,
            AccessDecisionManager accessDecisionManager) {
        this.authenticationManager = authenticationManager;
        this.accessDecisionManager = accessDecisionManager;
    }
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "noCredential");
        }
        
        // Validate the UsernameToken
        UsernameToken usernameToken = credential.getUsernametoken();
        String pwType = usernameToken.getPasswordType();
        if (log.isDebugEnabled()) {
            log.debug("UsernameToken user " + usernameToken.getName());
            log.debug("UsernameToken password type " + pwType);
        }
        if (!WSConstants.PASSWORD_TEXT.equals(pwType)) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication failed - digest passwords are not accepted");
            }
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
        if (usernameToken.getPassword() == null) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication failed - no password was provided");
            }
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
        
        // Validate it via Spring Security
        
        // Set a Subject up
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(usernameToken.getName(), 
                                                    usernameToken.getPassword());
        Subject subject = new Subject();
        subject.getPrincipals().add(authToken);
        
        Set<Authentication> authentications = subject.getPrincipals(Authentication.class);
        Authentication authenticated = null;
        try {
            authenticated = 
                authenticationManager.authenticate(authentications.iterator().next());
        } catch (AuthenticationException ex) {
            if (log.isDebugEnabled()) {
                log.debug(ex.getMessage(), ex);
            }
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
        
        if (!authenticated.isAuthenticated()) {
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
        
        for (GrantedAuthority authz : authenticated.getAuthorities()) {
            System.out.println("Granted: " + authz.getAuthority());
        }
        
        // Authorize request
        if (accessDecisionManager != null && !requiredRoles.isEmpty()) {
            List<ConfigAttribute> attributes
                = SecurityConfig.createList(requiredRoles.toArray(new String[requiredRoles.size()]));
            for (ConfigAttribute attr : attributes) {
                System.out.println("Attr: " + attr.getAttribute());
            }
            accessDecisionManager.decide(authenticated, this, attributes);
        }

        credential.setSubject(subject);
        return credential;
    }

    public List<String> getRequiredRoles() {
        return requiredRoles;
    }
    
    public void setRequiredRoles(List<String> roles) {
        requiredRoles.addAll(roles);
    }
}
