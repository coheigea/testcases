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
package org.apache.coheigea.cxf.kerberos.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.wss4j.dom.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * There are two test-cases covered in this class, one that uses a WS-SecurityPolicy 
 * KerberosToken policy, and the other that uses a SpnegoContextToken policy.
 *
 * Both testcases start up a KDC locally using Apache DS. In each case, the service endpoint 
 * has a TransportBinding policy, with a corresponding EndorsingSupportingToken which is either 
 * a KerberosToken or SpnegoContextToken. The client will obtain a service ticket from the KDC
 * and include it in the security header of the service request.
 */

@RunWith(FrameworkRunner.class)

//Define the DirectoryService
@CreateDS(name = "KerberosTest-class",
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
      },
  additionalInterceptors = {
     KeyDerivationInterceptor.class
  }
)

@CreateLdapServer(
  transports = {
      @CreateTransport(protocol = "LDAP")
  }
)

@CreateKdcServer(
  transports = {
      // @CreateTransport(protocol = "TCP", address = "127.0.0.1", port=1024)
      @CreateTransport(protocol = "UDP", address = "127.0.0.1")
  },
  primaryRealm = "service.ws.apache.org",
  kdcPrincipal = "krbtgt/service.ws.apache.org@service.ws.apache.org"
)

//Inject an file containing entries
@ApplyLdifFiles("kerberos/kerberos.ldif")

public class AuthenticationTest extends AbstractLdapTestUnit {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = TestUtil.getPortNumber(Server.class);
    
    private static boolean portUpdated;
    
    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();
        
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        } else {
            basedir += "/..";
        }

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/cxf-kerberos/src/test/resources/kerberos/kerberos.jaas");
        
        Assert.assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   AbstractBusClientServerTestBase.launchServer(Server.class, true)
        );
    }

    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }
            
            // Read in krb5.conf and substitute in the correct port
            File f = new File(basedir + "/src/test/resources/kerberos/krb5.conf");
            
            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("port", "" + super.getKdcServer().getTransports()[0].getPort());
            
            File f2 = new File(basedir + "/target/test-classes/kerberos/krb5.conf");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            
            System.setProperty("java.security.krb5.conf", f2.getPath());
            
            portUpdated = true;
        }
    }

    @org.junit.Test
    public void testKerberos() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testSpnego() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSpnegoTransportPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        doubleIt(transportPort, 25);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        Assert.assertEquals(numToDouble * 2 , resp);
    }
    
}
