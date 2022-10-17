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

package org.apache.coheigea.cxf.sts;

import javax.ws.rs.ForbiddenException;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

/**
 * This is a custom Validator that authenticates to a Keycloak IDM and checks to see whether the
 * supplied username and password are in the system.
 */
public class KeycloakUTValidator implements Validator {

    private static org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(KeycloakUTValidator.class);

    private String address;
    private String realm;

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
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
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        if (usernameToken.getPassword() == null) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication failed - no password was provided");
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        // Send it off to Keycloak for validation
        Keycloak keyCloak = KeycloakBuilder.builder()
            .serverUrl(address)
            .realm(realm)
            .username(usernameToken.getName())
            .password(usernameToken.getPassword())
            .clientId("admin-cli")
            .build();

        try {
            keyCloak.realm(realm).users().search(usernameToken.getName());
        } catch (ForbiddenException ex) {
            // We allow 403 here as we only care about authentication. 403 means authentication succeeds but
            // the user might not have the permissions to access the admin-cli
        } catch (RuntimeException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        return credential;
    }

    public void setAddress(String newAddress) {
        address = newAddress;
    }

    public String getAddress() {
        return address;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
