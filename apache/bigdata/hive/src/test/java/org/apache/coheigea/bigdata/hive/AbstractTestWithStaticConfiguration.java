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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.coheigea.bigdata.hive.dfs.DFS;
import org.apache.coheigea.bigdata.hive.dfs.MiniDFS;
import org.apache.coheigea.bigdata.hive.server.HiveServer;
import org.apache.coheigea.bigdata.hive.server.HiveServerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hive.conf.HiveConf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.io.Files;

public abstract class AbstractTestWithStaticConfiguration {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractTestWithStaticConfiguration.class);
  protected static final String SINGLE_TYPE_DATA_FILE_NAME = "kv1.dat";
  protected static final String ALL_DB1 = "server=server1->db=db_1",
      ALL_DB2 = "server=server1->db=db_2",
      SELECT_DB1_TBL1 = "server=server1->db=db_1->table=tb_1->action=select",
      SELECT_DB1_TBL2 = "server=server1->db=db_1->table=tb_2->action=select",
      SELECT_DB1_NONTABLE = "server=server1->db=db_1->table=blahblah->action=select",
      INSERT_DB1_TBL1 = "server=server1->db=db_1->table=tb_1->action=insert",
      SELECT_DB2_TBL2 = "server=server1->db=db_2->table=tb_2->action=select",
      INSERT_DB2_TBL1 = "server=server1->db=db_2->table=tb_1->action=insert",
      SELECT_DB1_VIEW1 = "server=server1->db=db_1->table=view_1->action=select",
      ADMIN1 = StaticUserGroup.ADMIN1,
      ADMINGROUP = StaticUserGroup.ADMINGROUP,
      USER1_1 = StaticUserGroup.USER1_1,
      USER1_2 = StaticUserGroup.USER1_2,
      USER2_1 = StaticUserGroup.USER2_1,
      USER3_1 = StaticUserGroup.USER3_1,
      USER4_1 = StaticUserGroup.USER4_1,
      USERGROUP1 = StaticUserGroup.USERGROUP1,
      USERGROUP2 = StaticUserGroup.USERGROUP2,
      USERGROUP3 = StaticUserGroup.USERGROUP3,
      USERGROUP4 = StaticUserGroup.USERGROUP4,
      GROUP1_ROLE = "group1_role",
      DB1 = "db_1",
      DB2 = "db_2",
      DB3 = "db_3",
      TBL1 = "tb_1",
      TBL2 = "tb_2",
      TBL3 = "tb_3",
      VIEW1 = "view_1",
      VIEW2 = "view_2",
      VIEW3 = "view_3",
      INDEX1 = "index_1";

  protected static final String SERVER_HOST = "localhost";

  protected static boolean setMetastoreListener = true;
  protected static boolean enableHiveConcurrency = false;
  // indicate if the database need to be clear for every test case in one test class
  protected static boolean clearDbPerTest = true;

  protected static File baseDir;
  protected static File logDir;
  protected static File confDir;
  protected static File dataDir;
  protected static HiveServer hiveServer;
  protected static FileSystem fileSystem;
  protected static DFS dfs;
  protected static Map<String, String> properties;
  protected static Context context;
  protected final String semanticException = "SemanticException No valid privileges";

  public static void createContext() throws Exception {
    context = new Context(hiveServer, fileSystem,
        baseDir, confDir, dataDir);
  }

  protected static File assertCreateDir(File dir) {
    if(!dir.isDirectory()) {
      Assert.assertTrue("Failed creating " + dir, dir.mkdirs());
    }
    return dir;
  }

  @BeforeClass
  public static void setupTestStaticConfiguration() throws Exception {
    LOGGER.info("AbstractTestWithStaticConfiguration setupTestStaticConfiguration");
    properties = Maps.newHashMap();
    baseDir = Files.createTempDir();
    LOGGER.info("BaseDir = " + baseDir);
    logDir = assertCreateDir(new File(baseDir, "log"));
    confDir = assertCreateDir(new File(baseDir, "etc"));
    dataDir = assertCreateDir(new File(baseDir, "data"));

    dfs = new MiniDFS(baseDir);
    fileSystem = dfs.getFileSystem();

    if (enableHiveConcurrency) {
      properties.put(HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY.varname, "true");
      properties.put(HiveConf.ConfVars.HIVE_TXN_MANAGER.varname,
          "org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager");
      properties.put(HiveConf.ConfVars.HIVE_LOCK_MANAGER.varname,
          "org.apache.hadoop.hive.ql.lockmgr.EmbeddedLockManager");
    }

    hiveServer = create(properties, baseDir, confDir, logDir, fileSystem);
    hiveServer.start();
    createContext();

    // Create tmp as scratch dir if it doesn't exist
    Path tmpPath = new Path("/tmp");
    if (!fileSystem.exists(tmpPath)) {
      fileSystem.mkdirs(tmpPath, new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL));
    }
  }

  public static HiveServer create(Map<String, String> properties,
      File baseDir, File confDir, File logDir,
      FileSystem fileSystem) throws Exception {
    return HiveServerFactory.create( properties,
        baseDir, confDir, logDir, fileSystem);
  }

  @Before
  public void setup() throws Exception{
    LOGGER.info("AbstractTestStaticConfiguration setup");
    dfs.createBaseDir();
    if (clearDbPerTest) {
      LOGGER.info("Before per test run clean up");
      clearAll(true);
    }
  }

  @After
  public void clearAfterPerTest() throws Exception {
    LOGGER.info("AbstractTestStaticConfiguration clearAfterPerTest");
    if (clearDbPerTest) {
      LOGGER.info("After per test run clean up");
      clearAll(true);
     }
  }

  protected static void clearAll(boolean clearDb) throws Exception {
    LOGGER.info("About to run clearAll");
    ResultSet resultSet;
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);

    if (clearDb) {
      LOGGER.info("About to clear all databases and default database tables");
      resultSet = statement.executeQuery("SHOW DATABASES");
      ArrayList<String> dbs = new ArrayList<String>();
      while(resultSet.next()) {
          dbs.add(resultSet.getString(1));
      }
      
      for (String db : dbs) {
        if(!db.equalsIgnoreCase("default")) {
          String sql = "DROP DATABASE if exists " + db + " CASCADE";
          LOGGER.info("Running [" + sql + "]");
          statement.execute(sql);
        }
      }
      statement.execute("USE default");
      resultSet = statement.executeQuery("SHOW tables");
      while (resultSet.next()) {
        Statement statement2 = context.createStatement(connection);
        String sql = "DROP table " + resultSet.getString(1);
        LOGGER.info("Running [" + sql + "]");
        statement2.execute(sql);
        statement2.close();
      }
    }

    statement.close();
    connection.close();

  }

  @AfterClass
  public static void tearDownTestStaticConfiguration() throws Exception {
    if(hiveServer != null) {
      hiveServer.shutdown();
      hiveServer = null;
    }

    if(baseDir != null) {
      FileUtils.deleteQuietly(baseDir);
      baseDir = null;
    }
    if(dfs != null) {
      try {
        dfs.tearDown();
      } catch (Exception e) {
        LOGGER.info("Exception shutting down dfs", e);
      }
    }
    if (context != null) {
      context.close();
    }
  }

  /**
   * A convenience method to validate:
   * if expected is equivalent to returned;
   * Firstly check if each expected item is in the returned list;
   * Secondly check if each returned item in in the expected list.
   */
  protected void validateReturnedResult(List<String> expected, List<String> returned) {
    for (String obj : expected) {
      assertTrue("expected " + obj + " not found in the returned list: " + returned.toString(),
              returned.contains(obj));
    }
    for (String obj : returned) {
      assertTrue("returned " + obj + " not found in the expected list: " + expected.toString(),
              expected.contains(obj));
    }
  }

  /**
   * A convenient function to run a sequence of sql commands
   * @param user
   * @param sqls
   * @throws Exception
   */
  protected void execBatch(String user, List<String> sqls) throws Exception {
    Connection conn = context.createConnection(user);
    Statement stmt = context.createStatement(conn);
    for (String sql : sqls) {
      exec(stmt, sql);
    }
    if (stmt != null) {
      stmt.close();
    }
    if (conn != null) {
      conn.close();
    }
  }

  /**
   * A convenient funciton to run one sql with log
   * @param stmt
   * @param sql
   * @throws Exception
   */
  protected void exec(Statement stmt, String sql) throws Exception {
    if (stmt == null) {
      LOGGER.error("Statement is null");
      return;
    }
    LOGGER.info("Running [" + sql + "]");
    stmt.execute(sql);
  }

}
