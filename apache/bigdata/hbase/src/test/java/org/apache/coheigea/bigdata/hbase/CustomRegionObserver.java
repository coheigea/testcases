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

package org.apache.coheigea.bigdata.hbase;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.security.AccessDeniedException;
import org.apache.hadoop.hbase.security.User;

/**
 * A custom HBase RegionObserver which allows the process owner access for a limited number of operations, but no other user.
 */
public class CustomRegionObserver extends BaseRegionObserver {
    
    private void authorizeUser() throws IOException {
        User user = RpcServer.getRequestUser();
        if (user == null) {
            user = User.getCurrent();
        }
        String loggedInUser = System.getProperty("user.name");
        if (user.getShortName() == null || !user.getShortName().startsWith(loggedInUser)) {
            throw new AccessDeniedException("Access is denied for: " + user.getShortName());
        }
    }

    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> arg0, Delete arg1, WALEdit arg2,
                           Durability arg3)
        throws IOException {
        authorizeUser();
    }

    @Override
    public void preGetOp(ObserverContext<RegionCoprocessorEnvironment> arg0, Get arg1, List<Cell> arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void prePut(ObserverContext<RegionCoprocessorEnvironment> arg0, Put arg1, WALEdit arg2,
                       Durability arg3)
        throws IOException {
        authorizeUser();
        
    }
}

