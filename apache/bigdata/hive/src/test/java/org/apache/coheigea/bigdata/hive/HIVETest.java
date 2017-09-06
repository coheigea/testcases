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

package org.apache.coheigea.bigdata.hive;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.server.HiveServer2;
import org.junit.Assert;

/**
 * A basic test that launches Hive, writes a table + then queries it
 */
public class HIVETest {
    
    private static final File hdfsBaseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static HiveServer2 hiveServer;
    private static int port;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        
        HiveConf conf = new HiveConf();
        
        // Warehouse
        File warehouseDir = new File("./target/hdfs/warehouse").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTOREWAREHOUSE.varname, warehouseDir.getPath());
        
        // Scratchdir
        File scratchDir = new File("./target/hdfs/scratchdir").getAbsoluteFile();
        conf.set("hive.exec.scratchdir", scratchDir.getPath());
     
        // Create a temporary directory for the Hive metastore
        File metastoreDir = new File("./target/metastore/").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTORECONNECTURLKEY.varname,
                 String.format("jdbc:derby:;databaseName=%s;create=true",  metastoreDir.getPath()));
        
        conf.set(HiveConf.ConfVars.METASTORE_AUTO_CREATE_ALL.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_THRIFT_PORT.varname, "" + port);
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_WEBUI_PORT.varname, "0");
        
        hiveServer = new HiveServer2();
        hiveServer.init(conf);
        hiveServer.start();
        
        Class.forName("org.apache.hive.jdbc.HiveDriver");
        
        // Load data into HIVE
        String url = "jdbc:hive2://localhost:" + port + "/default";
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();
        // statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("create table words (word STRING, count INT) row format delimited fields terminated by '\t' stored as textfile");
        
        // Copy "wordcount.txt" to "target" to avoid overwriting it during load
        java.io.File inputFile = new java.io.File(HIVEAuthorizerTest.class.getResource("../../../../../wordcount.txt").toURI());
        Path outputPath = Paths.get(inputFile.toPath().getParent().getParent().toString() + java.io.File.separator + "wordcountout.txt");
        Files.copy(inputFile.toPath(), outputPath);
        
        statement.execute("LOAD DATA INPATH '" + outputPath + "' OVERWRITE INTO TABLE words");
        
        // Just test to make sure it's working
        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals("Mr.", resultSet.getString(1));
        
        statement.close();
        connection.close();
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        hiveServer.stop();
        FileUtil.fullyDelete(hdfsBaseDir);
        File metastoreDir = new File("./target/metastore/").getAbsoluteFile();
        FileUtil.fullyDelete(metastoreDir);
    }
    
    @org.junit.Test
    public void testHiveQuery() throws Exception {
        
        // Load data into HIVE
        String url = "jdbc:hive2://localhost:" + port + "/default";
        Connection connection = DriverManager.getConnection(url, "alice", "alice");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals("Mr.", resultSet.getString(1));
        Assert.assertEquals(100, resultSet.getInt(2));

        statement.close();
        
    }
    
    
}
