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
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;

/**
 * A test for HDFS permissions using Kerberos.
 */
@org.junit.Ignore
public class HDFSKerberosTest {

    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set("hadoop.security.authentication", "kerberos");
        conf.set("dfs.block.access.token.enable", "true");

        conf.set("dfs.namenode.keytab.file", "/home/colm/src/testcases/apache/bigdata/kerberos/target/hdfs.keytab");
        conf.set("dfs.namenode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");
        conf.set("dfs.namenode.kerberos.internal.spnego.principal", "HTTP/localhost@hadoop.apache.org");

        conf.set("dfs.secondary.namenode.keytab.file", "/home/colm/src/testcases/apache/bigdata/kerberos/target/hdfs.keytab");
        conf.set("dfs.secondary.namenode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");
        conf.set("dfs.secondary.namenode.kerberos.internal.spnego.principal", "HTTP/localhost@hadoop.apache.org");

        conf.set("dfs.datanode.data.dir.perm", "700");
        // TODO use random ports here
        conf.set("dfs.datanode.address", "0.0.0.0:10004");
        conf.set("dfs.datanode.http.address", "0.0.0.0:10006");
        conf.set("dfs.data.transfer.protection", "integrity");
        conf.set("dfs.http.policy", "HTTPS_ONLY");
        conf.set("dfs.web.authentication.kerberos.principal", "HTTP/localhost@hadoop.apache.org");
        conf.set("dfs.datanode.keytab.file", "/home/colm/src/testcases/apache/bigdata/kerberos/target/hdfs.keytab");
        conf.set("dfs.datanode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");

        // SSL Configuration
        conf.set("dfs.https.server.keystore.resource", "ssl-server.xml");

        System.setProperty("java.security.krb5.conf", "/home/colm/src/testcases/apache/bigdata/kerberos/target/krb5.conf");

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
        
        // Now try to read the file as "alice"
        final Configuration conf = new Configuration();
        conf.set("fs.defaultFS", defaultFs);
        UserGroupInformation.setConfiguration(conf);
        
        UserGroupInformation.loginUserFromKeytab("alice", "/home/colm/src/testcases/apache/bigdata/kerberos/target/alice.keytab");
        UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                
                FileSystem fs = FileSystem.get(conf);
                
                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                
                fs.close();
                return null;
            }
        });
        
    }


}
