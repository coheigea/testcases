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
package org.apache.coheigea.camel.hdfs;

import java.io.File;

import org.apache.camel.spring.Main;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;

public class HDFSTest extends org.junit.Assert {
    
    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        conf.set("fs.defaultFS", "hdfs://localhost:43678");
        conf.set("dfs.namenode.http-address", "hdfs://localhost:43678");
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
        
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file
        final Path file = new Path("/tmp/tmpdir/data-file");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
        
        // Set the port so that it's available to the Camel context as a system property
        System.setProperty("port", defaultFs.substring(defaultFs.lastIndexOf(':') + 1));
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        FileUtil.fullyDelete(baseDir);
        hdfsCluster.shutdown();
    }
    
    @org.junit.Test
    public void testHDFS() throws Exception {
        // Start up the Camel route
        Main main = new Main();
        main.setApplicationContextUri("camel-hdfs.xml");
        
        main.start();
        
        // Sleep to allow time to copy the files etc.
        Thread.sleep(10 * 1000);
        
        main.stop();
    }
    

}
