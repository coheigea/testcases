/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.coheigea.bigdata.knox;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.hadoop.gateway.GatewayServer;
import org.apache.hadoop.gateway.security.ldap.SimpleLdapDirectoryServer;
import org.apache.hadoop.gateway.services.DefaultGatewayServices;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.test.TestUtils;
import org.apache.hadoop.test.mock.MockServer;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

public class KnoxHDFSTest {

    private static GatewayTestConfig config;
    private static GatewayServer gateway;
    private static SimpleLdapDirectoryServer ldap;
    private static TcpTransport ldapTransport;
    private static MockServer server;

    @BeforeClass
    public static void setupSuite() throws Exception {
        setupLdap();
        server = new MockServer( "WEBHDFS", true );
        
        setupGateway();
    }

    @AfterClass
    public static void cleanupSuite() throws Exception {
        gateway.stop();

        FileUtils.deleteQuietly( new File( config.getGatewayTopologyDir() ) );
        FileUtils.deleteQuietly( new File( config.getGatewayConfDir() ) );
        FileUtils.deleteQuietly( new File( config.getGatewaySecurityDir() ) );
        FileUtils.deleteQuietly( new File( config.getGatewayDeploymentDir() ) );
        FileUtils.deleteQuietly( new File( config.getGatewayDataDir() ) );

        server.stop();

        ldap.stop( true );
    }


    public static void setupLdap() throws Exception {
        URL usersUrl = TestUtils.getResourceUrl( KnoxHDFSTest.class, "../users.ldif" );
        ldapTransport = new TcpTransport( 0 );
        ldap = new SimpleLdapDirectoryServer( "dc=hadoop,dc=apache,dc=org", new File( usersUrl.toURI() ), ldapTransport );
        ldap.start();
    }

    public static void setupGateway() throws Exception {

        File targetDir = new File( System.getProperty( "user.dir" ), "target" );
        File gatewayDir = new File( targetDir, "gateway-home-" + UUID.randomUUID() );
        gatewayDir.mkdirs();

        config = new GatewayTestConfig();
        config.setGatewayHomeDir( gatewayDir.getAbsolutePath() );

        // TODO - see KNOX-958 config.setGatewayServicesDir(targetDir.getPath() + File.separator + "services");

        File topoDir = new File( config.getGatewayTopologyDir() );
        topoDir.mkdirs();

        File deployDir = new File( config.getGatewayDeploymentDir() );
        deployDir.mkdirs();

        File descriptor = new File( topoDir, "cluster.xml" );
        FileOutputStream stream = new FileOutputStream( descriptor );
        createTopology().toStream( stream );
        stream.close();

        DefaultGatewayServices srvcs = new DefaultGatewayServices();
        Map<String,String> options = new HashMap<>();
        options.put( "persist-master", "false" );
        options.put( "master", "password" );
        try {
            srvcs.init( config, options );
        } catch ( ServiceLifecycleException e ) {
            e.printStackTrace(); // I18N not required.
        }

        gateway = GatewayServer.startGateway( config, srvcs );
    }

    /**
     * Creates a topology that is deployed to the gateway instance for the test suite.
     * Note that this topology is shared by all of the test methods in this suite.
     * @return A populated XML structure for a topology file.
     */
    private static XMLTag createTopology() {
        XMLTag xml = XMLDoc.newDocument( true )
            .addRoot( "topology" )
            .addTag( "gateway" )
            .addTag( "provider" )
            .addTag( "role" ).addText( "webappsec" )
            .addTag("name").addText("WebAppSec")
            .addTag("enabled").addText("true")
            .addTag( "param" )
            .addTag("name").addText("csrf.enabled")
            .addTag("value").addText("true").gotoParent().gotoParent()
            .addTag("provider")
            .addTag("role").addText("authentication")
            .addTag("name").addText("ShiroProvider")
            .addTag("enabled").addText("true")
            .addTag( "param" )
            .addTag("name").addText("main.ldapRealm")
            .addTag("value").addText("org.apache.hadoop.gateway.shirorealm.KnoxLdapRealm").gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.userDnTemplate" )
            .addTag( "value" ).addText( "uid={0},ou=people,dc=hadoop,dc=apache,dc=org" ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.contextFactory.url" )
            .addTag( "value" ).addText( "ldap://localhost:" + ldapTransport.getAcceptor().getLocalAddress().getPort() ).gotoParent()
            //.addTag( "value" ).addText(driver.getLdapUrl() ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.contextFactory.authenticationMechanism" )
            .addTag( "value" ).addText( "simple" ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "urls./**" )
            .addTag( "value" ).addText( "authcBasic" ).gotoParent().gotoParent()
            .addTag("provider")
            .addTag("role").addText("identity-assertion")
            .addTag("enabled").addText("true")
            .addTag("name").addText("Default").gotoParent()
            .addTag("provider")
            .addTag( "role" ).addText( "authorization" )
            .addTag("name").addText("XASecurePDPKnox")
            .addTag( "enabled" ).addText( "true" )
            .gotoRoot()
            .addTag("service")
            .addTag("role").addText("WEBHDFS")
            .addTag("url").addText("http://localhost:" + server.getPort()).gotoParent()
            .gotoRoot();
        System.out.println( "GATEWAY=" + xml.toString() );
        return xml;
    }
    
    @Test
    public void testHDFSAllowed() throws IOException {
        String root = "/tmp/GatewayBasicFuncTest/testBasicHdfsUseCase";

        server
        .expect()
          .method( "GET" )
          .pathInfo( "/v1" + root )
          .queryParam( "op", "LISTSTATUS" )
        .respond()
          .status( HttpStatus.SC_OK )
          .content( IOUtils.toByteArray( TestUtils.getResourceUrl( KnoxHDFSTest.class, "../webhdfs-liststatus-test.json" ) ) )
          .contentType( "application/json" );

        given()
          .log().all()
          .auth().preemptive().basic( "hdfs", "hdfs-password" )
          .header("X-XSRF-Header", "jksdhfkhdsf")
          .queryParam( "op", "LISTSTATUS" )
        .when()
          .get( "http://localhost:" + gateway.getAddresses()[0].getPort() + "/gateway/cluster/webhdfs" + "/v1" + root )
        .then()
          .statusCode( HttpStatus.SC_OK )
          .log().body()
          .body( "FileStatuses.FileStatus[0].pathSuffix", is( "dir" ) );
    }
    
    @Test
    public void testHDFSNotAllowed() throws IOException {
        String root = "/tmp/GatewayBasicFuncTest/testBasicHdfsUseCase";

        server
        .expect()
          .method( "GET" )
          .pathInfo( "/v1" + root )
          .queryParam( "op", "LISTSTATUS" )
        .respond()
          .status( HttpStatus.SC_OK )
          .content( IOUtils.toByteArray( TestUtils.getResourceUrl( KnoxHDFSTest.class, "../webhdfs-liststatus-test.json" ) ) )
          .contentType( "application/json" );

        given()
          .log().all()
          .auth().preemptive().basic( "hive", "hive-password" )
          .header("X-XSRF-Header", "jksdhfkhdsf")
          .queryParam( "op", "LISTSTATUS" )
        .when()
          .get( "http://localhost:" + gateway.getAddresses()[0].getPort() + "/gateway/cluster/webhdfs" + "/v1" + root )
        .then()
          .statusCode( HttpStatus.SC_FORBIDDEN )
          .log().body();
    }


}