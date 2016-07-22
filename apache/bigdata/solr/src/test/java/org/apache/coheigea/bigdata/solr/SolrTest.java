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

import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple test that starts Solr with a single core, adds a new document and queries it. 
 */
public class SolrTest extends org.junit.Assert {
    
    private EmbeddedSolrServer server;
  
    
    @Before
    public void setUp() throws Exception {
        CoreContainer container = new CoreContainer("target/test-classes/solr");
        container.load();

        server = new EmbeddedSolrServer(container, "core1" );
    }
    
    @After
    public void shutdown() throws Exception {
        server.close();
        FileUtils.deleteDirectory(Paths.get("target/data").toFile());
    }

    @Test
    public void testAddAndQuery() throws Exception {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // Add document
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("title", "Title of Doc");
        doc.addField("content", "Test Content");
        server.add(doc);
        server.commit();
        
        // Test it's uploaded
        params.set("q", "*");
        QueryResponse qResp = server.query(params);

        SolrDocumentList foundDocs = qResp.getResults();
        Assert.assertEquals(1, foundDocs.getNumFound());
        
        SolrDocument foundDoc = foundDocs.get(0);
        Assert.assertEquals("Title of Doc", foundDoc.getFieldValue("title"));
    }
    

}
