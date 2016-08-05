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

package org.apache.coheigea.bigdata.kms.ranger;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.hadoop.crypto.key.RangerKMSDB;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider;
import org.apache.hadoop.crypto.key.RangerMasterKey;
import org.apache.hadoop.crypto.key.kms.server.KMSConfiguration;
import org.apache.ranger.kms.dao.DaoManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * A test for the RangerMasterKey.
 */
public class RangerMasterKeyTest {
    
    @BeforeClass
    public static void startServers() throws Exception {
        DerbyTestUtils.startDerby();
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        DerbyTestUtils.stopDerby();
    }
    
    @org.junit.Test
    public void testRangerMasterKey() throws Throwable {
        
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());

        RangerKMSDB rangerkmsDb = new RangerKMSDB(RangerKeyStoreProvider.getDBKSConf());     
        DaoManager daoManager = rangerkmsDb.getDaoManager();
        
        String masterKeyPassword = "password0password0password0password0password0password0password0password0"
            + "password0password0password0password0password0password0password0password0password0password0"
            + "password0password0password0password0password0password0password0password0password0password0";
        
        RangerMasterKey rangerMasterKey = new RangerMasterKey(daoManager);
        Assert.assertTrue(rangerMasterKey.generateMasterKey(masterKeyPassword));
        Assert.assertNotNull(rangerMasterKey.getMasterKey(masterKeyPassword));
        
        try {
            rangerMasterKey.getMasterKey("badpass");
            Assert.fail("Failure expected on retrieving a key with the wrong password");
        } catch (Throwable t) {
            // expected
        }
        
        Assert.assertNotNull(rangerMasterKey.getMasterSecretKey(masterKeyPassword));
        
        try {
            rangerMasterKey.getMasterSecretKey("badpass");
            Assert.fail("Failure expected on retrieving a key with the wrong password");
        } catch (Throwable t) {
            // expected
        }
    }
}
