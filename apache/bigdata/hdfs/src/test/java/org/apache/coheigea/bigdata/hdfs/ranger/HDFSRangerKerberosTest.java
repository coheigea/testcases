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

package org.apache.coheigea.bigdata.hdfs.ranger;

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
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer;
import org.junit.Assert;

/**
 * Here we plug the Ranger AccessControlEnforcer into HDFS. 
 * 
 * A custom RangerAdminClient is plugged into Ranger in turn, which loads security policies from a local file. These policies were 
 * generated in the Ranger Admin UI for a service called "HDFSTest". It contains three policies, each of which grants read, write and
 * execute permissions in turn to "/tmp/tmpdir", "/tmp/tmpdir2" and "/tmp/tmpdir3" to a user called "bob" and to a group called "IT".
 * 
 * In addition we have a TAG based policy, which grants "read" access to "bob" and the "IT" group to "/tmp/tmpdir6" (which is associated
 * with the tag called "TmpdirTag". A "hdfs_path" entity was created in Apache Atlas + then associated with the "TmpdirTag". This was
 * then imported into Ranger using the TagSyncService. The policies were then downloaded locally and saved for testing off-line.
 * 
 * Policies available from admin via:
 * 
 * http://localhost:6080/service/plugins/policies/download/cl1_hadoop
 */
@org.junit.Ignore
public class HDFSRangerKerberosTest {
    
    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        System.setProperty("java.security.krb5.conf", "/home/colm/src/testcases/apache/bigdata/kerberos/target/krb5.conf");
        
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

        conf.set("dfs.namenode.inode.attributes.provider.class", RangerHdfsAuthorizer.class.getName());
        
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
    public void readTest() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        
        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path("/tmp/tmpdir/data-file2");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();
        
        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
        
        // Now try to read the file as "bob" - this should be allowed (by the policy - user)
        final Configuration conf = new Configuration();
        conf.set("fs.defaultFS", defaultFs);
        UserGroupInformation.setConfiguration(conf);
        
        UserGroupInformation.loginUserFromKeytab("bob", "/home/colm/src/testcases/apache/bigdata/kerberos/target/bob.keytab");
        UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
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
        
    }
    
}
