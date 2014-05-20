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

package org.apache.coheigea.cxf.x509.authorization;

import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;

/**
 * This is a custom Validator that just mocks up some roles depending on the certificate
 * principal, so that the authorization interceptor in CXF will work. Authentication is
 * handled by the superclass SignatureTrustValidator.
 */
public class X509AuthorizationValidator extends SignatureTrustValidator {
    
    private static org.apache.commons.logging.Log log = 
            org.apache.commons.logging.LogFactory.getLog(X509AuthorizationValidator.class);
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
    	Credential validatedCredential = super.validate(credential, data);
        
        // Validate the Certificate
    	X509Certificate[] certs = validatedCredential.getCertificates();
        if (certs == null || certs.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No X.509 Certificates are found");
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        
        Principal principal = validatedCredential.getPrincipal();
        // Mock up a Subject
        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        subject.getPrincipals().add(new SimpleGroup("employee"));
        if (principal.getName().startsWith("CN=Client,O=Apache")) {
            subject.getPrincipals().add(new SimpleGroup("boss"));
        }
        subject.setReadOnly();
        credential.setSubject(subject);
        
        return credential;
    }

}
