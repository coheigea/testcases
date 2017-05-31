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

package org.apache.coheigea.bigdata.kerberos.storm;

import java.io.File;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbConfigKey;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Create a KDC for use with securing Apache Storm. It contains the following principals:
 * a) zookeeper/localhost
 * b) zookeeper-client
 * c) storm/localhost
 * d) storm-client
 * e) alice
 * f) HTTP/localhost
 *
 * The krb.conf file with the (random) port that the KDC is running on is written out to target/krb5.conf
 *
 * Comment out the org.junit.Ignore annotation on the test below to run this KDC.
 */
public class StormKerbyTest extends org.junit.Assert {

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
        kerbyServer.getKdcConfig().setBoolean(KrbConfigKey.PREAUTH_REQUIRED, false);

        kerbyServer.setKdcRealm("storm.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        //kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String zookeeper = "zookeeper/localhost@storm.apache.org";
        String zookeeper_client = "zookeeper-client@storm.apache.org";
        String storm = "storm/localhost@storm.apache.org";
        String storm_client = "storm-client@storm.apache.org";
        String alice = "alice@storm.apache.org";
        String http = "HTTP/localhost@storm.apache.org";

        kerbyServer.createPrincipal(zookeeper, "zookeeper");
        File keytabFile = new File(basedir + "/target/zookeeper.keytab");
        kerbyServer.exportPrincipal(zookeeper, keytabFile);
        
        kerbyServer.createPrincipal(zookeeper_client, "zookeeper-client");
        keytabFile = new File(basedir + "/target/zookeeper_client.keytab");
        kerbyServer.exportPrincipal(zookeeper_client, keytabFile);
        
        kerbyServer.createPrincipal(storm, "storm");
        keytabFile = new File(basedir + "/target/storm.keytab");
        kerbyServer.exportPrincipal(storm, keytabFile);
        
        kerbyServer.createPrincipal(storm_client, "storm-client");
        keytabFile = new File(basedir + "/target/storm_client.keytab");
        kerbyServer.exportPrincipal(storm_client, keytabFile);
        
        kerbyServer.createPrincipal(alice, "alice");
        keytabFile = new File(basedir + "/target/alice.keytab");
        kerbyServer.exportPrincipal(alice, keytabFile);

        kerbyServer.createPrincipal(http, "http");
        keytabFile = new File(basedir + "/target/http.keytab");
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
    public void testKerberos() throws Exception {
        System.out.println("KDC ready on port: " + kerbyServer.getKdcSetting().getKdcUdpPort());
        Thread.sleep(2 * 10 * 60 * 1000);
    }

}
