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

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
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
 * Some benchmarking tests for different ways of referencing keys in XML Signatures.
 */
public class SignatureReferenceBenchmark {
    
    private static Crypto clientCrypto;
    private static Crypto serviceCrypto;
    private static Pattern certConstraint = Pattern.compile(".*O=Apache.*");
    
    static {
        WSSConfig.init();
        try {
            clientCrypto = CryptoFactory.getInstance("clientKeystore.properties");
            serviceCrypto = CryptoFactory.getInstance("serviceKeystore.properties");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureIssuerSerial() throws Exception {
        doSignature(WSConstants.ISSUER_SERIAL, REFERENCE_TYPE.ISSUER_SERIAL, clientCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureBST() throws Exception {
        doSignature(WSConstants.BST_DIRECT_REFERENCE, REFERENCE_TYPE.DIRECT_REF, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureX509KeyIdentifier() throws Exception {
        doSignature(WSConstants.X509_KEY_IDENTIFIER, REFERENCE_TYPE.KEY_IDENTIFIER, serviceCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureThumbprintSHA1() throws Exception {
        doSignature(WSConstants.THUMBPRINT_IDENTIFIER, REFERENCE_TYPE.THUMBPRINT_SHA1, clientCrypto);
    }
    
    private void doSignature(int identifier, REFERENCE_TYPE referenceType,
                             Crypto verifyingCrypto) throws Exception {
        Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();

        WSSecSignature builder = new WSSecSignature(secHeader);
        builder.setUserInfo("myclientkey", "ckpass");
        builder.setKeyIdentifierType(identifier);
        Document signedDoc = builder.build(clientCrypto);

        WSSecurityEngine engine = new WSSecurityEngine();
        
        RequestData data = new RequestData();
        data.setWssConfig(WSSConfig.getNewInstance());
        data.setSigVerCrypto(verifyingCrypto);
        data.setSubjectCertConstraints(Collections.singletonList(certConstraint));
        Element securityHeader = WSSecurityUtil.getSecurityHeader(signedDoc, "");
        Assert.assertNotNull(securityHeader);
        
        WSHandlerResult results = 
            engine.processSecurityHeader(securityHeader, data);
        
        WSSecurityEngineResult actionResult =
            results.getActionResults().get(WSConstants.SIGN).get(0);
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE));
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
        REFERENCE_TYPE refType = 
            (REFERENCE_TYPE)actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
        Assert.assertTrue(refType == referenceType);
    }
    
}
