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

package org.apache.coheigea.bigdata.kerberos.hive;

import java.io.File;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Create a KDC for use with securing Apache Hive. It contains the following principals:
 * a) hiveserver2/localhost
 * b) alice
 * c) hdfs/localhost
 * d) HTTP/localhost
 *
 * The krb.conf file with the (random) port that the KDC is running on is written out to target/krb5.conf
 *
 * Comment out the org.junit.Ignore annotation on the test below to run this KDC.
 */
public class HiveKerbyTest extends org.junit.Assert {

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        System.setProperty("sun.security.krb5.debug", "true");
        //System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("hadoop.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        //kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String hive = "hiveserver2/localhost@hadoop.apache.org";
        String alice = "alice@hadoop.apache.org";
        String hdfs = "hdfs/localhost@hadoop.apache.org";
        String http = "HTTP/localhost@hadoop.apache.org";

        kerbyServer.createPrincipal(hive, "hiveserver2");
        File keytabFile = new File(basedir + "/target/hiveserver2.keytab");
        kerbyServer.exportPrincipal(hive, keytabFile);

        kerbyServer.createPrincipal(alice, "alice");
        keytabFile = new File(basedir + "/target/alice.keytab");
        kerbyServer.exportPrincipal(alice, keytabFile);

        kerbyServer.createPrincipal(hdfs, "hdfs");
        kerbyServer.createPrincipal(http, "http");
        keytabFile = new File(basedir + "/target/hdfs.keytab");
        kerbyServer.exportPrincipal(hdfs, keytabFile);
        kerbyServer.exportPrincipal(http, keytabFile);

        kerbyServer.start();
    }

    @AfterClass
    public static void tearDown() throws KrbException {
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    @Test
    @org.junit.Ignore
    public void testHive() throws Exception {
        System.out.println("KDC ready on port: " + kerbyServer.getKdcSetting().getKdcUdpPort());
        Thread.sleep(3 * 10 * 60 * 1000);
    }

}
