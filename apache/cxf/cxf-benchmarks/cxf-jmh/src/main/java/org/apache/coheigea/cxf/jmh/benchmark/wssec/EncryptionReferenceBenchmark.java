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
import org.apache.wss4j.dom.str.STRParser.REFERENCE_TYPE;
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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

/**
 * Some benchmarking tests for different ways of referencing keys in XML Encryption.
 */
public class EncryptionReferenceBenchmark {
    
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
    public void encryptionIssuerSerial() throws Exception {
        doEncryption(WSConstants.ISSUER_SERIAL, REFERENCE_TYPE.ISSUER_SERIAL, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void encryptionBST() throws Exception {
        doEncryption(WSConstants.BST_DIRECT_REFERENCE, REFERENCE_TYPE.DIRECT_REF, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void encryptionEncryptedKeySHA1() throws Exception {
        doEncryption(WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER, REFERENCE_TYPE.THUMBPRINT_SHA1, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void encryptionThumbprintSHA1() throws Exception {
        doEncryption(WSConstants.THUMBPRINT_IDENTIFIER, REFERENCE_TYPE.THUMBPRINT_SHA1, serviceCrypto);
    }
    
    private void doEncryption(int identifier, REFERENCE_TYPE referenceType,
                             Crypto verifyingCrypto) throws Exception {
        Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();

        WSSecEncrypt builder = new WSSecEncrypt(secHeader);
        builder.setUserInfo("myservicekey", "skpass");
        builder.setKeyIdentifierType(identifier);
        Document encryptedDoc = builder.build(serviceCrypto);

        WSSecurityEngine engine = new WSSecurityEngine();
        
        RequestData data = new RequestData();
        data.setWssConfig(WSSConfig.getNewInstance());
        data.setDecCrypto(verifyingCrypto);
        data.setCallbackHandler(new CommonCallbackHandler());
        Element securityHeader = WSSecurityUtil.getSecurityHeader(encryptedDoc, "");
        Assert.assertNotNull(securityHeader);
        
        WSHandlerResult results = 
            engine.processSecurityHeader(securityHeader, data);
        
        WSSecurityEngineResult actionResult =
            results.getActionResults().get(WSConstants.ENCR).get(0);
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE));
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
        REFERENCE_TYPE refType = 
            (REFERENCE_TYPE)actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
        Assert.assertTrue(refType == referenceType);
    }
    
}
