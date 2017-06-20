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
package org.apache.coheigea.cxf.kerberos.authentication;

import java.io.File;

import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbConstant;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbConfigKey;
import org.apache.kerby.kerberos.kerb.client.KrbPkinitClient;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.kerberos.kerb.type.ticket.SgtTicket;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Some tests for anonymous PKINIT
 */
public class AnonymousPKInitTest extends org.junit.Assert {

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setAllowUdp(true);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        // kerbyServer.getKdcConfig().setString(KdcConfigKey.PKINIT_IDENTITY, "myclient.cer");

        String pkinitIdentity = AnonymousPKInitTest.class.getResource("/kdccerttest.pem").getPath();
            // TODO private key should be required?+ AnonymousPKInitTest.class.getResource("/kdckey.pem").getPath();
        kerbyServer.getKdcConfig().setString(KdcConfigKey.PKINIT_IDENTITY, pkinitIdentity);
        kerbyServer.getKdcConfig().setBoolean(KdcConfigKey.PREAUTH_REQUIRED, Boolean.TRUE);

        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.createPrincipal(KrbConstant.ANONYMOUS_PRINCIPAL + "@service.ws.apache.org");
        kerbyServer.start();

        System.setProperty("java.security.krb5.conf", basedir + "/target/krb5.conf");
    }

    @AfterClass
    public static void tearDown() throws KrbException {
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }


    @org.junit.Test
    public void unitTest() throws Exception {
        KrbPkinitClient client = new KrbPkinitClient();

        client.setKdcHost("localhost");
        client.setKdcTcpPort(kerbyServer.getKdcPort());
        client.setAllowUdp(false);
        // client.setKdcUdpPort(Integer.parseInt(KDC_UDP_PORT));

        client.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        // client.getKrbConfig().setString(KrbConfigKey.PKINIT_ANCHORS, "myclient.cer");

        String pkinitAnchors = AnonymousPKInitTest.class.getResource("/cacerttest.pem").getPath();
        client.getKrbConfig().setString(KrbConfigKey.PKINIT_ANCHORS, pkinitAnchors);

        client.init();

        try {
            TgtTicket tgt = client.requestTgt();
            assertTrue(tgt != null);

            SgtTicket tkt = client.requestSgt(tgt, "bob/service.ws.apache.org@service.ws.apache.org");
            assertTrue(tkt != null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
