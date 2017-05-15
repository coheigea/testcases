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

import java.util.Map;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

import kafka.network.RequestChannel.Session;
import kafka.security.auth.Acl;
import kafka.security.auth.Authorizer;
import kafka.security.auth.Operation;
import kafka.security.auth.Resource;
import scala.collection.immutable.Set;

/**
 * A trivial Kafka Authorizer for use with SASL PLAINTEXT + GSS.
 * 
 * The "kafka" principal can do anything,
 * The "client" principal can read/describe/write on the "test*" topics.
 */
public class CustomSASLGSSAuthorizer implements Authorizer {

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
        if (arg0.principal() == null) {
            return false;
        }
        String principal = arg0.principal().getName();
        if ("Describe".equals(arg1.name())) {
            return true;
        } else if (principal.equals("client") && ("Read".equals(arg1.name())
            || "Write".equals(arg1.name()) || "Create".equals(arg1.name())) && arg2.name().startsWith("test")) {
            return true;
        } else if (principal.equals("kafka")) {
            return true;
        }
        
        return false;
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
    
}
