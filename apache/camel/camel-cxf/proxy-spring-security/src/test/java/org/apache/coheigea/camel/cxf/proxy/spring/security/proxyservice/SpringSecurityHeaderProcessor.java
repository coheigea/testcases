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
package org.apache.coheigea.camel.cxf.proxy.spring.security.proxyservice;

import java.security.Principal;

import javax.security.auth.Subject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * A Camel processor that takes the Subject stored in the CamelAuthentication header, and stores the received username and password
 * in the correct headers so that it can be read by Camel-Shiro.
 */
public class SpringSecurityHeaderProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
        Subject subject = exchange.getIn().getHeader(Exchange.AUTHENTICATION, Subject.class);
        if (subject != null && subject.getPrincipals() != null && !subject.getPrincipals().isEmpty()) {
            UsernamePasswordAuthenticationToken authToken = null;
            for (Principal principal : subject.getPrincipals()) {
                if (principal instanceof WSUsernameTokenPrincipalImpl) {
                    authToken = 
                        new UsernamePasswordAuthenticationToken(principal.getName(), 
                                                                ((WSUsernameTokenPrincipalImpl)principal).getPassword());
                    break;
                }
            }
            
            if (authToken != null) {
                subject.getPrincipals().clear();
                subject.getPrincipals().add(authToken);
            }
        }
    }
    
}
