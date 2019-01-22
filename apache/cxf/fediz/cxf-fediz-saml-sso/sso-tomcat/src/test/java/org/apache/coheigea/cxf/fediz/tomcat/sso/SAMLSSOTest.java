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

package org.apache.coheigea.cxf.fediz.tomcat.sso;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A test for SAML SSO using the CXF SAML SSO interceptors (deployed in Tomcat).
 */
public class SAMLSSOTest {

    static String idpHttpsPort;
    static String rpHttpsPort;
    
    private static Tomcat idpServer;
    private static Tomcat rpServer;
    
    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "info");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "info");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.springframework.webflow", "info");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.springframework.security.web", "info");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.cxf.fediz", "info");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.cxf", "info");  
        
        idpHttpsPort = System.getProperty("idp.https.port");
        // idpHttpsPort = "12345";
        assertNotNull("Property 'idp.https.port' null", idpHttpsPort);
        rpHttpsPort = System.getProperty("rp.https.port");
        assertNotNull("Property 'rp.https.port' null", rpHttpsPort);

        idpServer = startServer(true, idpHttpsPort);
        rpServer = startServer(false, rpHttpsPort);
    }
    
    private static Tomcat startServer(boolean idp, String port) 
        throws ServletException, LifecycleException, IOException {
        Tomcat server = new Tomcat();
        server.setPort(0);
        String currentDir = new File(".").getCanonicalPath();
        String baseDir = currentDir + File.separator + "target";
        server.setBaseDir(baseDir);

        if (idp) {
            server.getHost().setAppBase("tomcat/idp/webapps");
        } else {
            server.getHost().setAppBase("tomcat/rp/webapps");
        }
        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(port));
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        //httpsConnector.setAttribute("keyAlias", keyAlias);
        httpsConnector.setAttribute("keystorePass", "tompass");
        httpsConnector.setAttribute("keystoreFile", "test-classes/server.jks");
        httpsConnector.setAttribute("truststorePass", "tompass");
        httpsConnector.setAttribute("truststoreFile", "test-classes/server.jks");
        httpsConnector.setAttribute("clientAuth", "want");
        // httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        server.getService().addConnector(httpsConnector);

        if (idp) {
            File stsWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp-sts");
            server.addWebapp("/fediz-idp-sts", stsWebapp.getAbsolutePath());
    
            File idpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp");
            server.addWebapp("/fediz-idp", idpWebapp.getAbsolutePath());
        } else {
            File rpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "cxfWebapp");
            server.addWebapp("fedizdoubleit", rpWebapp.getAbsolutePath());
        }

        server.start();

        return server;
    } 

    @AfterClass
    public static void cleanup() throws Exception {
        shutdownServer(idpServer);
        shutdownServer(rpServer);
    }
    
    private static void shutdownServer(Tomcat server) {
        try {
            if (server != null && server.getServer() != null
                && server.getServer().getState() != LifecycleState.DESTROYED) {
                if (server.getServer().getState() != LifecycleState.STOPPED) {
                    server.stop();
                }
                server.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getIdpHttpsPort() {
        return idpHttpsPort;
    }
    
    public String getRpHttpsPort() {
        return rpHttpsPort;
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testInBrowser() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/fedizdoubleit/app1/services/25";
        // Use "alice/ecila"
        
        System.out.println("URL: " + url);
        Thread.sleep(60 * 1000);
    }
    
    @org.junit.Test
    // @org.junit.Ignore
    public void testAlice() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/fedizdoubleit/app1/services/25";
        String user = "alice";
        String password = "ecila";
        
        final String bodyTextContent = 
            login(url, user, password, getIdpHttpsPort());
        
        assertTrue(bodyTextContent.contains("This is the double number response"));
        assertTrue(bodyTextContent.contains("50"));
    }

    public String getServletContextName() {
        return "fedizdoubleit";
    }
    
    private static String login(String url, String user, String password, String idpPort) throws IOException {
        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(idpPort)),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        final HtmlForm form = idpPage.getFormByName("samlsigninresponseform");
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        final XmlPage rpPage = button.click();

        return rpPage.asXml();
    }
    
}