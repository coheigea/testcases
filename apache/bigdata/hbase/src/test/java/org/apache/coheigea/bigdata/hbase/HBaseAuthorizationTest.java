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
import java.net.ServerSocket;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Assert;

/**
 * Here we plug custom CoProcessors into HBase for authorization. The logged in user can do anything, but no-one else can create, drop,
 * read, etc.
 */
public class HBaseAuthorizationTest {
    
    private static int port;
    private static HBaseTestingUtility utility;
    
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        
        utility = new HBaseTestingUtility();
        utility.getConfiguration().set("test.hbase.zookeeper.property.clientPort", "" + port);
        utility.getConfiguration().set("zookeeper.znode.parent", "/hbase-unsecure");
        
        // Enable authorization
        utility.getConfiguration().set("hbase.security.authorization", "true");
        utility.getConfiguration().set("hbase.coprocessor.master.classes", 
                                       "org.apache.coheigea.bigdata.hbase.CustomMasterObserver");
        utility.getConfiguration().set("hbase.coprocessor.region.classes", 
                                       "org.apache.coheigea.bigdata.hbase.CustomRegionObserver");
        
        utility.startMiniCluster();
        
        // Create a table as the logged in user
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        // Create a table
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();

        // Create a table
        if (!admin.tableExists(TableName.valueOf("temp"))) {
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf("temp"));

            // Adding column families to table descriptor
            tableDescriptor.addFamily(new HColumnDescriptor("colfam1"));
            tableDescriptor.addFamily(new HColumnDescriptor("colfam2"));

            admin.createTable(tableDescriptor);
        }
        
        // Add a new row
        Put put = new Put(Bytes.toBytes("row1"));
        put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val1"));
        Table table = conn.getTable(TableName.valueOf("temp"));
        table.put(put);

        conn.close();
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        utility.shutdownMiniCluster();
    }
    
    @org.junit.Test
    public void testReadTablesAsProcessOwner() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();

        HTableDescriptor[] tableDescriptors = admin.listTables();
        Assert.assertEquals(1, tableDescriptors.length);

        conn.close();
    }
    
    @org.junit.Test
    public void testReadTablesAsBob() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        String user = "bob";
        if ("bob".equals(System.getProperty("user.name"))) {
            user = "alice";
        }
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection conn = ConnectionFactory.createConnection(conf);
                Admin admin = conn.getAdmin();
                
                try {
                    HTableDescriptor[] tableDescriptors = admin.listTables();
                    Assert.assertEquals(1, tableDescriptors.length);
                    Assert.fail("Failure expected on an unauthorized user");
                } catch (IOException ex) {
                    // expected
                }
        
                conn.close();
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testCreateAndDropTables() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();

        // Create a new table as process owner
        HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf("temp2"));

        // Adding column families to table descriptor
        tableDescriptor.addFamily(new HColumnDescriptor("colfam1"));
        tableDescriptor.addFamily(new HColumnDescriptor("colfam2"));

        admin.createTable(tableDescriptor);

        conn.close();
        
        // Try to disable + delete the table as "bob"
        String user = "bob";
        if ("bob".equals(System.getProperty("user.name"))) {
            user = "alice";
        }
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection conn = ConnectionFactory.createConnection(conf);
                Admin admin = conn.getAdmin();
                
                try {
                    admin.disableTable(TableName.valueOf("temp2"));
                    admin.deleteTable(TableName.valueOf("temp2"));
                    Assert.fail("Failure expected on an unauthorized user");
                } catch (IOException ex) {
                    // expected
                }
                
                conn.close();
                return null;
            }
        });
        
        // Now disable and delete as process owner
        conn = ConnectionFactory.createConnection(conf);
        admin = conn.getAdmin();
        admin.disableTable(TableName.valueOf("temp2"));
        admin.deleteTable(TableName.valueOf("temp2"));
        
        conn.close();
    }
    
    @org.junit.Test
    public void testReadRowAsProcessOwner() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(TableName.valueOf("temp"));
        
        // Read a row
        Get get = new Get(Bytes.toBytes("row1"));
        Result result = table.get(get);
        byte[] valResult = result.getValue(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"));
        Assert.assertTrue(Arrays.equals(valResult, Bytes.toBytes("val1")));
        
        conn.close();
    }
    
    @org.junit.Test
    public void testReadRowAsBob() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        String user = "bob";
        if ("bob".equals(System.getProperty("user.name"))) {
            user = "alice";
        }
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection conn = ConnectionFactory.createConnection(conf);
                Table table = conn.getTable(TableName.valueOf("temp"));
                
                // Read a row
                try {
                    Get get = new Get(Bytes.toBytes("row1"));
                    table.get(get);
                    Assert.fail("Failure expected on an unauthorized user");
                } catch (IOException ex) {
                    // expected
                }
                
                conn.close();
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testWriteRowAsProcessOwner() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(TableName.valueOf("temp"));
        
        // Add a new row
        Put put = new Put(Bytes.toBytes("row2"));
        put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val2"));
        table.put(put);
        
        conn.close();
    }
    
    @org.junit.Test
    public void testWriteRowAsBob() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        String user = "bob";
        if ("bob".equals(System.getProperty("user.name"))) {
            user = "alice";
        }
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection conn = ConnectionFactory.createConnection(conf);
                Table table = conn.getTable(TableName.valueOf("temp"));
                
                // Add a new row
                try {
                    Put put = new Put(Bytes.toBytes("row3"));
                    put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val2"));
                    table.put(put);
                    Assert.fail("Failure expected on an unauthorized user");
                } catch (IOException ex) {
                    // expected
                }
                
                conn.close();
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testDeleteRowAsProcessOwner() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(TableName.valueOf("temp"));
        
        // Add a new row
        Put put = new Put(Bytes.toBytes("row4"));
        put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val2"));
        table.put(put);
        
        // Delete the new row
        Delete delete = new Delete(Bytes.toBytes("row4"));
        table.delete(delete);
        
        conn.close();
    }
    
    @org.junit.Test
    public void testDeleteRowAsBob() throws Exception {
        final Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "" + port);
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        
        Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(TableName.valueOf("temp"));
        
        // Add a new row (as process owner)
        Put put = new Put(Bytes.toBytes("row5"));
        put.addColumn(Bytes.toBytes("colfam1"), Bytes.toBytes("col1"), Bytes.toBytes("val2"));
        table.put(put);
        
        String user = "bob";
        if ("bob".equals(System.getProperty("user.name"))) {
            user = "alice";
        }
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection conn = ConnectionFactory.createConnection(conf);
                Table table = conn.getTable(TableName.valueOf("temp"));
                
                try {
                    // Delete the new row
                    Delete delete = new Delete(Bytes.toBytes("row5"));
                    table.delete(delete);
                    Assert.fail("Failure expected on an unauthorized user");
                } catch (IOException ex) {
                    // expected
                }
                
                conn.close();
                return null;
            }
        });
        
        // Delete the new row (as process owner)
        Delete delete = new Delete(Bytes.toBytes("row5"));
        table.delete(delete);
        
        conn.close();
    }
}
