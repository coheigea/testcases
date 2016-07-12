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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.fediz.tomcat7.FederationAuthenticator;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

/**
 * A test for SAML SSO using the CXF SAML SSO interceptors (deployed in Tomcat).
 */
public class OIDCTest {
    
    private enum Server {
        IDP,
        OIDC,
        RP
    };

    static String idpHttpsPort;
    static String oidcHttpsPort;
    static String rpHttpsPort;
    
    private static Tomcat idpServer;
    private static Tomcat oidcServer;
    private static Tomcat rpServer;
    
    private static String storedClientId;
    
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
        Assert.assertNotNull("Property 'idp.https.port' null", idpHttpsPort);
        rpHttpsPort = System.getProperty("rp.https.port");
        Assert.assertNotNull("Property 'rp.https.port' null", rpHttpsPort);
        oidcHttpsPort = System.getProperty("oidc.https.port");
        Assert.assertNotNull("Property 'oidc.https.port' null", oidcHttpsPort);

        // Start IdP + OIDC servers
        idpServer = startServer(Server.IDP, idpHttpsPort);
        oidcServer = startServer(Server.OIDC, oidcHttpsPort);
        
        // Log onto OIDC + create a new client
        loginToClientsPage(rpHttpsPort, oidcHttpsPort, idpHttpsPort);
        
        // Substitute in Client ID into RP configuration
        String currentDir = new File(".").getCanonicalPath();
        File rpConfig = new File(currentDir + File.separator 
                                 + "target/tomcat/rp/webapps/cxfWebapp/WEB-INF/cxf-service.xml");
        FileInputStream inputStream = new FileInputStream(rpConfig);
        String content = IOUtils.toString(inputStream, "UTF-8");
        inputStream.close();
        
        content = content.replaceAll("consumer-id", storedClientId);
        
        rpConfig = new File(currentDir + File.separator 
                            + "target/tomcat/rp/webapps/cxfWebapp/WEB-INF/cxf-service.xml");
        try (FileOutputStream outputStream = new FileOutputStream(rpConfig)) {
            IOUtils.write(content, outputStream, "UTF-8");
        }
        
        // Start RP server
        rpServer = startServer(Server.RP, rpHttpsPort);
    }
    
    private static Tomcat startServer(Server serverType, String port) 
        throws ServletException, LifecycleException, IOException {
        Tomcat server = new Tomcat();
        server.setPort(0);
        String currentDir = new File(".").getCanonicalPath();
        String baseDir = currentDir + File.separator + "target";
        server.setBaseDir(baseDir);

        if (serverType == Server.IDP) {
            server.getHost().setAppBase("tomcat/idp/webapps");
        } else if (serverType == Server.OIDC) {
            server.getHost().setAppBase("tomcat/oidc/webapps");
            System.out.println("Starting OIDC on port: " + port);
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

        if (serverType == Server.IDP) {
            File stsWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp-sts");
            server.addWebapp("/fediz-idp-sts", stsWebapp.getAbsolutePath());
    
            File idpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp");
            server.addWebapp("/fediz-idp", idpWebapp.getAbsolutePath());
        } else if (serverType == Server.OIDC) {
            File rpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-oidc");
            Context cxt = server.addWebapp("/fediz-oidc", rpWebapp.getAbsolutePath());

            // Substitute the IDP port. Necessary if running the test in eclipse where port filtering doesn't seem
            // to work
            File f = new File(currentDir + "/src/test/resources/fediz_config.xml");
            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            if (content.contains("idp.https.port")) {
                content = content.replaceAll("\\$\\{idp.https.port\\}", "" + idpHttpsPort);

                File f2 = new File(baseDir + "/test-classes/fediz_config.xml");
                try (FileOutputStream outputStream = new FileOutputStream(f2)) {
                    IOUtils.write(content, outputStream, "UTF-8");
                }
            }

            FederationAuthenticator fa = new FederationAuthenticator();
            fa.setConfigFile(currentDir + File.separator + "target" + File.separator
                             + "test-classes" + File.separator + "fediz_config.xml");
            cxt.getPipeline().addValve(fa);
            
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
    // @org.junit.Ignore
    public void testInBrowser() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/fedizdoubleit/app1/services/25";
        // Use "alice/ecila"
        
        System.out.println("URL: " + url);
        Thread.sleep(60 * 1000);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testAlice() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/fedizdoubleit/app1/services/25";
        String user = "alice";
        String password = "ecila";
        
        final String bodyTextContent = 
            login(url, user, password, getIdpHttpsPort());
        
        Assert.assertTrue(bodyTextContent.contains("This is the double number response"));
        Assert.assertTrue(bodyTextContent.contains("50"));
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
        Assert.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        final HtmlForm form = idpPage.getFormByName("samlsigninresponseform");
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        final XmlPage rpPage = button.click();

        String rpPageXml = rpPage.asXml();
        
        webClient.close();
        
        return rpPageXml;
    }
    
    // Runs as BeforeClass: Login to the OIDC Clients page + create a new client
    private static void loginToClientsPage(String rpPort, String oidcPort, String idpPort) throws Exception {
        String url = "https://localhost:" + oidcPort + "/fediz-oidc/console/clients";
        String user = "alice";
        String password = "ecila";
        
        // Login to the client page successfully
        WebClient webClient = setupWebClient(user, password, idpPort);
        HtmlPage loginPage = login(url, webClient);
        final String bodyTextContent = loginPage.getBody().getTextContent();
        Assert.assertTrue(bodyTextContent.contains("Registered Clients"));
        
        String clientUrl = "https://localhost:" + rpPort + "/fedizdoubleit/app1/services/25";
        
        // Now try to register a new client
        HtmlPage registeredClientPage = 
            registerNewClient(webClient, url, "consumer-id", clientUrl, clientUrl);
        String registeredClientPageBody = registeredClientPage.getBody().getTextContent();
        Assert.assertTrue(registeredClientPageBody.contains("Registered Clients"));
        Assert.assertTrue(registeredClientPageBody.contains("consumer-id"));
        
        HtmlTable table = registeredClientPage.getHtmlElementById("registered_clients");
        storedClientId = table.getCellAt(1, 1).asText().trim();
        Assert.assertNotNull(storedClientId);
        
        webClient.close();
    }
    
    private static HtmlPage registerNewClient(WebClient webClient, String url,
                                              String clientName, String redirectURI,
                                              String clientAudience) throws Exception {
        HtmlPage registerPage = webClient.getPage(url + "/register");

        final HtmlForm form = registerPage.getForms().get(0);

        // Set new client values
        final HtmlTextInput clientNameInput = form.getInputByName("client_name");
        clientNameInput.setValueAttribute(clientName);
        final HtmlSelect clientTypeSelect = form.getSelectByName("client_type");
        clientTypeSelect.setSelectedAttribute("confidential", true);
        final HtmlTextInput redirectURIInput = form.getInputByName("client_redirectURI");
        redirectURIInput.setValueAttribute(redirectURI);
        final HtmlTextInput clientAudienceURIInput = form.getInputByName("client_audience");
        clientAudienceURIInput.setValueAttribute(clientAudience);

        final HtmlButton button = form.getButtonByName("submit_button");
        return button.click();
    }
    
    private static WebClient setupWebClient(String user, String password, String idpPort) {
        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(idpPort)),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        
        return webClient;
    }
    
    private static HtmlPage login(String url, WebClient webClient) throws IOException {
        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assert.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Test the SAML Version here
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String wresult = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
                break;
            }
        }
        Assert.assertTrue(wresult != null 
            && wresult.contains("urn:oasis:names:tc:SAML:2.0:cm:bearer"));

        final HtmlForm form = idpPage.getFormByName("signinresponseform");
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        return button.click();
    }
}
