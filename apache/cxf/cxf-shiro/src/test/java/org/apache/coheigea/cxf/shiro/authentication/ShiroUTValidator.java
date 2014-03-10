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

package org.apache.coheigea.cxf.shiro.authentication;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

/**
 * This is a custom Validator that authenticates via Apache Shiro.
 */
public class ShiroUTValidator implements Validator {
    
    private static org.apache.commons.logging.Log log = 
            org.apache.commons.logging.LogFactory.getLog(ShiroUTValidator.class);
    private final List<String> requiredRoles = new ArrayList<String>();
    
    public ShiroUTValidator(String iniResourcePath) {
        Factory<SecurityManager> factory = new IniSecurityManagerFactory(iniResourcePath);
        SecurityUtils.setSecurityManager(factory.getInstance());
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
        
        // Validate it via Shiro
        Subject currentUser = SecurityUtils.getSubject();
        UsernamePasswordToken token = 
            new UsernamePasswordToken(usernameToken.getName(), usernameToken.getPassword());
        try {
            currentUser.login(token);
        } catch (AuthenticationException ex) {
            if (log.isDebugEnabled()) {
                log.debug(ex.getMessage(), ex);
            }
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }

        // Perform authorization check
        if (!requiredRoles.isEmpty() && !currentUser.hasAllRoles(requiredRoles)) {
            log.debug("Authorization failed for authenticated user");
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
        
        return credential;
    }

    public List<String> getRequiredRoles() {
        return requiredRoles;
    }
    
    public void setRequiredRoles(List<String> roles) {
        requiredRoles.addAll(roles);
    }
}
