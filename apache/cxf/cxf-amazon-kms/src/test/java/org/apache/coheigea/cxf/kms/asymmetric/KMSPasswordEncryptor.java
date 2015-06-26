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
package org.apache.coheigea.cxf.kms.asymmetric;

import java.nio.ByteBuffer;

import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;

public class KMSPasswordEncryptor implements PasswordEncryptor {
    
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String masterKeyId;

    @Override
    public String encrypt(String password) {
        final AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

        AWSKMSClient kms = new AWSKMSClient(creds);
        kms.setEndpoint(endpoint);
        
        ByteBuffer plaintext = ByteBuffer.wrap(password.getBytes());
        
        EncryptRequest req = new EncryptRequest().withPlaintext(plaintext);
        req.setKeyId(masterKeyId);
        ByteBuffer encryptedKey = kms.encrypt(req).getCiphertextBlob();
        
        byte[] key = new byte[encryptedKey.remaining()];
        encryptedKey.get(key);
        
        return Base64.encode(key);
    }

    @Override
    public String decrypt(String encryptedPassword) {
        
        final AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

        AWSKMSClient kms = new AWSKMSClient(creds);
        kms.setEndpoint(endpoint);
        
        try {
            byte[] encryptedBytes = Base64.decode(encryptedPassword);
            ByteBuffer encryptedKey = ByteBuffer.wrap(encryptedBytes);
            
            DecryptRequest req = new DecryptRequest().withCiphertextBlob(encryptedKey);
            ByteBuffer plaintextKey = kms.decrypt(req).getPlaintext();
            
            byte[] key = new byte[plaintextKey.remaining()];
            plaintextKey.get(key);
            
            return new String(key);
        } catch (Base64DecodingException ex) {
            return null;
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
