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

import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.ProcedureInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.MasterObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.master.RegionPlan;
import org.apache.hadoop.hbase.master.procedure.MasterProcedureEnv;
import org.apache.hadoop.hbase.procedure2.ProcedureExecutor;
import org.apache.hadoop.hbase.protobuf.generated.HBaseProtos.SnapshotDescription;
import org.apache.hadoop.hbase.protobuf.generated.QuotaProtos.Quotas;
import org.apache.hadoop.hbase.security.AccessDeniedException;
import org.apache.hadoop.hbase.security.User;

/**
 * A custom HBase MasterObserver which allows the "admin" user access, but no other user
 */
public class CustomMasterObserver implements MasterObserver {
    
    private void authorizeUser() throws IOException {
        User user = RpcServer.getRequestUser();
        if (user == null) {
            user = User.getCurrent();
        }
        String loggedInUser = System.getProperty("user.name");
        if (!loggedInUser.equals(user.getShortName())) {
            throw new AccessDeniedException("Access is denied for: " + user.getShortName());
        }
    }

    @Override
    public void start(CoprocessorEnvironment arg0) throws IOException {
        // Allow anyone to start to make the test setup easier
    }

    @Override
    public void stop(CoprocessorEnvironment arg0) throws IOException {
     // Allow anyone to stop to make the test setup easier
    }

    @Override
    public void postAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postAddColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                              HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postAddColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                     HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postAssign(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postBalance(ObserverContext<MasterCoprocessorEnvironment> arg0, List<RegionPlan> arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> arg0, boolean arg1,
                                  boolean arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                  SnapshotDescription arg1, HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                    NamespaceDescriptor arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postCreateTable(ObserverContext<MasterCoprocessorEnvironment> arg0, HTableDescriptor arg1,
                                HRegionInfo[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postCreateTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                       HTableDescriptor arg1, HRegionInfo[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                 byte[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                        byte[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                   SnapshotDescription arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDeleteTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDisableTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postDisableTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postEnableTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postEnableTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postGetNamespaceDescriptor(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                           NamespaceDescriptor arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                        List<HTableDescriptor> arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                        List<TableName> arg1, List<HTableDescriptor> arg2, String arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postGetTableNames(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                  List<HTableDescriptor> arg1, String arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                             List<NamespaceDescriptor> arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postListProcedures(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                   List<ProcedureInfo> arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postListSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0, SnapshotDescription arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postModifyColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                 HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postModifyColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                        HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                    NamespaceDescriptor arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postModifyTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postModifyTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                       HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postMove(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1,
                         ServerName arg2, ServerName arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postRegionOffline(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                    SnapshotDescription arg1, HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1,
                                      Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                  Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1, Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1,
                                 TableName arg2, Quotas arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1, String arg2,
                                 Quotas arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0, SnapshotDescription arg1,
                             HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postStartMaster(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postTableFlush(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postTruncateTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postTruncateTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void postUnassign(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1,
                             boolean arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                  ProcedureExecutor<MasterProcedureEnv> arg1, long arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preAddColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                             HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preAddColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                    HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preAssign(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preBalance(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public boolean preBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> arg0, boolean arg1)
        throws IOException {
        authorizeUser();
        return false;
    }

    @Override
    public void preCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0, SnapshotDescription arg1,
                                 HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                   NamespaceDescriptor arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preCreateTable(ObserverContext<MasterCoprocessorEnvironment> arg0, HTableDescriptor arg1,
                               HRegionInfo[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preCreateTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                      HTableDescriptor arg1, HRegionInfo[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                byte[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                       byte[] arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                  SnapshotDescription arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDeleteTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDisableTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preDisableTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preEnableTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preEnableTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preGetNamespaceDescriptor(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                       List<TableName> arg1, List<HTableDescriptor> arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                       List<TableName> arg1, List<HTableDescriptor> arg2, String arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preGetTableNames(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                 List<HTableDescriptor> arg1, String arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                            List<NamespaceDescriptor> arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preListProcedures(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preListSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0, SnapshotDescription arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preMasterInitialization(ObserverContext<MasterCoprocessorEnvironment> arg0)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preModifyColumn(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preModifyColumnHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                       HColumnDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                   NamespaceDescriptor arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preModifyTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                               HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preModifyTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                      HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preMove(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1, ServerName arg2,
                        ServerName arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preRegionOffline(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0,
                                   SnapshotDescription arg1, HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1,
                                     Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1,
                                 Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1, Quotas arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1,
                                TableName arg2, Quotas arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1, String arg2,
                                Quotas arg3)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preShutdown(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preSnapshot(ObserverContext<MasterCoprocessorEnvironment> arg0, SnapshotDescription arg1,
                            HTableDescriptor arg2)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preStopMaster(ObserverContext<MasterCoprocessorEnvironment> arg0) throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preTableFlush(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preTruncateTable(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preTruncateTableHandler(ObserverContext<MasterCoprocessorEnvironment> arg0, TableName arg1)
        throws IOException {
        authorizeUser();
        
    }

    @Override
    public void preUnassign(ObserverContext<MasterCoprocessorEnvironment> arg0, HRegionInfo arg1,
                            boolean arg2)
        throws IOException {
        authorizeUser();
        
    }
}

