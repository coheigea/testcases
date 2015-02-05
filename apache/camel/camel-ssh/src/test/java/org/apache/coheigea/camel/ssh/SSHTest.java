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

import org.apache.camel.spring.Main;
import org.apache.mina.util.AvailablePortFinder;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.openssl.PasswordFinder;
import org.junit.After;
import org.junit.Before;

/**
 */
public class SSHTest extends org.junit.Assert {
    
    private SshServer sshServer;

    @Before
    public void setup() throws Exception {
        // int port = AvailablePortFinder.getNextAvailable(10000);
        int port = 8081;

        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        
        // Generate a key
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("generatedkey.pen"));

        // Simple CommandFactory to run commands in a process
        sshServer.setCommandFactory(new CommandFactory() {
            public Command createCommand(String command) {
                return new ProcessShellFactory(command.split(";")).create();
            }
        });
        // sshServer.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));

        // TODO investigate JAAS
        sshServer.setPasswordAuthenticator(new BogusPasswordAuthenticator());
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
