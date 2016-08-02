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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider;
import org.apache.hadoop.crypto.key.kms.server.KMSConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * Policies available from admin via:
 * 
 * http://localhost:6080/service/plugins/policies/download/KMSTest
 */
@Ignore
public class KMSRangerTest {
    
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
    public void testCreateKey() throws Throwable {
        
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());
        
        Configuration conf = new Configuration();
        RangerKeyStoreProvider keyProvider = new RangerKeyStoreProvider(conf);
        
        /*
        ServletContextEvent servletContextEvent = EasyMock.createMock(ServletContextEvent.class);
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(servletContextEvent.getServletContext()).andReturn(servletContext).anyTimes();
        EasyMock.replay(servletContextEvent);
        
        
        KMSWebApp kmsWebapp = new KMSWebApp();
        kmsWebapp.contextInitialized(servletContextEvent);
        */
        /*
        KMS rangerKMS = new KMS();
        Map<String, String> jsonKeys = new HashMap<>();
        jsonKeys.put(KMSRESTConstants.NAME_FIELD, "newkey");
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        
        rangerKMS.createKey(jsonKeys, request);
        */
        
        // tempDir.toFile().deleteOnExit();
    }
    /*
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    @org.junit.Test
    public void testCreateKey() throws Exception {
        final Configuration conf = new Configuration();
        KeyProvider kp = 
            new UserProvider.Factory().createProvider(new URI("user:///"), conf);
        
        KeyACLs rangerAuthorizer = new RangerKmsAuthorizer();
        /*
        when(mock.isACLPresent("foo", KeyOpType.MANAGEMENT)).thenReturn(true);
        
        UserGroupInformation u1 = UserGroupInformation.createRemoteUser("u1");
       // when(mock.hasAccessToKey("foo", u1, KeyOpType.MANAGEMENT)).thenReturn(true);
        final KeyProviderCryptoExtension kpExt =
            new KeyAuthorizationKeyProvider(
                KeyProviderCryptoExtension.createKeyProviderCryptoExtension(kp),
                rangerAuthorizer);

        u1.doAs(
            new PrivilegedExceptionAction<Void>() {
              @Override
              public Void run() throws Exception {
                try {
                  byte[] seed = new byte[16];
                  SECURE_RANDOM.nextBytes(seed);
                  kpExt.createKey("foo", seed, newOptions(conf));
                } catch (IOException ioe) {
                  Assert.fail("User should be Authorized !!");
                }

                return null;
              }
            }
            );
    }
    
    private static KeyProvider.Options newOptions(Configuration conf) {
        KeyProvider.Options options = new KeyProvider.Options(conf);
        options.setCipher("AES");
        options.setBitLength(128);
        return options;
      }
    
    */
}
