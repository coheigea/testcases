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

package org.apache.coheigea.cxf.oauth1.balanceservice;

import javax.annotation.Resource;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth.data.OAuthContext;

/**
 * A "Balance" service for a bank. It checks that the original OAuth end username is the same
 * as the given username.
 */
@Path("/balance/")
public class PartnerBalanceService extends BalanceService {
    
    @Resource
    private MessageContext messageContext;
    
    @Override
    protected void authenticateUser(String user) {
        OAuthContext oauthContext = messageContext.getContent(OAuthContext.class);
        
        if (oauthContext == null || oauthContext.getSubject() == null
            || !user.equals(oauthContext.getSubject().getLogin())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }
    
}


