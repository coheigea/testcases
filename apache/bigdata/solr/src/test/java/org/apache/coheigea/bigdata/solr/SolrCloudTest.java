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

package org.apache.coheigea.bigdata.solr;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple test that starts SolrCloud, adds a new document and queries it. 
 */
public class SolrCloudTest extends org.junit.Assert {
    
    private static MiniSolrCloudCluster server;
    private static Path tempDir;
  
    @BeforeClass
    public static void setUp() throws Exception {
        
        JettyConfig.Builder jettyConfig = JettyConfig.builder();
        jettyConfig.waitForLoadingCoresToFinish(null);

        String solrConfig = new String(Files.readAllBytes(Paths.get("target/test-classes/solrcloud/solr.xml")), Charset.defaultCharset());
        tempDir = Files.createTempDirectory("solrcloud");
        server = new MiniSolrCloudCluster(2, tempDir, solrConfig, jettyConfig.build());
        
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
    public void testAddAndQuery() throws Exception {
        CloudSolrClient cloudSolrClient = server.getSolrClient();
        
        cloudSolrClient.setDefaultCollection("docs");
        
        // Add document
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("title", "Title of Doc");
        doc.addField("content", "Test Content");
        
        cloudSolrClient.add(doc);
        cloudSolrClient.commit();
        
        ModifiableSolrParams params = new ModifiableSolrParams();
        // Test it's uploaded
        params.set("q", "*");
        QueryResponse qResp = cloudSolrClient.query(params);

        SolrDocumentList foundDocs = qResp.getResults();
        Assert.assertEquals(1, foundDocs.getNumFound());
        
        SolrDocument foundDoc = foundDocs.get(0);
        Assert.assertEquals("Title of Doc", foundDoc.getFieldValue("title"));
    }
    

}
