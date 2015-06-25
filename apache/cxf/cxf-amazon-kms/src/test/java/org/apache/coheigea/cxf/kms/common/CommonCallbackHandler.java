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
package org.apache.coheigea.cxf.kms.common;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;

public class CommonCallbackHandler implements CallbackHandler {
    
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String masterKeyId;
    
    public void handle(Callback[] callbacks) throws IOException,
        UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                if (pc.getUsage() == WSPasswordCallback.SECRET_KEY) {
                    final AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

                    AWSKMSClient kms = new AWSKMSClient(creds);
                    kms.setEndpoint(endpoint);
                    
                    if (pc.getEncryptedSecret() != null) {
                        ByteBuffer encryptedKey = ByteBuffer.wrap(pc.getEncryptedSecret());
                        
                        DecryptRequest req = new DecryptRequest().withCiphertextBlob(encryptedKey);
                        ByteBuffer plaintextKey = kms.decrypt(req).getPlaintext();
                        
                        byte[] key = new byte[plaintextKey.remaining()];
                        plaintextKey.get(key);
                        pc.setKey(key);
                    } else {
    
                        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
                        dataKeyRequest.setKeyId(masterKeyId);
                        String algorithm = "AES_128";
                        if (pc.getAlgorithm() != null && pc.getAlgorithm().contains("aes256")) {
                            algorithm = "AES_256";
                        }
                        dataKeyRequest.setKeySpec(algorithm);
    
                        GenerateDataKeyResult dataKeyResult = kms.generateDataKey(dataKeyRequest);
                        
                        ByteBuffer plaintextKey = dataKeyResult.getPlaintext();
                        byte[] key = new byte[plaintextKey.remaining()];
                        plaintextKey.get(key);
                        pc.setKey(key);
    
                        ByteBuffer encryptedKey = dataKeyResult.getCiphertextBlob();
                        byte[] encKey = new byte[encryptedKey.remaining()];
                        encryptedKey.get(encKey);
                        pc.setEncryptedSecret(encKey);
                        
                        // Create a KeyName pointing to the encryption key
                        Document doc = DOMUtils.newDocument();
                        Element keyInfoElement =
                            doc.createElementNS(
                                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN
                            );
                        keyInfoElement.setAttributeNS(
                            WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS
                        );
                        Element keyNameElement =
                            doc.createElementNS(
                                WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":KeyName"
                            );
                        keyNameElement.setTextContent("1c84a3f2-51cc-4c66-9045-68f51ef8b1eb");
                        keyInfoElement.appendChild(keyNameElement);
                        pc.setKeyInfoReference(keyInfoElement);
                    }
                }
            }
        }
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMasterKeyId() {
        return masterKeyId;
    }

    public void setMasterKeyId(String masterKeyId) {
        this.masterKeyId = masterKeyId;
    }
}
