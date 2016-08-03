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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.crypto.key.KeyProvider.KeyVersion;
import org.apache.hadoop.crypto.key.KeyProvider.Options;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider;
import org.apache.hadoop.crypto.key.kms.server.KMSConfiguration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * A test for the RangerKeyStoreProvider, which is an implementation of the Hadoop KeyProvider interface, which stores keys in a database.
 * Apache Derby is used to create the relevant tables to store the keys in for this test.
 */
public class RangerKeyStoreProviderTest {
    
    private static Connection conn;
    
    @BeforeClass
    public static void startServers() throws Exception {
        // Start Apache Derby
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        
        Properties props = new Properties();
        Connection conn = DriverManager.getConnection("jdbc:derby:memory:derbyDB;create=true", props);
        
        Statement statement = conn.createStatement();
        statement.execute("CREATE SCHEMA KMSADMIN");
        
        statement.execute("SET SCHEMA KMSADMIN");
        
        // Create masterkey table
        statement.execute("CREATE SEQUENCE RANGER_MASTERKEY_SEQ START WITH 1 INCREMENT BY 1");
        String tableCreationString = "CREATE TABLE ranger_masterkey (id VARCHAR(20) NOT NULL PRIMARY KEY, create_time DATE,"
            + "update_time DATE, added_by_id VARCHAR(20), upd_by_id VARCHAR(20),"
            + "cipher VARCHAR(255), bitlength VARCHAR(11), masterkey VARCHAR(2048))";
        statement.execute(tableCreationString);
        
        // Create keys table
        statement.execute("CREATE SEQUENCE RANGER_KEYSTORE_SEQ START WITH 1 INCREMENT BY 1");
        statement.execute("CREATE TABLE ranger_keystore(id VARCHAR(20) NOT NULL PRIMARY KEY, create_time DATE,"
            + "update_time DATE, added_by_id VARCHAR(20), upd_by_id VARCHAR(20),"
            + "kms_alias VARCHAR(255) NOT NULL, kms_createdDate VARCHAR(20), kms_cipher VARCHAR(255),"
            + "kms_bitLength VARCHAR(20), kms_description VARCHAR(512), kms_version VARCHAR(20),"
            + "kms_attributes VARCHAR(1024), kms_encoded VARCHAR(2048))");

    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        // Shut Derby down
        if (conn != null) {
            conn.close();
        }
        try {
            DriverManager.getConnection("jdbc:derby:memory:derbyDB;drop=true");
        } catch (SQLException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testCreateDeleteKey() throws Throwable {
        
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());
        
        Configuration conf = new Configuration();
        RangerKeyStoreProvider keyProvider = new RangerKeyStoreProvider(conf);
        
        // Create a key
        Options options = new Options(conf);
        options.setBitLength(128);
        options.setCipher("AES");
        KeyVersion keyVersion = keyProvider.createKey("newkey1", options);
        Assert.assertEquals("newkey1", keyVersion.getName());
        Assert.assertEquals(128 / 8, keyVersion.getMaterial().length);
        Assert.assertEquals("newkey1@0", keyVersion.getVersionName());
        
        keyProvider.flush();
        Assert.assertEquals(1, keyProvider.getKeys().size());
        keyProvider.deleteKey("newkey1");
        
        keyProvider.flush();
        Assert.assertEquals(0, keyProvider.getKeys().size());
        
        // Try to delete a key that isn't there
        try {
            keyProvider.deleteKey("newkey2");
            Assert.fail("Failure expected on trying to delete an unknown key");
        } catch (IOException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testRolloverKey() throws Throwable {
        
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());
        
        Configuration conf = new Configuration();
        RangerKeyStoreProvider keyProvider = new RangerKeyStoreProvider(conf);
        
        // Create a key
        Options options = new Options(conf);
        options.setBitLength(192);
        options.setCipher("AES");
        KeyVersion keyVersion = keyProvider.createKey("newkey1", options);
        Assert.assertEquals("newkey1", keyVersion.getName());
        Assert.assertEquals(192 / 8, keyVersion.getMaterial().length);
        Assert.assertEquals("newkey1@0", keyVersion.getVersionName());
        
        keyProvider.flush();
        
        // Rollover a new key
        byte[] oldKey = keyVersion.getMaterial();
        keyVersion = keyProvider.rollNewVersion("newkey1");
        Assert.assertEquals("newkey1", keyVersion.getName());
        Assert.assertEquals(192 / 8, keyVersion.getMaterial().length);
        Assert.assertEquals("newkey1@1", keyVersion.getVersionName());
        Assert.assertFalse(Arrays.equals(oldKey, keyVersion.getMaterial()));
        
        keyProvider.deleteKey("newkey1");
        
        keyProvider.flush();
        Assert.assertEquals(0, keyProvider.getKeys().size());
        
    }
    
    
}
