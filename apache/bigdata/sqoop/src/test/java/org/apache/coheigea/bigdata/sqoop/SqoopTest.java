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

import static org.junit.Assert.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.sqoop.client.SqoopClient;
import org.apache.sqoop.model.MJob;
import org.apache.sqoop.model.MLink;

/**
 * A simple Sqoop test to create some links and a job
 */
public class SqoopTest {

    private static JettySqoopRunner jettySqoopRunner;
    private static Path tempDir;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        tempDir = Files.createTempDirectory("sqoop");
        jettySqoopRunner = new JettySqoopRunner(tempDir.toString(), "sqoopServer1", null, false);
        jettySqoopRunner.start();
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        jettySqoopRunner.stop();
        tempDir.toFile().deleteOnExit();
    }

    @org.junit.Test
    public void testCreateLinksAndJob() throws Exception {
        SqoopClient client = jettySqoopRunner.getSqoopClient("admin");

        assertFalse(client.getConnectors().isEmpty());

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

}
