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
package org.apache.coheigea.camel.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;

import org.apache.camel.spring.Main;
import org.apache.commons.io.IOUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.junit.After;
import org.junit.Before;

/**
 * This test uses the Apache Camel File component to take plaintext files from
 * src/test/resources/ssh_data which contain various unix commands. These commands
 * are run on the SSH server, and the results are stored in target/results.
 */
public class SSHTest extends org.junit.Assert {

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
        File f = new File(basedir + "/src/test/resources/camel-ssh.xml");

        FileInputStream inputStream = new FileInputStream(f);
        String content = IOUtils.toString(inputStream, "UTF-8");
        inputStream.close();
        content = content.replaceAll("portno", "" + port);

        File f2 = new File(basedir + "/target/test-classes/camel-ssh.xml");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);

        // Generate a key
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("target/generatedkey.pem")));

        // Simple CommandFactory to run commands in a process
        sshServer.setCommandFactory(new CommandFactory() {
            public Command createCommand(String command) {
                return new ProcessShellFactory(command.split(";")).create();
            }
        });
        // sshServer.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));

        sshServer.setPasswordAuthenticator(new CamelPasswordAuthenticator());
        sshServer.start();
    }

    @After
    public void cleanup() throws Exception {
        if (sshServer != null) {
            sshServer.stop(true);
        }
    }

    @org.junit.Test
    public void testSSH() throws Exception {
        // Start up the Camel route
        Main main = new Main();
        main.setApplicationContextUri("camel-ssh.xml");

        main.start();

        // Sleep to allow time to copy the files etc.
        Thread.sleep(10 * 1000);

        main.stop();
    }

}
