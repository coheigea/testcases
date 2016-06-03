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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.server.HiveServer2;
import org.junit.Assert;

public class HIVETest {
    
    private static final File hdfsBaseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, hdfsBaseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        FileUtil.fullyDelete(hdfsBaseDir);
        hdfsCluster.shutdown();
    }
    
    @org.junit.Test
    public void basicHIVETest() throws Exception {
        
        HiveConf conf = new HiveConf();
       
        // Warehouse
        File warehouseDir = new File("./target/hdfs/warehouse").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTOREWAREHOUSE.varname, warehouseDir.getPath());
        
        FileSystem fileSystem = hdfsCluster.getFileSystem();
        fileSystem.mkdirs(new Path("/", "warehouse"), new FsPermission((short) 0777));
        
        // Scratchdir
        File scratchDir = new File("./target/hdfs/scratchdir").getAbsoluteFile();
        conf.set("hive.exec.scratchdir", scratchDir.getPath());
        
        fileSystem.mkdirs(new Path("/", "scratchdir"), new FsPermission((short) 0777));
        
        // Create a temporary directory for the Hive metastore
        File metastoreDir = new File("./target/metastore/").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTORECONNECTURLKEY.varname,
                 String.format("jdbc:derby:;databaseName=%s;create=true",  metastoreDir.getPath()));
        
        HiveServer2 hiveServer2 = new HiveServer2();
        hiveServer2.init(conf);
        hiveServer2.start();
        
        Class.forName("org.apache.hive.jdbc.HiveDriver");
        
        // Load data into HIVE (from local filesystem, HIVE is not setup to talk to the HDFS)
        String url = "jdbc:hive2://localhost:10000/default";
        Connection connection = DriverManager.getConnection(url, "alice", "alice");
        Statement statement  = connection.createStatement();
        // statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("create table words (word STRING, count INT) row format delimited fields terminated by '\t' stored as textfile");
        java.io.File inputFile = new java.io.File(this.getClass().getResource("../../../../../wordcount.txt").toURI());
        statement.execute("LOAD DATA INPATH '" + inputFile.getAbsolutePath() + "' OVERWRITE INTO TABLE words");
        
        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        resultSet.next();
        Assert.assertEquals("Mr.", resultSet.getString(1));
        Assert.assertEquals(100, resultSet.getInt(2));
        
        statement.close();
        
        hiveServer2.stop();
    }
    
    
}
