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
package org.apache.coheigea.cxf.kerberos.jwtjaxrs;

import java.net.URI;
import java.text.ParseException;

import javax.security.auth.Subject;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.auth.AbstractSpnegoAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.kerby.kerberos.kerb.KrbRuntime;
import org.apache.kerby.kerberos.kerb.type.base.AuthToken;
import org.apache.kerby.kerberos.kerb.type.base.KrbToken;
import org.apache.kerby.kerberos.kerb.type.base.TokenFormat;
import org.apache.kerby.kerberos.provider.token.JwtAuthToken;
import org.apache.kerby.kerberos.provider.token.JwtTokenProvider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

/**
 * A Custom HttpAuthSupplier implementation that decorates the Subject with the JWT Token.
 */
public class KerbyHttpAuthSupplier extends AbstractSpnegoAuthSupplier implements HttpAuthSupplier {

    private String jwtToken;

    @Override
    protected void decorateSubject(Subject subject) {
        KrbRuntime.setTokenProvider(new JwtTokenProvider());
        try {
            JWT jwt = JWTParser.parse(jwtToken);
            AuthToken authToken = new JwtAuthToken(jwt.getJWTClaimsSet());

            KrbToken krbToken = new KrbToken(authToken, TokenFormat.JWT);
            krbToken.setTokenValue(jwtToken.getBytes());

            subject.getPrivateCredentials().add(krbToken);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getAuthorization(AuthorizationPolicy  authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
       return super.getAuthorization(authPolicy, currentURI, message);
   }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    @Override
    public boolean requiresRequestCaching() {
        return false;
    }

}
