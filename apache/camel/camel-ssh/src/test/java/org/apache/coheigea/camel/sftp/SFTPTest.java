/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.coheigea.camel.sftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.spring.Main;
import org.apache.commons.io.IOUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Before;

/**
 * This test uses the Apache Camel File component to take XML files from
 * src/test/resources/scp_data. These files are then copied using SFTP to a directory on
 * the SSH server (target/storage_sftp) using the camel-ftp component.
 */
public class SFTPTest extends org.junit.Assert {

    private SshServer sshServer;

    @Before
    public void setup() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(10000);

        // Write port number to configuration file in target
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        // Read in camel configuration file and substitute in the correct port
        File f = new File(basedir + "/src/test/resources/camel-sftp.xml");

        FileInputStream inputStream = new FileInputStream(f);
        String content = IOUtils.toString(inputStream, "UTF-8");
        inputStream.close();
        content = content.replaceAll("portno", "" + port);

        File f2 = new File(basedir + "/target/test-classes/camel-sftp.xml");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);

        // Generate a key
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("target/generatedkey.pem")));
        List<NamedFactory<Command>> subsystemFactories = new ArrayList<NamedFactory<Command>>(1);
        subsystemFactories.add(new SftpSubsystemFactory());
        sshServer.setSubsystemFactories(subsystemFactories);
        sshServer.setCommandFactory(new ScpCommandFactory());

        sshServer.setPasswordAuthenticator(new CamelPasswordAuthenticator());
        sshServer.start();

        // Create "storage_sftp" directory (delete it first if it exists)
        File storageDirectory = new File(basedir + "/target/storage_sftp");
        if (storageDirectory.exists()) {
            storageDirectory.delete();
        }
        storageDirectory.mkdir();
    }

    @After
    public void cleanup() throws Exception {
        if (sshServer != null) {
            sshServer.stop(true);
        }
    }

    @org.junit.Test
    public void testSFTP() throws Exception {
        // Start up the Camel route
        Main main = new Main();
        main.setApplicationContextUri("camel-sftp.xml");

        main.start();

        // Sleep to allow time to copy the files etc.
        Thread.sleep(10 * 1000);

        main.stop();
    }

}
