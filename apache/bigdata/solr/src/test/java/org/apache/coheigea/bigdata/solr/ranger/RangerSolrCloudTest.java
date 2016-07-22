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

package org.apache.coheigea.bigdata.solr.ranger;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple test that starts SolrCloud, adds a new document and queries it. It also plugs in the RangerSolrAuthorizer with the following policies:
 * 
 * a) "bob" has all privileges on the "docs" collection
 * b) "alice" and the "IT" group can only query the "docs" collection
 * c) The "Legal" group can only query the "docs" collection from the IP 127.0.0.*
 * 
 */
public class RangerSolrCloudTest extends org.junit.Assert {
    
    private static MiniSolrCloudCluster server;
    private static Path tempDir;
  
    @BeforeClass
    public static void setUp() throws Exception {
        
        JettyConfig.Builder jettyConfig = JettyConfig.builder();
        jettyConfig.waitForLoadingCoresToFinish(null);

        String solrConfig = new String(Files.readAllBytes(Paths.get("target/test-classes/solrcloud/solr.xml")), Charset.defaultCharset());
        tempDir = Files.createTempDirectory("solrcloud");
        server = new MiniSolrCloudCluster(2, tempDir, solrConfig, jettyConfig.build());
        
        // Insert the RangerSolrAuthorizer + BasicAuthPlugin
        try (ZkStateReader zkStateReader = new ZkStateReader( server.getZkServer().getZkAddress(),
                                                              10000, 10000)) {
            zkStateReader.getZkClient().delete(ZkStateReader.SOLR_SECURITY_CONF_PATH, 0, true);
            String securityJson = new String(Files.readAllBytes(Paths.get("src/test/resources/security.json")), Charset.defaultCharset());
            zkStateReader.getZkClient().create(ZkStateReader.SOLR_SECURITY_CONF_PATH, securityJson.getBytes(Charset.defaultCharset()),
                                               CreateMode.PERSISTENT, true);
        }
        
        String configName = "core1Config";
        File configDir = Paths.get("target/test-classes/solrcloud").toFile();
        server.uploadConfigDir(configDir, configName);
        
        Map<String, String> collectionProperties = new HashMap<>();
        collectionProperties.put("config", "solrconfig.xml");
        collectionProperties.put("schema", "schema.xml");
        
        server.createCollection("docs", 1, 1, configName, collectionProperties);
        
        JettySolrRunner startedServer = server.startJettySolrRunner();
        assertTrue(startedServer.isRunning());
        
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null) {
            server.shutdown();
        }
        tempDir.toFile().deleteOnExit();
    }

    @Test
    public void testReadPrivilege() throws Exception {
        performQuery("alice", null, false);
        performQuery("bob", null, false);
        performQuery("eve", null, true);
        performQuery("frank", "IT", false);
        performQuery("helen", "finance", true);
    }
    
    private void performQuery(String user, String group, boolean exceptionExpected) throws Exception {
        final CloudSolrClient cloudSolrClient = server.getSolrClient();
        cloudSolrClient.setDefaultCollection("docs");
        
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", "*");

        final QueryRequest queryRequest = new QueryRequest(params);
        queryRequest.setBasicAuthCredentials(user, "SolrRocks"); 

        try {
            if (group != null) {
                UserGroupInformation ugi = UserGroupInformation.createUserForTesting(user, new String[] {group});
                ugi.doAs(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws Exception {
                        cloudSolrClient.request(queryRequest);
                        return null;
                    }
                });
            } else {
                cloudSolrClient.request(queryRequest);
            }
        } catch (Exception ex) {
            if (!exceptionExpected) {
                throw ex;
            }
            return;
        }
        
        Assert.assertFalse(exceptionExpected);
    }

}
