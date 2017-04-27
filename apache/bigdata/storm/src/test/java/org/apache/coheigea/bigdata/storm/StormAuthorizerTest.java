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

package org.apache.coheigea.bigdata.storm;

import java.net.URI;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;

/**
 * A simple test that wires a WordSpout + WordCounterBolt into a topology and runs it. We're also plugging in the CustomIAuthorizer,
 * which allows access to "alice" but no-one else.
 */
public class StormAuthorizerTest {

    private static LocalCluster cluster;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        System.setProperty("storm.conf.file", "storm_customauth.yaml");
        cluster = new LocalCluster();
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        cluster.shutdown();
        System.clearProperty("storm.conf.file");
    }

    @org.junit.Test
    public void testCreateTopologyAlice() throws Exception {
        final Config conf = new Config();
        conf.setDebug(true);

        final TopologyBuilder builder = new TopologyBuilder();
        URI fileName = StormAuthorizerTest.class.getResource("../../../../../words.txt").toURI();
        builder.setSpout("words", new WordSpout(fileName));
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");

        // Alice can create a new topology
        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("alice"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                cluster.submitTopology("word-count", conf, builder.createTopology());
                return null;
            }
        });

        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                cluster.killTopology("word-count");
                return null;
            }
        });

    }

    private static class SimplePrincipal implements Principal {

        private final String name;

        public SimplePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }
}
