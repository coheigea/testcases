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

import java.io.File;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;

public class HDFSTest {
    
    @org.junit.Test
    public void simpleTest() throws Exception {
        
        File baseDir = new File("./target/hdfs/").getAbsoluteFile();
        // FileUtil.fullyDelete(baseDir);
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        MiniDFSCluster hdfsCluster = builder.build();
        // String hdfsURI = "hdfs://localhost:"+ hdfsCluster.getNameNodePort() + "/";
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        final String defaultFs = conf.get("fs.defaultFS");
        
        // Write a file
        final Path file = new Path(new File("target/hdfs/data-file").getAbsolutePath());
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Set permission
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
        // fileSystem.setOwner(file, "bob", null);
        
        // Read the file
        // FSDataInputStream in = fileSystem.open(file);
        
        // Print it out
        // IOUtils.copy(in, System.out);
        
        // Check status
        FileStatus status = fileSystem.getFileStatus(file);
        System.out.println("OWNER: " + status.getOwner());
        System.out.println("PERM: " + status.getPermission().toString());
        
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);
                
                FileSystem fs = FileSystem.get(conf);
                FSDataInputStream in = fs.open(file);
                
                // Print it out
                IOUtils.copy(in, System.out);
                
                return null;
            }
        });
    }
    
    // TODO teardown, shutdown the cluster. Move the creation above to a setUp
}
