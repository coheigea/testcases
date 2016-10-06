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

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.wss4j.StaxSerializer;
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
import org.apache.xml.security.encryption.Serializer;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

/**
 * Some benchmarking tests for different encryption serializers with WSS4J.
 */
public class EncryptionSerializerBenchmark {
    
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
    @Measurement(iterations = 20)
    public void encryptionDefaultSerializer() throws Exception {
        doEncryption(null);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    @Measurement(iterations = 20)
    public void encryptionStaxSerializer() throws Exception {
        doEncryption(new StaxSerializer());
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    @Measurement(iterations = 20)
    public void decryptionDefaultSerializer() throws Exception {
        Document encryptedDoc = doEncryption(null);
        doDecryption(encryptedDoc, null);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    @Measurement(iterations = 20)
    public void decryptionStaxSerializer() throws Exception {
        Serializer serializer = new StaxSerializer();
        Document encryptedDoc = doEncryption(null);
        doDecryption(encryptedDoc, serializer);
    }
    
    private Document doEncryption(Serializer serializer) throws Exception {
        WSSecEncrypt builder = new WSSecEncrypt();
        builder.setUserInfo("myservicekey", "skpass");
        builder.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
        builder.setEncryptionSerializer(serializer);
        Document doc = StaxUtils.read(new StringReader(SOAPUtil.SAMPLE_SOAP_MSG));
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();
        return builder.build(doc, serviceCrypto, secHeader);
    }
    
    private void doDecryption(Document encryptedDoc, Serializer serializer) throws Exception {
        WSSecurityEngine engine = new WSSecurityEngine();
        
        RequestData data = new RequestData();
        data.setWssConfig(WSSConfig.getNewInstance());
        data.setDecCrypto(serviceCrypto);
        data.setCallbackHandler(new CommonCallbackHandler());
        data.setEncryptionSerializer(serializer);

        Element securityHeader = WSSecurityUtil.getSecurityHeader(encryptedDoc, "");
        Assert.assertNotNull(securityHeader);
        
        WSHandlerResult results = 
            engine.processSecurityHeader(securityHeader, data);
        
        WSSecurityEngineResult actionResult =
            results.getActionResults().get(WSConstants.ENCR).get(0);
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE));
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    }
}
