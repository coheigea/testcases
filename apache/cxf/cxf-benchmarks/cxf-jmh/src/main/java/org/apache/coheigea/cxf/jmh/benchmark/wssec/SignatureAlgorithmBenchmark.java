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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.wss4j.common.bsp.BSPRule;
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
 * Some benchmarking tests for different algorithms with XML Signatures.
 */
public class SignatureAlgorithmBenchmark {
    
    private static Crypto clientCrypto;
    private static Pattern certConstraint = Pattern.compile(".*O=Apache.*");
    
    static {
        WSSConfig.init();
        try {
            clientCrypto = CryptoFactory.getInstance("clientKeystore.properties");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureExclusiveC14N() throws Exception {
        doSignature(WSConstants.C14N_EXCL_OMIT_COMMENTS, true, clientCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureInclusiveC14N() throws Exception {
        doSignature(WSConstants.C14N_OMIT_COMMENTS, true, clientCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureExclusiveNoPrefixesC14N() throws Exception {
        doSignature(WSConstants.C14N_EXCL_OMIT_COMMENTS, false, clientCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureSHA256() throws Exception {
        doSignature(WSConstants.C14N_EXCL_OMIT_COMMENTS, true, WSConstants.SHA256, 
                    WSConstants.RSA_SHA1, clientCrypto);
    }
    
    @Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
    public void signatureRSASHA256() throws Exception {
        doSignature(WSConstants.C14N_EXCL_OMIT_COMMENTS, true, WSConstants.SHA256, 
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", clientCrypto);
    }
    
    private void doSignature(String c14nAlgo, boolean addInclusivePrefixes, Crypto verifyingCrypto) throws Exception {
        doSignature(c14nAlgo, addInclusivePrefixes, WSConstants.SHA1, WSConstants.RSA_SHA1, verifyingCrypto);
    }
                             
    private void doSignature(String c14nAlgo, 
                             boolean addInclusivePrefixes, 
                             String digestAlgo, 
                             String sigAlgo, 
                             Crypto verifyingCrypto) throws Exception {
        Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
        WSSecHeader secHeader = new WSSecHeader(doc);
        secHeader.insertSecurityHeader();

        WSSecSignature builder = new WSSecSignature(secHeader);
        builder.setUserInfo("myclientkey", "ckpass");
        builder.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
        builder.setSigCanonicalization(c14nAlgo);
        builder.setDigestAlgo(digestAlgo);
        builder.setSignatureAlgorithm(sigAlgo);
        builder.setAddInclusivePrefixes(addInclusivePrefixes);
        
        Document signedDoc = builder.build(clientCrypto);

        WSSecurityEngine engine = new WSSecurityEngine();
        
        RequestData data = new RequestData();
        data.setWssConfig(WSSConfig.getNewInstance());
        data.setSigVerCrypto(verifyingCrypto);
        data.setSubjectCertConstraints(Collections.singletonList(certConstraint));
        
        List<BSPRule> ignoredRules = new ArrayList<BSPRule>();
        ignoredRules.add(BSPRule.R5404);
        ignoredRules.add(BSPRule.R5406);
        data.setIgnoredBSPRules(ignoredRules);
        
        Element securityHeader = WSSecurityUtil.getSecurityHeader(signedDoc, "");
        Assert.assertNotNull(securityHeader);
        
        WSHandlerResult results = 
            engine.processSecurityHeader(securityHeader, data);
        
        WSSecurityEngineResult actionResult =
            results.getActionResults().get(WSConstants.SIGN).get(0);
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE));
        Assert.assertNotNull(actionResult.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE));
    }
    
}
