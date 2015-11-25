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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Map;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.fest.reflect.core.Reflection;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveServerFactory {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(HiveServerFactory.class);
  private static final String HIVE_DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
  private static final String DERBY_DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String HIVESERVER2_TYPE = "sentry.e2etest.hiveServer2Type";
  public static final String KEEP_BASEDIR = "sentry.e2etest.keepBaseDir";
  public static final String METASTORE_CONNECTION_URL = HiveConf.ConfVars.METASTORECONNECTURLKEY.varname;
  public static final String WAREHOUSE_DIR = HiveConf.ConfVars.METASTOREWAREHOUSE.varname;
  public static final String AUTHZ_PROVIDER_FILENAME = "sentry-provider.ini";
  public static final String HS2_PORT = ConfVars.HIVE_SERVER2_THRIFT_PORT.toString();
  public static final String SUPPORT_CONCURRENCY = HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY.varname;
  public static final String HADOOPBIN = ConfVars.HADOOPBIN.toString();
  public static final String DEFAULT_AUTHZ_SERVER_NAME = "server1";
  public static final String HIVESERVER2_IMPERSONATION = "hive.server2.enable.doAs";
  public static final String METASTORE_URI = HiveConf.ConfVars.METASTOREURIS.varname;
  public static final String METASTORE_HOOK = HiveConf.ConfVars.METASTORE_PRE_EVENT_LISTENERS.varname;
  public static final String METASTORE_SETUGI = HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI.varname;
  public static final String METASTORE_CLIENT_TIMEOUT = HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT.varname;
  public static final String METASTORE_RAW_STORE_IMPL = HiveConf.ConfVars.METASTORE_RAW_STORE_IMPL.varname;

  static {
    try {
      Assert.assertNotNull(DERBY_DRIVER_NAME + " is null", Class.forName(DERBY_DRIVER_NAME));
      Assert.assertNotNull(HIVE_DRIVER_NAME + " is null", Class.forName(HIVE_DRIVER_NAME));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static HiveServer create(Map<String, String> properties, File baseDir, File confDir,
      File logDir, FileSystem fileSystem) throws Exception {

    if(!properties.containsKey(WAREHOUSE_DIR)) {
      LOGGER.info("fileSystem " + fileSystem.getClass().getSimpleName());
      if (fileSystem instanceof DistributedFileSystem) {
        @SuppressWarnings("static-access")
        String dfsUri = fileSystem.getDefaultUri(fileSystem.getConf()).toString();
        LOGGER.info("dfsUri " + dfsUri);
        properties.put(WAREHOUSE_DIR, dfsUri + "/data");
        fileSystem.mkdirs(new Path("/data/"), new FsPermission((short) 0777));
      } else {
        properties.put(WAREHOUSE_DIR, new File(baseDir, "warehouse").getPath());
        fileSystem.mkdirs(new Path("/", "warehouse"), new FsPermission((short) 0777));
      }
    }
    Boolean policyOnHDFS = new Boolean(System.getProperty("sentry.e2etest.policyonhdfs", "false"));
    if (policyOnHDFS) {
      // Initialize "hive.exec.scratchdir", according the description of
      // "hive.exec.scratchdir", the permission should be (733).
      // <description>HDFS root scratch dir for Hive jobs which gets created with write
      // all (733) permission. For each connecting user, an HDFS scratch dir:
      // ${hive.exec.scratchdir}/&lt;username&gt; is created,
      // with ${hive.scratch.dir.permission}.</description>
      fileSystem.mkdirs(new Path("/tmp/hive/"));
      fileSystem.setPermission(new Path("/tmp/hive/"), new FsPermission((short) 0733));
    } else {
      LOGGER.info("Setting an readable path to hive.exec.scratchdir");
      properties.put("hive.exec.scratchdir", new File(baseDir, "scratchdir").getPath());
    }
    if(!properties.containsKey(METASTORE_CONNECTION_URL)) {
      properties.put(METASTORE_CONNECTION_URL,
          String.format("jdbc:derby:;databaseName=%s;create=true",
              new File(baseDir, "metastore").getPath()));
    }
    if(!properties.containsKey(HS2_PORT)) {
      properties.put(HS2_PORT, String.valueOf(findPort()));
    }
    if(!properties.containsKey(SUPPORT_CONCURRENCY)) {
      properties.put(SUPPORT_CONCURRENCY, "false");
    }
    if(!properties.containsKey(HADOOPBIN)) {
      properties.put(HADOOPBIN, "./target/hadoop");
    }

    properties.put(METASTORE_SETUGI, "true");
    properties.put(METASTORE_CLIENT_TIMEOUT, "100");
    properties.put(ConfVars.HIVE_WAREHOUSE_SUBDIR_INHERIT_PERMS.varname, "true");

    properties.put(ConfVars.HIVESTATSAUTOGATHER.varname, "false");
    properties.put(ConfVars.HIVE_STATS_COLLECT_SCANCOLS.varname, "true");
    String hadoopBinPath = properties.get(HADOOPBIN);
    Assert.assertNotNull(hadoopBinPath, "Hadoop Bin");
    File hadoopBin = new File(hadoopBinPath);
    if(!hadoopBin.isFile()) {
      Assert.fail("Path to hadoop bin " + hadoopBin.getPath() + " is invalid. "
          + "Perhaps you missed the download-hadoop profile.");
    }

    /*
     * This hack, setting the hiveSiteURL field removes a previous hack involving
     * setting of system properties for each property. Although both are hacks,
     * I prefer this hack because once the system properties are set they can
     * affect later tests unless those tests clear them. This hack allows for
     * a clean switch to a new set of defaults when a new HiveConf object is created.
     */
    Reflection.staticField("hiveSiteURL")
      .ofType(URL.class)
      .in(HiveConf.class)
      .set(null);
    HiveConf hiveConf = new HiveConf();
    for(Map.Entry<String, String> entry : properties.entrySet()) {
      LOGGER.info(entry.getKey() + " => " + entry.getValue());
      hiveConf.set(entry.getKey(), entry.getValue());
    }
    File hiveSite = new File(confDir, "hive-site.xml");

    hiveConf.set(HIVESERVER2_IMPERSONATION, "false");
    OutputStream out = new FileOutputStream(hiveSite);
    hiveConf.writeXml(out);
    out.close();

    Reflection.staticField("hiveSiteURL")
      .ofType(URL.class)
      .in(HiveConf.class)
      .set(hiveSite.toURI().toURL());

    LOGGER.info("Creating InternalHiveServer");
    return new InternalHiveServer(hiveConf);
  }
  private static int findPort() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    int port = socket.getLocalPort();
    socket.close();
    return port;
  }

}
