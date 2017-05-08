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
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.util.NetworkUtil;
import org.junit.Assert;

/**
 * A HDFSTest, where we are setting up HDFS to use kerberos for authentication, and launch a KDC (Apache Kerby) as well.
 */
public class HDFSKerberosTest {

    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    private static SimpleKdcServer kerbyServer;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        configureKerby(basedir);

        Configuration conf = new Configuration();
        conf.set("hadoop.security.authentication", "kerberos");
        conf.set("dfs.block.access.token.enable", "true");

        conf.set("dfs.namenode.keytab.file", basedir + "/target/hdfs.keytab");
        conf.set("dfs.namenode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");
        conf.set("dfs.namenode.kerberos.internal.spnego.principal", "HTTP/localhost@hadoop.apache.org");

        conf.set("dfs.secondary.namenode.keytab.file", basedir + "/target/hdfs.keytab");
        conf.set("dfs.secondary.namenode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");
        conf.set("dfs.secondary.namenode.kerberos.internal.spnego.principal", "HTTP/localhost@hadoop.apache.org");

        conf.set("dfs.datanode.data.dir.perm", "700");
        conf.set("dfs.datanode.address", "0.0.0.0:" + NetworkUtil.getServerPort());
        conf.set("dfs.datanode.http.address", "0.0.0.0:" + NetworkUtil.getServerPort());
        conf.set("dfs.data.transfer.protection", "integrity");
        conf.set("dfs.http.policy", "HTTPS_ONLY");
        conf.set("dfs.web.authentication.kerberos.principal", "HTTP/localhost@hadoop.apache.org");
        conf.set("dfs.datanode.keytab.file", basedir + "/target/hdfs.keytab");
        conf.set("dfs.datanode.kerberos.principal", "hdfs/localhost@hadoop.apache.org");

        // SSL Configuration
        conf.set("dfs.https.server.keystore.resource", "ssl-server.xml");

        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
    }

    private static void configureKerby(String baseDir) throws Exception {

        //System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.conf", baseDir + "/target/krb5.conf");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("hadoop.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(baseDir + "/target"));

        kerbyServer.init();

        // Create principals
        String alice = "alice@hadoop.apache.org";
        String bob = "bob@hadoop.apache.org";
        String hdfs = "hdfs/localhost@hadoop.apache.org";
        String http = "HTTP/localhost@hadoop.apache.org";

        kerbyServer.createPrincipal(alice, "alice");
        File keytabFile = new File(baseDir + "/target/alice.keytab");
        kerbyServer.exportPrincipal(alice, keytabFile);

        kerbyServer.createPrincipal(bob, "bob");
        keytabFile = new File(baseDir + "/target/bob.keytab");
        kerbyServer.exportPrincipal(bob, keytabFile);

        kerbyServer.createPrincipal(hdfs, "hdfs");
        kerbyServer.createPrincipal(http, "http");
        keytabFile = new File(baseDir + "/target/hdfs.keytab");
        kerbyServer.exportPrincipal(hdfs, keytabFile);
        kerbyServer.exportPrincipal(http, keytabFile);

        kerbyServer.start();
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        FileUtil.fullyDelete(baseDir);
        hdfsCluster.shutdown();
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
        System.clearProperty("java.security.krb5.conf");
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
        conf.set("hadoop.security.authentication", "kerberos");
        UserGroupInformation.setConfiguration(conf);

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
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
