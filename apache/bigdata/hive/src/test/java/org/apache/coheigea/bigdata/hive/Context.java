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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class Context {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(Context.class);

  private final HiveServer hiveServer;
  private final FileSystem fileSystem;
  private final File baseDir;
  private final File dataDir;

  private final Set<Connection> connections;
  private final Set<Statement> statements;

  public Context(HiveServer hiveServer, FileSystem fileSystem,
      File baseDir, File confDir, File dataDir) throws Exception {
    this.hiveServer = hiveServer;
    this.fileSystem = fileSystem;
    this.baseDir = baseDir;
    this.dataDir = dataDir;
    connections = Sets.newHashSet();
    statements = Sets.newHashSet();
  }

  public Connection createConnection(String username) throws Exception {

    String password = username;
    Connection connection =  hiveServer.createConnection(username, password);
    connections.add(connection);
    assertNotNull("Connection is null", connection);
    assertFalse("Connection should not be closed", connection.isClosed());
    Statement statement  = connection.createStatement();
    statement.close();
    return connection;
  }

  public Statement createStatement(Connection connection)
  throws Exception {
    Statement statement  = connection.createStatement();
    assertNotNull("Statement is null", statement);
    statements.add(statement);
    return statement;
  }

  public void close() {
    for(Statement statement : statements) {
      try {
        statement.close();
      } catch (Exception exception) {
        LOGGER.warn("Error closing " + statement, exception);
      }
    }
    statements.clear();

    for(Connection connection : connections) {
      try {
        connection.close();
      } catch (Exception exception) {
        LOGGER.warn("Error closing " + connection, exception);
      }
    }
    connections.clear();
  }

  public File getBaseDir() {
    return baseDir;
  }

  public File getDataDir() {
    return dataDir;
  }

  @SuppressWarnings("static-access")
  public URI getDFSUri() throws IOException {
    return fileSystem.getDefaultUri(fileSystem.getConf());
  }

  public String getProperty(String propName) {
    return hiveServer.getProperty(propName);
  }

  public String getConnectionURL() {
    return hiveServer.getURL();
  }

  // TODO: Handle kerberos login
  public HiveMetaStoreClient getMetaStoreClient(String userName) throws Exception {
    UserGroupInformation clientUgi = UserGroupInformation.createRemoteUser(userName);
    HiveMetaStoreClient client = (HiveMetaStoreClient) clientUgi.
        doAs(new PrivilegedExceptionAction<Object> () {
          @Override
          public HiveMetaStoreClient run() throws Exception {
            return new HiveMetaStoreClient(new HiveConf());
          }
        });
    return client;
  }

  /**
   * Execute "set x" and extract value from key=val format result Verify the
   * extracted value
   *
   * @param stmt
   * @return
   * @throws Exception
   */
  public void verifySessionConf(Connection con, String key, String expectedVal)
      throws Exception {
    Statement stmt = con.createStatement();
    ResultSet res = stmt.executeQuery("set " + key);
    assertTrue(res.next());
    String resultValues[] = res.getString(1).split("="); // "key=val"
    assertEquals("Result not in key = val format", 2, resultValues.length);
    assertEquals("Conf value should be set by execute()", expectedVal,
        resultValues[1]);
  }

}
