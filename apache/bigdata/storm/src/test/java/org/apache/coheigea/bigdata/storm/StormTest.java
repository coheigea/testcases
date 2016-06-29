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
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;

/**
 * A simple test that wires a WordSpout + WordCounterBolt into a topology and runs it.
 */
public class StormTest {
    
    @org.junit.Test
    public void testStorm() throws Exception {
        TopologyBuilder builder = new TopologyBuilder();        
        builder.setSpout("words", new WordSpout());
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");
        
        Config conf = new Config();
        conf.put("nimbus.authorization.class", CustomIAuthorizer.class.getName());
        conf.put(Config.NIMBUS_AUTHORIZER, "org.apache.coheigea.bigdata.storm.CustomIAuthorizer");
        conf.put(Config.NIMBUS_IMPERSONATION_AUTHORIZER, CustomIAuthorizer.class.getName());
        conf.setDebug(true);
        
        LocalCluster cluster = new LocalCluster();
        
        cluster.submitTopology("word-count", conf, builder.createTopology());
        
        Utils.sleep(10000);
        
        cluster.killTopology("word-count");
        cluster.shutdown();
        
    }
    
}
