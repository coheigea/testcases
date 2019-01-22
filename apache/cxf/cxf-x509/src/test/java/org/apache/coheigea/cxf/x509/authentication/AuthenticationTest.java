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
package org.apache.coheigea.cxf.x509.authentication;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;

import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.w3c.dom.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This tests using X.509 Certificates for authentication in CXF using a WS-SecurityPolicy
 * TransportBinding with an endorsing X509Token.
 */
public class AuthenticationTest extends AbstractBusClientServerTestBase {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
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
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.signature.username", "myclientkey");
        client.getRequestContext().put("ws-security.signature.properties",
                "clientKeystore.properties");

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
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        Client client = ClientProxy.getClient(transportPort);
        client.getRequestContext().put("ws-security.signature.username", "imposter");
        client.getRequestContext().put("ws-security.signature.properties",
                "imposterKeystore.properties");

        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on imposter");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testAuthenticatedRequestCodeFirst() throws Exception {

        String address = "https://localhost:" + PORT + "/doubleit/services/doubleittransportcodefirst";

        WSPolicyFeature policyFeature = new WSPolicyFeature();
        Element policyElement =
            StaxUtils.read(getClass().getResourceAsStream("securitypolicy.xml")).getDocumentElement();
        policyFeature.setPolicyElements(Collections.singletonList(policyElement));

        JaxWsProxyFactoryBean clientFactoryBean = new JaxWsProxyFactoryBean();
        clientFactoryBean.setFeatures(Collections.singletonList(policyFeature));
        clientFactoryBean.setAddress(address);
        clientFactoryBean.setServiceName(SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportPort");
        clientFactoryBean.setEndpointName(portQName);
        clientFactoryBean.setServiceClass(DoubleItCodeFirst.class);

        DoubleItCodeFirst transportPort = (DoubleItCodeFirst)clientFactoryBean.create();
        Client client = ClientProxy.getClient(transportPort);

        // WS-Security config
        client.getRequestContext().put("security.signature.username", "myclientkey");
        client.getRequestContext().put("security.signature.properties", "clientKeystore.properties");
        client.getRequestContext().put("security.callback-handler",
                                       "org.apache.coheigea.cxf.x509.authentication.CommonCallbackHandler");
        client.getRequestContext().put("security.encryption.properties", "clientKeystore.properties");
        client.getRequestContext().put("security.encryption.username", "myservicekey");

        // TLS config
        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        final KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream trustStore =
            ClassLoaderUtils.getResourceAsStream("clientstore.jks", AuthenticationTest.class)) {
            ts.load(trustStore, "cspass".toCharArray());
        }
        tmf.init(ts);

        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setTrustManagers(tmf.getTrustManagers());
        tlsParams.setDisableCNCheck(true);

        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        int resp = transportPort.doubleIt(25);
        assertEquals(25 * 2 , resp);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }

}