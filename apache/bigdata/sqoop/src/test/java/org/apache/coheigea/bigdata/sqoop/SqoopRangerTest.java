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

package org.apache.coheigea.bigdata.sqoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.sqoop.client.SqoopClient;
import org.apache.sqoop.common.SqoopException;
import org.apache.sqoop.model.MJob;
import org.apache.sqoop.model.MLink;

/**
 * A simple Sqoop test for authorization for Ranger:
 *  - the "admin" user can do anything with jobs, connectors, links.
 *  - "bob" can't do anything
 *  - "alice" can read/write links and read connectors and jobs, but nothing else.
 */
public class SqoopRangerTest {

    private static JettySqoopRunner jettySqoopRunner;
    private static Path tempDir;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        tempDir = Files.createTempDirectory("sqoop");
        jettySqoopRunner = new JettySqoopRunner(tempDir.toString(), "sqoopServer1", null, true);
        jettySqoopRunner.start();

        // Create some links, jobs via the admin
        SqoopClient client = jettySqoopRunner.getSqoopClient("admin");

        MLink rdbmsLink = client.createLink("generic-jdbc-connector");
        jettySqoopRunner.fillRdbmsLinkConfig(rdbmsLink);
        rdbmsLink.setName("JDBC_link1");
        jettySqoopRunner.saveLink(client, rdbmsLink);

        MLink hdfsLink = client.createLink("hdfs-connector");
        jettySqoopRunner.fillHdfsLink(hdfsLink);
        hdfsLink.setName("HDFS_link1");
        jettySqoopRunner.saveLink(client, hdfsLink);

        MJob job1 = client.createJob(hdfsLink.getName(), rdbmsLink.getName());
        jettySqoopRunner.fillHdfsFromConfig(job1);
        jettySqoopRunner.fillRdbmsToConfig(job1);
        // create job
        job1.setName("HDFS_JDBS_job1");
        jettySqoopRunner.saveJob(client, job1);
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        jettySqoopRunner.stop();
        tempDir.toFile().deleteOnExit();
    }

    // The admin user can do anything
    @org.junit.Test
    public void testAdminUser() throws Exception {
        SqoopClient client = jettySqoopRunner.getSqoopClient("admin");

        assertFalse(client.getConnectors().isEmpty());
        assertEquals(client.getLinks().size(), 2);
        assertEquals(client.getJobs().size(), 1);
    }

    // "bob" can't do anything
    @org.junit.Test
    public void testBobUser() throws Exception {
        SqoopClient client = jettySqoopRunner.getSqoopClient("bob");

        assertTrue(client.getConnectors().isEmpty());
        assertTrue(client.getLinks().isEmpty());
        assertTrue(client.getJobs().isEmpty());
    }

    // "alice" can create links and read connectors and jobs
    @org.junit.Test
    public void testAliceUser() throws Exception {
        SqoopClient client = jettySqoopRunner.getSqoopClient("alice");

        assertFalse(client.getConnectors().isEmpty());
        assertEquals(client.getLinks().size(), 2);
        assertEquals(client.getJobs().size(), 1);

        // Update one of the links
        MLink rdbmsLink = client.getLink("JDBC_link1");
        assertNotNull(rdbmsLink);
        rdbmsLink.setName("JDBC_link2");
        client.updateLink(rdbmsLink, "JDBC_link1");

        // Try to read the job
        MJob job1 = client.getJob("HDFS_JDBS_job1");
        assertNotNull(job1);
        job1.setName("HDFS_JDBS_job2");
        try {
            client.updateJob(job1, "HDFS_JDBS_job1");
            fail("Authorization failure expected");
        } catch (SqoopException ex) {
            // expected
        }
    }
}
