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
package org.apache.coheigea.cxf.openaz.xacml3;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.openaz.xacml.api.Decision;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.Response;
import org.apache.openaz.xacml.api.Result;
import org.apache.openaz.xacml.api.Status;
import org.apache.openaz.xacml.std.json.JSONRequest;
import org.apache.openaz.xacml.std.json.JSONResponse;

/**
 * A PEP implementation using XACML 3.0 based on OpenAZ. It sends a JSON request instead of DOM and expects a JSON response!
 */
public class XACML3AuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(XACML3AuthorizingInterceptor.class);

    private XACML3RequestBuilder requestBuilder = new DefaultXACML3RequestBuilder();
    private PolicyDecisionPoint pdp;

    public XACML3AuthorizingInterceptor(PolicyDecisionPoint pdp) {
        super(Phase.PRE_INVOKE);
        this.pdp = pdp;
    }
    
    public void handleMessage(Message message) throws Fault {
        SecurityContext sc = message.get(SecurityContext.class);
        
        if (sc instanceof LoginSecurityContext) {
            Principal principal = sc.getUserPrincipal();
            
            LoginSecurityContext loginSecurityContext = (LoginSecurityContext)sc;
            Set<Principal> principalRoles = loginSecurityContext.getUserRoles();
            List<String> roles = new ArrayList<String>();
            if (principalRoles != null) {
                for (Principal p : principalRoles) {
                    if (p != principal) {
                        roles.add(p.getName());
                    }
                }
            }
            
            try {
                if (authorize(principal, roles, message)) {
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Unauthorized: " + e.getMessage(), e);
                throw new AccessDeniedException("Unauthorized");
            }
        } else {
            LOG.log(
                Level.FINE,
                "The SecurityContext was not an instance of LoginSecurityContext. No authorization "
                + "is possible as a result"
            );
        }
        
        throw new AccessDeniedException("Unauthorized");
    }


    protected boolean authorize(Principal principal, List<String> roles, Message message) throws Exception {
        Request request = requestBuilder.createRequest(principal, roles, message);
        String jsonRequest = JSONRequest.toString(request);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("XACML Request: " + jsonRequest);
        }
        
        // Evaluate the request
        String responseString = this.pdp.evaluate(jsonRequest);

        // Parse the Response into an OpenAZ Response Object
        Response response = JSONResponse.load(responseString);
        Collection<Result> results = response.getResults();

        if (results == null) {
            return false;
        }

        for (Result result : results) {
            // Handle any Obligations returned by the PDP
            handleObligations(request, principal, message, result);

            Decision decision = result.getDecision() != null ? result.getDecision() : Decision.DENY;
            String code = "";
            String statusMessage = "";
            if (result.getStatus() != null) {
                Status status = result.getStatus();
                code = status.getStatusCode() != null ? status.getStatusCode().getStatusCodeValue().stringValue() : "";
                statusMessage = status.getStatusMessage() != null ? status.getStatusMessage() : "";
            }
            LOG.fine("XACML authorization result: " + decision + ", code: " + code + ", message: " + statusMessage);
            return decision == Decision.PERMIT;
        }

        return false;
    }
    
    /**
     * Handle any Obligations returned by the PDP
     */
    protected void handleObligations(
        Request request,
        Principal principal,
        Message message,
        Result result
    ) throws Exception {
        // Do nothing by default
    }

    public XACML3RequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    public void setRequestBuilder(XACML3RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }


}
