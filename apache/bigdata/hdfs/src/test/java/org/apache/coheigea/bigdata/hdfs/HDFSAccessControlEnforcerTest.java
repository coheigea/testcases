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

package org.apache.coheigea.bigdata.hdfs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributes;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Assert;

/**
 * Here we plug a custom AccessControlEnforcer into HDFS.
 */
public class HDFSAccessControlEnforcerTest {
    
    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        conf.set("dfs.namenode.inode.attributes.provider.class", CustomINodeAttributeProvider.class.getName());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        FileUtil.fullyDelete(baseDir);
        hdfsCluster.shutdown();
    }
    
    @org.junit.Test
    public void customPermissionsTest() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path("/tmp/tmpdir/data-file2");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Now try to read the file as "bob" - this should be allowed
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                String content = new String(output.toByteArray());
                Assert.assertTrue(content.startsWith("data0"));
                
                fs.close();
                return null;
            }
        });
        
        // Now try to read the file as "eve" - this should not be allowed
        ugi = UserGroupInformation.createRemoteUser("eve");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                }
                
                fs.close();
                return null;
            }
        });
        
        // Write to the file as the owner, this should be allowed
        out = fileSystem.append(file);
        out.write(("new data\n").getBytes("UTF-8"));
        out.flush();
        out.close();
        
        // Now try to write to the file as "bob" - this should not be allowed
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                // Write to the file
                try {
                    fs.append(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                }
                
                fs.close();
                return null;
            }
        });
    }
 
    
    private static class CustomINodeAttributeProvider extends INodeAttributeProvider {

        @Override
        public INodeAttributes getAttributes(String[] arg0, INodeAttributes arg1) {
            return arg1;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
        
        @Override
        public AccessControlEnforcer getExternalAccessControlEnforcer(AccessControlEnforcer defaultEnforcer) {
            return new CustomAccessControlEnforcer();
        }
        
        /**
         * A trivial AccessControlEnforcer that allows "bob" the ability to read any file, but no-one else
         */
        private static class CustomAccessControlEnforcer implements AccessControlEnforcer {

            @Override
            public void checkPermission(String fsOwner, String superGroup, UserGroupInformation ugi,
                                        INodeAttributes[] inodeAttrs, INode[] inodes, byte[][] pathByNameArr,
                                        int snapshotId, String path, int ancestorIndex, boolean doCheckOwner,
                                        FsAction ancestorAccess, FsAction parentAccess, FsAction access,
                                        FsAction subAccess, boolean ignoreEmptyDir) throws AccessControlException {
                if (!("bob".equals(ugi.getUserName()) && access.equals(FsAction.READ))) {
                    throw new AccessControlException("Access denied");
                }
                
            }

            
        }
        
    }
}
