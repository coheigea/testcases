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
package org.apache.coheigea.cxf.ldap.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
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

/**
 * Just a unit test to authenticate using the LDAP API
 */
@RunWith(FrameworkRunner.class)

//Define the DirectoryService
@CreateDS(name = "LDAPAPITest-class",
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
      @CreateTransport(protocol = "LDAP", address = "127.0.0.1")
  }
)

//Inject an file containing entries
@ApplyLdifFiles("ldap.ldif")

public class LDAPAPITest extends AbstractLdapTestUnit {
    
    private static boolean portUpdated;
    
    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }
            
            // Read in jaas file and substitute in the correct port
            File f = new File(basedir +
                              "/src/test/resources/org/apache/coheigea/cxf/ldap/authentication/ldap.jaas");
            
            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("portno", "" + super.getLdapServer().getPort());
            
            File f2 = new File(basedir 
                               + "/target/test-classes/org/apache/coheigea/cxf/ldap/authentication/ldap.jaas");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            
            System.setProperty("java.security.auth.login.config", f2.getPath());
            
            portUpdated = true;
        }
    }
   
    @org.junit.Test
    public void testAuthentication() throws Exception {
    	/*LdapConnectionConfig tlsConfig = new LdapConnectionConfig();
		tlsConfig.setLdapHost( Network.LOOPBACK_HOSTNAME );
		tlsConfig.setLdapPort( 10636 );
		tlsConfig.setTrustManagers( new NoVerificationTrustManager() );
		*/
    	try (LdapNetworkConnection connection = 
    			new LdapNetworkConnection(Network.LOOPBACK_HOSTNAME, super.getLdapServer().getPort())) {
    		connection.connect();
    		connection.bind("cn=alice,ou=users,dc=example,dc=com", "security");

    		Entry admin = connection.lookup("cn=alice,ou=users,dc=example,dc=com");
    		System.out.println("DN: " + admin.getDn());
    	}
    }
    
}
