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
package org.apache.coheigea.cxf.jaxrs.json.jwt;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.coheigea.cxf.jaxrs.json.common.Number;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jaxrs.JwtTokenSecurityContext;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.security.SecurityContext;
import org.junit.Assert;

@Path("/services")
public class DoubleItAuthenticationService {
    
    @Context 
    MessageContext jaxrsContext;
    
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Number doubleIt(Number numberToDouble) {
        // Check the context was set up correctly etc.
        Assert.assertNotNull(jaxrsContext.getSecurityContext().getUserPrincipal());
        
        JwtTokenSecurityContext securityContext = 
            (JwtTokenSecurityContext)jaxrsContext.get(SecurityContext.class.getName());
        Assert.assertNotNull(securityContext);
        Assert.assertEquals("DoubleItSTSIssuer",
                            securityContext.getToken().getClaim(JwtConstants.CLAIM_ISSUER));
        
        Number newNumber = new Number();
        newNumber.setDescription(numberToDouble.getDescription());
        newNumber.setNumber(numberToDouble.getNumber() * 2);
        return newNumber;
    }
    
}
