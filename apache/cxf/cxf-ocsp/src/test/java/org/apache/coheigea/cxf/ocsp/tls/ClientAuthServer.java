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
package org.apache.coheigea.cxf.ocsp.tls;

import java.security.KeyStore;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.Endpoint;

import org.apache.coheigea.cxf.ocsp.common.DoubleItPortTypeImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.xml.security.utils.ClassLoaderUtils;

/**
 * Here we are configuring TLS programmatically using Jetty (and requiring client auth + enabling OCSP).
 */
public class ClientAuthServer extends AbstractBusTestServerBase {

    public ClientAuthServer() {
        // complete
    }

    protected void run()  {

        Bus busLocal = BusFactory.getDefaultBus(true);
        setBus(busLocal);

        String address = "https://localhost:" + TLSOCSPClientAuthTest.PORT + "/doubleit/services/doubleittlsocspclientauth";

        try {
            TrustManagerFactory tmf  =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(ClassLoaderUtils.getResourceAsStream("servicestore.jks", this.getClass()), "sspass".toCharArray());

            PKIXBuilderParameters param = new PKIXBuilderParameters(keyStore, new X509CertSelector());
            param.setRevocationEnabled(true);

            tmf.init(new CertPathTrustManagerParameters(param));

            KeyManagerFactory kmf  =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "skpass".toCharArray());

            ClientAuthentication clientAuthentication = new ClientAuthentication();
            clientAuthentication.setRequired(true);
            clientAuthentication.setWant(true);

            TLSServerParameters tlsParams = new TLSServerParameters();
            tlsParams.setTrustManagers(tmf.getTrustManagers());
            tlsParams.setKeyManagers(kmf.getKeyManagers());
            tlsParams.setClientAuthentication(clientAuthentication);

            Map<String, TLSServerParameters> map = new HashMap<>();
            map.put("tlsId", tlsParams);

            JettyHTTPServerEngineFactory factory =
                busLocal.getExtension(JettyHTTPServerEngineFactory.class);
            factory.setTlsServerParametersMap(map);
            factory.createJettyHTTPServerEngine("localhost", Integer.parseInt(TLSOCSPClientAuthTest.PORT), "https", "tlsId");

            factory.initComplete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Endpoint.publish(address, new DoubleItPortTypeImpl());

    }
}
