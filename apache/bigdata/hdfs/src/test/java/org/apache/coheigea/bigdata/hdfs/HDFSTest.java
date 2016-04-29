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
import java.io.IOException;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Assert;

public class HDFSTest {
    
    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
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
    public void basicHDFSTest() throws Exception {
        
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file
        final Path file = new Path("/tmp/tmpdir/data-file");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Read the file
        FSDataInputStream in = fileSystem.open(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(in, output);
        String content = new String(output.toByteArray());
        Assert.assertTrue(content.startsWith("data0"));
        
        // Delete the file
        Assert.assertTrue(fileSystem.delete(file, false));
        
        // Try to read the file again..
        try {
            fileSystem.open(file);
            Assert.fail("Failure expected on trying to read a deleted file");
        } catch (IOException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void defaultPermissionsTest() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file
        final Path file = new Path("/tmp/tmpdir/data-file2");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Check status
        // FileStatus status = fileSystem.getFileStatus(file);
        // System.out.println("OWNER: " + status.getOwner());
        // System.out.println("GROUP: " + status.getGroup());
        // System.out.println("PERM: " + status.getPermission().toString());
        // fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
        // fileSystem.setOwner(file, "bob", null);
        
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
                
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testChangedPermissionsTest() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file
        final Path file = new Path("/tmp/tmpdir/data-file3");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
        
        // Now try to read the file as "bob" - this should fail
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                // Read the file
                try {
                    FSDataInputStream in = fs.open(file);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    IOUtils.copy(in, output);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                } 
                
                return null;
            }
        });
        
    }
    
    @org.junit.Test
    public void testDirectoryPermissions() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file
        final Path file = new Path("/tmp/tmpdir/data-file4");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Try to read the directory as "bob" - this should be allowed
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                RemoteIterator<LocatedFileStatus> iter = fs.listFiles(file.getParent(), false);
                Assert.assertTrue(iter.hasNext());
                
                return null;
            }
        });
        
        // Change permissions so that the directory can't be read by "other"
        fileSystem.setPermission(file.getParent(), new FsPermission(FsAction.ALL, FsAction.READ, FsAction.NONE));
        
        // Try to read the base directory as the file owner
        RemoteIterator<LocatedFileStatus> iter = fileSystem.listFiles(file.getParent(), false);
        Assert.assertTrue(iter.hasNext());
        
        // Now try to read the directory as "bob" again - this should fail
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                
                try {
                    RemoteIterator<LocatedFileStatus> iter = fs.listFiles(file.getParent(), false);
                    Assert.assertTrue(iter.hasNext());
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                } 
                
                return null;
            }
        });
    }
    
}
