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
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.server.HiveServer2;
import org.junit.Assert;

/**
 * Here we plug a custom HiveAuthorizer into HIVE. It enforces the following rules:
 *   a) The logged in user can do anything
 *   b) "bob" can do a select on the tables
 *   c) "alice" can do a select only on the "count" column
 */
public class HIVEAuthorizerTest {

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
        File metastoreDir = new File("./target/authzmetastore/").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTORECONNECTURLKEY.varname,
                 String.format("jdbc:derby:;databaseName=%s;create=true",  metastoreDir.getPath()));

        conf.set(HiveConf.ConfVars.METASTORE_AUTO_CREATE_ALL.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_THRIFT_PORT.varname, "" + port);

        // Enable authorization
        conf.set(HiveConf.ConfVars.HIVE_AUTHORIZATION_ENABLED.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER.varname,
                 "org.apache.coheigea.bigdata.hive.CustomHiveAuthorizerFactory");
        //conf.set(HiveConf.ConfVars.HIVE_METASTORE_AUTHORIZATION_MANAGER.varname,
        //         "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_WEBUI_PORT.varname, "0");

        hiveServer = new HiveServer2();
        hiveServer.init(conf);
        hiveServer.start();

        Class.forName("org.apache.hive.jdbc.HiveDriver");

        String processOwner = System.getProperty("user.name");

        // Create database
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, processOwner, processOwner);
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE authz");

        statement.close();
        connection.close();

        // Load data into HIVE
        String url = "jdbc:hive2://localhost:" + port + "/authz";
        connection = DriverManager.getConnection(url, processOwner, processOwner);
        statement = connection.createStatement();
        // statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("create table words (word STRING, count INT) row format delimited fields terminated by '\t'");

        // Copy "wordcount.txt" to "target" to avoid overwriting it during load
        java.io.File inputFile = new java.io.File(HIVEAuthorizerTest.class.getResource("../../../../../wordcount.txt").toURI());
        Path outputPath = Paths.get(inputFile.toPath().getParent().getParent().toString() + java.io.File.separator + "wordcountout.txt");
        if (!outputPath.toFile().exists()) {
            Files.copy(inputFile.toPath(), outputPath);
        }

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
        File metastoreDir = new File("./target/authzmetastore/").getAbsoluteFile();
        FileUtil.fullyDelete(metastoreDir);
    }

    @org.junit.Test
    public void testHiveSelectAllAsProcessOwner() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        String user = System.getProperty("user.name");
        Connection connection = DriverManager.getConnection(url, user, user);
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals("Mr.", resultSet.getString(1));
        Assert.assertEquals(100, resultSet.getInt(2));

        statement.close();
        connection.close();
    }

    @org.junit.Test
    public void testHiveSelectAllAsBob() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals("Mr.", resultSet.getString(1));
        Assert.assertEquals(100, resultSet.getInt(2));

        statement.close();
        connection.close();
    }

    @org.junit.Test
    public void testHiveSelectAllAsAlice() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        Connection connection = DriverManager.getConnection(url, "alice", "alice");
        Statement statement = connection.createStatement();

        try {
            statement.executeQuery("SELECT * FROM words where count == '100'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();
    }

    @org.junit.Test
    public void testHiveSelectSpecificColumnAsBob() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT count FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals(100, resultSet.getInt(1));

        statement.close();
        connection.close();
    }

    @org.junit.Test
    public void testHiveSelectSpecificColumnAsAlice() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        Connection connection = DriverManager.getConnection(url, "alice", "alice");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT count FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals(100, resultSet.getInt(1));

        statement.close();
        connection.close();
    }

    @org.junit.Test
    public void testHiveSelectSpecificColumnAsEve() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/authz";
        Connection connection = DriverManager.getConnection(url, "eve", "eve");
        Statement statement = connection.createStatement();

        try {
            statement.executeQuery("SELECT count FROM words where count == '100'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();
    }

}
