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
package org.apache.coheigea.camel.spring_ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.camel.spring.Main;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)

//Define the DirectoryService
@CreateDS(name = "LDAPTest-class",
enableAccessControl = false,
allowAnonAccess = false,
enableChangeLog = true,
partitions = {
  @CreatePartition(
      name = "example",
      suffix = "dc=example,dc=com",
      indexes = {
          @CreateIndex(attribute = "objectClass"),
          @CreateIndex(attribute = "dc"),
          @CreateIndex(attribute = "ou")
      } )
  }
)

@CreateLdapServer(
transports = {
    @CreateTransport(protocol = "LDAP")
}
)

//Inject an file containing entries
@ApplyLdifFiles("ldap.ldif")
public class SpringLDAPTest extends AbstractLdapTestUnit {
    
    private static boolean portUpdated;
    
    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }
            
            // Read in jaas file and substitute in the correct port
            File f = new File(basedir + "/src/test/resources/camel-spring-ldap.xml");
            
            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("portno", "" + super.getLdapServer().getPort());
            
            File f2 = new File(basedir + "/target/test-classes/camel-spring-ldap.xml");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            
            portUpdated = true;
        }
    }
    
    @org.junit.Test
    public void testSpringLDAP() throws Exception {
        // Start up the Camel route
        Main main = new Main();
        main.setApplicationContextUri("camel-spring-ldap.xml");
        
        main.start();
        
        // Sleep to allow time to copy the files etc.
        Thread.sleep(10 * 1000);
        
        main.stop();
    }
    
}
