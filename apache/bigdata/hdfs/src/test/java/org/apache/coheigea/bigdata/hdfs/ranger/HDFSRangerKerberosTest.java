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
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

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
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.util.NetworkUtil;
import org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer;
import org.apache.ranger.authorization.hadoop.exceptions.RangerAccessControlException;
import org.junit.Assert;

/**
 * Here we plug the Ranger AccessControlEnforcer into HDFS, authenticating to HDFS using Kerberos.
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

        conf.set("dfs.namenode.inode.attributes.provider.class", RangerHdfsAuthorizer.class.getName());

        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
    }

    private static void configureKerby(String baseDir) throws Exception {

        //System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.conf", baseDir + "/target/krb5.conf");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("hadoop.apache.org");
        kerbyServer.setAllowUdp(true);
        kerbyServer.setWorkDir(new File(baseDir + "/target"));

        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String alice = "alice@hadoop.apache.org";
        String bob = "bob@hadoop.apache.org";
        String dave = "dave@hadoop.apache.org";
        String hdfs = "hdfs/localhost@hadoop.apache.org";
        String http = "HTTP/localhost@hadoop.apache.org";

        kerbyServer.createPrincipal(alice, "alice");
        File keytabFile = new File(baseDir + "/target/alice.keytab");
        kerbyServer.exportPrincipal(alice, keytabFile);

        kerbyServer.createPrincipal(bob, "bob");
        keytabFile = new File(baseDir + "/target/bob.keytab");
        kerbyServer.exportPrincipal(bob, keytabFile);

        kerbyServer.createPrincipal(dave, "dave");
        keytabFile = new File(baseDir + "/target/dave.keytab");
        kerbyServer.exportPrincipal(dave, keytabFile);

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

        UserGroupInformation.loginUserFromKeytab("hdfs/localhost", basedir + "/target/hdfs.keytab");
        UserGroupInformation ugi = UserGroupInformation.getLoginUser();
        System.out.println("UGI: " + ugi.getUserName());

        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                System.out.println("HERE: " + System.getProperty("java.security.krb5.conf"));
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
        ugi.logoutUserFromKeytab();
/*
        // Now try to read the file as known user "dave" - this should not be allowed, as he doesn't have the correct permissions
        UserGroupInformation ugi = UserGroupInformation.getLoginUser();
        ugi.logoutUserFromKeytab();
        UserGroupInformation.loginUserFromKeytab("dave", basedir + "/target/dave.keytab");
        ugi = UserGroupInformation.getCurrentUser();
        System.out.println("UGI: " + ugi.getUserName());
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (RemoteException ex) {
                    // expected
                    Assert.assertTrue(RangerAccessControlException.class.getName().equals(ex.getClassName()));
                }

                fs.close();
                return null;
            }
        });
        ugi.logoutUserFromKeytab();
        */
    }

}
