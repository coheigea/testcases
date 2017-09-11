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

package org.apache.coheigea.cxf.syncope.common;

import java.util.Collection;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;

/**
 * Deploy some Syncope users + roles to Syncope to get the tests to work!
 */
public class SyncopeDeployer {

    private String address;

    @SuppressWarnings("unchecked")
    public void deployUserData() {
        WebClient client = WebClient.create(address);
        client = client.type("application/xml");

        String authorizationHeader =
            "Basic " + Base64Utility.encode(("admin" + ":" + "password").getBytes());

        client.header("Authorization", authorizationHeader);

        // Create the roles first
        client = client.path("roles");

        PagedResult<RoleTO> existingRoles = (PagedResult<RoleTO>)client.get(PagedResult.class);

        RoleTO bossRole = findOrCreateRole("boss", existingRoles, client);

        RoleTO employeeRole = findOrCreateRole("employee", existingRoles, client);

        // Now create the users
        client = client.replacePath("users");

        PagedResult<UserTO> existingUsers = (PagedResult<UserTO>)client.get(PagedResult.class);

        if (!doesUserAlreadyExist("alice", existingUsers.getResult())) {
            UserTO user = new UserTO();
            user.setUsername("alice");
            user.setPassword("security");

            MembershipTO membership = new MembershipTO();
            membership.setRoleId(employeeRole.getId());
            user.getMemberships().add(membership);
            membership = new MembershipTO();
            membership.setRoleId(bossRole.getId());
            user.getMemberships().add(membership);
            client.post(user, UserTO.class);
        }

        if (!doesUserAlreadyExist("bob", existingUsers.getResult())) {
            UserTO user = new UserTO();
            user.setUsername("bob");
            user.setPassword("security");

            MembershipTO membership = new MembershipTO();
            membership.setRoleId(employeeRole.getId());
            user.getMemberships().add(membership);
            client.post(user, UserTO.class);
        }
    }

    private RoleTO findOrCreateRole(
                                    String roleName,
                                    PagedResult<RoleTO> roles,
                                    WebClient client
        ) {
        // See if the Role already exists
        for (RoleTO role : roles.getResult()) {
            if (roleName.equals(role.getName())) {
                return role;
            }
        }

        RoleTO role = new RoleTO();
        role.setName(roleName);
        return client.post(role, RoleTO.class);
    }

    private boolean doesUserAlreadyExist(String username, Collection<? extends UserTO> users) {
        for (UserTO user : users) {
            if (username.equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


}
