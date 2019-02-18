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
package org.apache.coheigea.cxf.jmeter.fediz;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Assert;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * A unit test to use to benchmark the Fediz IdP. To enable, first deploy the Fediz IdP to Tomcat.
 */
@org.junit.Ignore
public class FedizTest extends AbstractBusClientServerTestBase {
	
	private int serverPort = 10443;
    
    @org.junit.Test
    public void testSuccessfulInvokeOnIdP() throws Exception {
        String url = "https://localhost:" + serverPort + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:12345/fedizhelloworld/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", serverPort),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assert.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Parse the form to get the token (wresult)
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String wresult = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
                break;
            }
        }

        Assert.assertNotNull(wresult);

        webClient.close();
    }
    
    @org.junit.Test
    public void testHammerIdP() throws Exception {
        String url = "https://localhost:" + serverPort + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:12345/fedizhelloworld/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        for (int i = 0; i < 100000; i++) {
        	if (i % 10000 == 0) {
        		System.out.println(i);
	        }
        	
	        final WebClient webClient = new WebClient();
	        webClient.getOptions().setUseInsecureSSL(true);
	        webClient.getCredentialsProvider().setCredentials(
	            new AuthScope("localhost", serverPort),
	            new UsernamePasswordCredentials(user, password));
	
	        webClient.getOptions().setJavaScriptEnabled(false);
	        final HtmlPage idpPage = webClient.getPage(url);
	        webClient.getOptions().setJavaScriptEnabled(true);
	        Assert.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
	
	        // Parse the form to get the token (wresult)
	        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");
	
	        String wresult = null;
	        for (DomElement result : results) {
	            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
	                wresult = result.getAttributeNS(null, "value");
	                break;
	            }
	        }
	
	        Assert.assertNotNull(wresult);
	
	        webClient.close();
        }
    }
    
}