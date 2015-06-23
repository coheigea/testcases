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
package org.apache.coheigea.cxf.jdbc.authentication;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * This tests using JDBC for authentication. A cxf client sends a SOAP UsernameToken to a CXF
 * Endpoint. The CXF Endpoint has been configured (see cxf-service.xml) to validate the UsernameToken 
 * via the JAASUsernameTokenValidator, which dispatches it to a database for authentication.
 */

public class AuthenticationTest extends AbstractBusClientServerTestBase {
    
	private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
	    
	private static final String PORT = allocatePort(Server.class);
	
	private static Connection conn;

	@BeforeClass
	public static void startServers() throws Exception {
		assertTrue(
				"Server failed to launch",
				// run the server in the same process
				// set this to false to forkj
				launchServer(Server.class, true)
		);
		
		String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
		
		File f = new File(basedir 
                  + "/target/test-classes/org/apache/coheigea/cxf/jdbc/authentication/jdbc.jaas");
		System.setProperty("java.security.auth.login.config", f.getPath());
		
		// Start Apache Derby
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		
		Properties props = new Properties();
	    conn = DriverManager.getConnection("jdbc:derby:memory:derbyDB;create=true", props);
		
		Statement statement = conn.createStatement();
		
		// Read in SQL file + populate the database
		File sqlFile = new File(basedir + "/target/test-classes/create-users.sql");
		String sqlString = FileUtils.readFileToString(sqlFile);
		String[] statements = sqlString.split(";");
		for (String s : statements) {
			String trimmedS = s.trim();
			if (!trimmedS.equals("")) {
				statement.executeUpdate(trimmedS);
			}
		}

	}
	
	@AfterClass
	public static void stopServers() throws Exception {
		// Shut Derby down
		if (conn != null) {
			conn.close();
		}
		try {
			DriverManager.getConnection("jdbc:derby:memory:derbyDB;drop=true");
		} catch (SQLException ex) {
			// expected
		}
	}

    @org.junit.Test
    public void testAuthenticatedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "alice");
        
        doubleIt(transportPort, 25);
    }
    
    @org.junit.Test
    public void testUnauthenticatedRequest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AuthenticationTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = AuthenticationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportPort = 
            service.getPort(portQName, DoubleItPortType.class);
        TestUtil.updateAddressPort(transportPort, PORT);
        
        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.username", "bob");
        
        try {
            doubleIt(transportPort, 25);
            Assert.fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        Assert.assertEquals(numToDouble * 2 , resp);
    }
    
}
