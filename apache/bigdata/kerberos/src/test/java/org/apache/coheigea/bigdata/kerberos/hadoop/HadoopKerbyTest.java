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

package org.apache.coheigea.bigdata.kerberos.hadoop;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.util.NetworkUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Create a KDC for use with securing HDFS. It contains the following principals:
 * a) hdfs/localhost
 * b) HTTP/localhost
 * c) alice
 * d) bob
 *
 * alice and bob are written out to "target/alice.keytab" and "target/bob.keytab". The hdfs/localhost + HTTP/localhost principals
 * are written out to "target/hdfs.keytab".
 *
 * The krb.conf file with the (random) port that the KDC is running on is written out to target/test-classes/hadoop.krb5.conf
 *
 * Comment out the org.junit.Ignore annotation on the test below to run this KDC.
 */
public class HadoopKerbyTest extends org.junit.Assert {

    private static KerbyServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        System.setProperty("sun.security.krb5.debug", "true");
        //System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");

        kerbyServer = new KerbyServer();

        kerbyServer.setKdcHost("localhost");
        kerbyServer.setKdcRealm("hadoop.apache.org");
        kerbyServer.setKdcTcpPort(NetworkUtil.getServerPort());
        kerbyServer.setAllowUdp(true);
        kerbyServer.setKdcUdpPort(NetworkUtil.getServerPort());

        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String alice = "alice@hadoop.apache.org";
        String bob = "bob@hadoop.apache.org";
        String hdfs = "hdfs/localhost@hadoop.apache.org";
        String http = "HTTP/localhost@hadoop.apache.org";

        kerbyServer.createPrincipal(alice, "alice", "alice");
        kerbyServer.createPrincipal(bob, "bob", "bob");
        kerbyServer.createPrincipal(hdfs, "hdfs", "hdfs");
        kerbyServer.createPrincipal(http, "http", "hdfs");
        kerbyServer.createPrincipal("krbtgt/hadoop.apache.org@hadoop.apache.org", "krbtgt", null);

        kerbyServer.start();

        // Read in krb5.conf and substitute in the correct port
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/org/apache/coheigea/bigdata/kerberos/hadoop/krb5.conf");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        content = content.replaceAll("port", "" + kerbyServer.getKdcSetting().getKdcUdpPort());

        Path path2 = FileSystems.getDefault().getPath(basedir,
                                                      "/target/test-classes/hadoop.krb5.conf");
        Files.write(path2, content.getBytes());
    }

    @AfterClass
    public static void tearDown() throws KrbException {
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    @Test
    @org.junit.Ignore
    public void testKerberos() throws Exception {
        System.out.println("KDC ready on port: " + kerbyServer.getKdcSetting().getKdcUdpPort());
        Thread.sleep(10 * 60 * 1000);
    }

}
