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

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;

/**
 * A simple test that wires a WordSpout + WordCounterBolt into a topology and runs it.
 * This class is used to deploy the topology to a Storm cluster.
 *
 * Deploy it via:
 *
 * mvn assembly:assembly
 * bin/storm jar org.apache.coheigea.bigdata.storm.StormMain target/bigdata-storm-demo-1.0-jar-with-dependencies.jar <path to words.txt>
 */
public class StormMain {

    public static void main(String[] args) throws Exception {
        final TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("words", new WordSpout(args[0]));
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");

        final Config conf = new Config();
        conf.setDebug(true);

        StormSubmitter.submitTopology("mytopology", conf, builder.createTopology());

    }

}
