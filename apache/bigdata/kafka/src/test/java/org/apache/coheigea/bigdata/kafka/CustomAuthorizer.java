/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.coheigea.bigdata.kafka;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

import kafka.network.RequestChannel.Session;
import kafka.security.auth.Acl;
import kafka.security.auth.Authorizer;
import kafka.security.auth.Operation;
import kafka.security.auth.Resource;
import scala.collection.immutable.Set;

/**
 * A trivial Kafka Authorizer. The "logged in" user can do the CLUSTER_ACTION, READ, WRITE, DESCRIBE and CREATE operations, but no others.
 * Basically, just the permissions that are required to set up a broker, create a topic + write + read some data to/from it. As we're not 
 * using Kerberos we don't have the luxury of logging different users in etc.   
 */
public class CustomAuthorizer implements Authorizer {

    @Override
    public void configure(Map<String, ?> arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addAcls(Set<Acl> arg0, Resource arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean authorize(Session arg0, Operation arg1, Resource arg2) {
        try {
            String currentUser = UserGroupInformation.getCurrentUser().getUserName();
            if (isLoggedInUser(currentUser) 
                && ("ClusterAction".equals(arg1.name()) || "Read".equals(arg1.name()) || "Write".equals(arg1.name())
                    || "Describe".equals(arg1.name()) || "Create".equals(arg1.name()))) {
                return true;
            }
            
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Acl> getAcls(Resource arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls(KafkaPrincipal arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeAcls(Resource arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeAcls(Set<Acl> arg0, Resource arg1) {
        // TODO Auto-generated method stub
        return false;
    }
    
    private boolean isLoggedInUser(String remoteUser) {
        return remoteUser != null && remoteUser.equals(System.getProperty("user.name"));
    }
}
