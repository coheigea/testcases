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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * A ClaimsHandler implementation that works with Roles obtained from Keycloak.
 */
public class KeycloakRolesClaimsHandler implements ClaimsHandler {

    public static final URI ROLE =
            URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");

    private String address;
    private String realm;
    private String adminUser;
    private String adminPassword;

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        if (claims != null && claims.size() > 0) {
            ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
            for (Claim requestClaim : claims) {
                ProcessedClaim claim = new ProcessedClaim();
                claim.setClaimType(requestClaim.getClaimType());
                if (ROLE.equals(requestClaim.getClaimType())) {

                    Keycloak keyCloak = KeycloakBuilder.builder()
                        .serverUrl(address)
                        .realm(realm)
                        .username(adminUser)
                        .password(adminPassword)
                        .clientId("admin-cli")
                        .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build())
                        .build();

                    claim.setIssuer("keycloak");

                    // Search for the user using the admin credentials
                    List<UserRepresentation> users =
                        keyCloak.realm(realm).users().search(parameters.getPrincipal().getName());
                    if (users != null) {
                        for (UserRepresentation user : users) {
                            UserResource userResource = keyCloak.realm(realm).users().get(user.getId());
                            // Add the effective roles to the claim
                            for (RoleRepresentation roleRep : userResource.roles().realmLevel().listEffective()) {
                                claim.addValue(roleRep.getName());
                            }
                        }
                    }
                }
                claimCollection.add(claim);
            }
            return claimCollection;
        }
        return null;
    }

    public List<URI> getSupportedClaimTypes() {
        List<URI> list = new ArrayList<URI>();
        list.add(ROLE);
        return list;
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

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

}
