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
package org.apache.coheigea.cxf.jmh.benchmark.wssec;

import java.util.concurrent.TimeUnit;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.assertNotNull;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

/**
 * Some benchmarking tests for different algorithms with XML Encryption.
 */
public class EncryptionAlgorithmBenchmark {
    
    private static Crypto serviceCrypto;
    
    static {
        WSSConfig.init();
        try {
            serviceCrypto = CryptoFactory.getInstance("serviceKeystore.properties");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void encryptionRSAOAEP() throws Exception {
        doEncryption(WSConstants.KEYTRANSPORT_RSAOAEP, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void encryptionRSA15() throws Exception {
        doEncryption(WSConstants.KEYTRANSPORT_RSA15, serviceCrypto);
    }
    
    private void doEncryption(String keyTransportAlgorithm, Crypto verifyingCrypto) throws Exception {
        Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();

        WSSecEncrypt builder = new WSSecEncrypt(secHeader);
        builder.setUserInfo("myservicekey", "skpass");
        builder.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
        builder.setKeyEncAlgo(keyTransportAlgorithm);
        Document encryptedDoc = builder.build(serviceCrypto);

        WSSecurityEngine engine = new WSSecurityEngine();
        
        RequestData data = new RequestData();
        data.setWssConfig(WSSConfig.getNewInstance());
        data.setDecCrypto(verifyingCrypto);
        data.setCallbackHandler(new CommonCallbackHandler());
        if (WSConstants.KEYTRANSPORT_RSA15.equals(keyTransportAlgorithm)) {
            data.setAllowRSA15KeyTransportAlgorithm(true);
        }
        Element securityHeader = WSSecurityUtil.getSecurityHeader(encryptedDoc, "");
        assertNotNull(securityHeader);
        
        WSHandlerResult results = 
            engine.processSecurityHeader(securityHeader, data);
        
        WSSecurityEngineResult actionResult =
            results.getActionResults().get(WSConstants.ENCR).get(0);
        assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE));
        assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    }
    
}