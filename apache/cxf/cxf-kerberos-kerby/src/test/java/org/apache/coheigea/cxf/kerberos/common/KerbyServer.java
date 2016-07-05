/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.coheigea.cxf.kerberos.common;

import java.util.List;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.common.EncryptionUtil;
import org.apache.kerby.kerberos.kerb.identity.KrbIdentity;
import org.apache.kerby.kerberos.kerb.server.KdcServer;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionType;

/**
 * A simple extension of the KdcServer which just creates some principals
 */
public class KerbyServer extends KdcServer {

    public synchronized void createPrincipal(String principal, String password)  throws Exception {
        KrbIdentity identity = new KrbIdentity(principal);
        List<EncryptionType> encTypes = getKdcSetting().getKdcConfig().getEncryptionTypes();
        List<EncryptionKey> encKeys = null;
        try {
            encKeys = EncryptionUtil.generateKeys(fixPrincipal(principal), password, encTypes);
        } catch (KrbException e) {
            throw new RuntimeException("Failed to generate encryption keys", e);
        }
        identity.addKeys(encKeys);
        getIdentityService().addIdentity(identity);
    }
    
    public synchronized void createPrincipal(String principal) throws Exception {
        KrbIdentity identity = new KrbIdentity(principal);
        getIdentityService().addIdentity(identity);
    }

    private String fixPrincipal(String principal) {
        if (! principal.contains("@")) {
            principal += "@" + getKdcSetting().getKdcRealm();
        }
        return principal;
    }


}
