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
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.UserService;
import org.junit.Assert;

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
        client.accept("application/xml");

        // Create the groups first
        client = client.path("groups");

        PagedResult<GroupTO> existingGroups = (PagedResult<GroupTO>)client.get(PagedResult.class);

        GroupTO bossGroup = findOrCreateGroup("boss", existingGroups, client);

        GroupTO employeeGroup = findOrCreateGroup("employee", existingGroups, client);

        // Now create the users
        client = client.replacePath("users");

        PagedResult<UserTO> existingUsers = (PagedResult<UserTO>)client.get(PagedResult.class);

        if (!doesUserAlreadyExist("alice", existingUsers.getResult())) {
            UserTO user = new UserTO();
            user.setUsername("alice");
            user.setPassword("security");
            user.setRealm("/");

            MembershipTO membership = new MembershipTO();
            membership.setRightKey(employeeGroup.getKey());
            // membership.setGroupName(employeeGroup.getName());
            user.getMemberships().add(membership);
            membership = new MembershipTO();
            // membership.setGroupName(bossGroup.getName());
            membership.setRightKey(bossGroup.getKey());
            user.getMemberships().add(membership);
            client.post(user, ProvisioningResult.class);
        }

        if (!doesUserAlreadyExist("bob", existingUsers.getResult())) {
            UserTO user = new UserTO();
            user.setUsername("bob");
            user.setPassword("security");
            user.setRealm("/");

            MembershipTO membership = new MembershipTO();
            membership.setRightKey(employeeGroup.getKey());
            // membership.setGroupName(employeeGroup.getName());
            user.getMemberships().add(membership);
            client.post(user, ProvisioningResult.class);
        }

        client.close();

        // Check via the client API that the users were created correctly
        SyncopeClientFactoryBean clientFactory = new SyncopeClientFactoryBean().setAddress(address);
        SyncopeClient syncopeClient = clientFactory.create("admin", "password");

        UserService userService = syncopeClient.getService(UserService.class);

        int count = userService.search(new AnyQuery.Builder().build()).getTotalCount();
        Assert.assertEquals(2, count);
    }

    private GroupTO findOrCreateGroup(
                                    String roleName,
                                    PagedResult<GroupTO> roles,
                                    WebClient client
        ) {
        // See if the Group already exists
        for (GroupTO role : roles.getResult()) {
            if (roleName.equals(role.getName())) {
                return role;
            }
        }

        GroupTO role = new GroupTO();
        role.setName(roleName);
        role.setRealm("/");
        @SuppressWarnings("unchecked")
        ProvisioningResult<GroupTO> result = client.post(role, ProvisioningResult.class);
        return (GroupTO)result.getEntity();
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
