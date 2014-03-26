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

package org.apache.coheigea.cxf.samlsso.idp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.AttributeBean;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;
import org.apache.ws.security.saml.ext.bean.SubjectConfirmationDataBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.opensaml.common.SAMLVersion;

/**
 * A Callback Handler implementation for a SAML 2 assertion for use by the SAML SSO IdP. By
 * default it creates a SAML 2.0 Assertion with an AuthenticationStatement. If a list of roles 
 * are also supplied, it will insert them as part of an AttributeStatement.
 */
public class SAML2CallbackHandler implements CallbackHandler {
    
    private String subjectName;
    private String subjectQualifier;
    private String confirmationMethod = SAML2Constants.CONF_BEARER;
    private String issuer;
    private String subjectNameIDFormat;
    private ConditionsBean conditions;
    private SubjectConfirmationDataBean subjectConfirmationData;
    
    private void createAndSetStatement(SAMLCallback callback) {
        AuthenticationStatementBean authBean = new AuthenticationStatementBean();
        authBean.setAuthenticationMethod("Password");
        callback.setAuthenticationStatementData(Collections.singletonList(authBean));

        // Add roles for certain users
        List<String> roles = new ArrayList<String>();
        if ("alice".equals(subjectName)) {
            roles.add("boss");
            roles.add("employee");
        } else if ("bob".equals(subjectName)) {
            roles.add("employee");
        }
        
        if (!roles.isEmpty()) {
            AttributeStatementBean attrBean = new AttributeStatementBean();
            AttributeBean attributeBean = new AttributeBean();
            attributeBean.setQualifiedName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
            attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
            attributeBean.setAttributeValues(roles);
                
            attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
            callback.setAttributeStatementData(Collections.singletonList(attrBean));
        }
    }
    
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setSamlVersion(SAMLVersion.VERSION_20);
                callback.setIssuer(issuer);
                if (conditions != null) {
                    callback.setConditions(conditions);
                }
                
                SubjectBean subjectBean = 
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                if (subjectNameIDFormat != null) {
                    subjectBean.setSubjectNameIDFormat(subjectNameIDFormat);
                }
                subjectBean.setSubjectConfirmationData(subjectConfirmationData);

                callback.setSubject(subjectBean);
                createAndSetStatement(callback);
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }
    
    public void setSubjectConfirmationData(SubjectConfirmationDataBean subjectConfirmationData) {
        this.subjectConfirmationData = subjectConfirmationData;
    }
    
    public void setConditions(ConditionsBean conditionsBean) {
        this.conditions = conditionsBean;
    }
    
    public void setConfirmationMethod(String confMethod) {
        confirmationMethod = confMethod;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public void setSubjectNameIDFormat(String subjectNameIDFormat) {
        this.subjectNameIDFormat = subjectNameIDFormat;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectQualifier() {
        return subjectQualifier;
    }

    public void setSubjectQualifier(String subjectQualifier) {
        this.subjectQualifier = subjectQualifier;
    }
    
}
