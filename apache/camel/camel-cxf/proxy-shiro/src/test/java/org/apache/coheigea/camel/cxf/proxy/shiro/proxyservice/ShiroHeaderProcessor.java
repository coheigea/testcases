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
package org.apache.coheigea.camel.cxf.proxy.shiro.proxyservice;

import java.security.Key;
import java.security.Principal;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.security.auth.Subject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;

/**
 * A Camel processor that takes the Subject stored in the CamelAuthentication header, and stores the received username and password
 * in the correct headers so that it can be read by Camel-Shiro.
 */
public class ShiroHeaderProcessor implements Processor {

    private static Key key;

    static {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            key = generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static byte[] getKey() throws NoSuchAlgorithmException {
        return key.getEncoded();
    }

    public void process(Exchange exchange) throws Exception {
        Subject subject = exchange.getIn().getHeader(Exchange.AUTHENTICATION, Subject.class);
        if (subject != null && subject.getPrincipals() != null && !subject.getPrincipals().isEmpty()) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal instanceof WSUsernameTokenPrincipalImpl) {
                    exchange.getIn().setHeader("SHIRO_SECURITY_USERNAME", principal.getName());
                    exchange.getIn().setHeader("SHIRO_SECURITY_PASSWORD", ((WSUsernameTokenPrincipalImpl)principal).getPassword());
                    break;
                }
            }
        }
    }
    
}
