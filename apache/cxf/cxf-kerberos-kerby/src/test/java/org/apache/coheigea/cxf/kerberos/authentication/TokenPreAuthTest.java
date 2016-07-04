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
package org.apache.coheigea.cxf.kerberos.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;

import org.apache.coheigea.cxf.kerberos.common.KerbyServer;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.KrbRuntime;
import org.apache.kerby.kerberos.kerb.ccache.Credential;
import org.apache.kerby.kerberos.kerb.ccache.CredentialCache;
import org.apache.kerby.kerberos.kerb.client.KrbClient;
import org.apache.kerby.kerberos.kerb.client.KrbTokenClient;
import org.apache.kerby.kerberos.kerb.integration.test.jaas.TokenCache;
import org.apache.kerby.kerberos.kerb.server.KdcConfigKey;
import org.apache.kerby.kerberos.kerb.type.ticket.SgtTicket;
import org.apache.kerby.kerberos.kerb.type.ticket.TgtTicket;
import org.apache.kerby.kerberos.provider.token.JwtTokenProvider;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Some tests for Kerberos Token Pre-Authentication
 */
public class TokenPreAuthTest extends org.junit.Assert {

    private static final String KDC_PORT = TestUtil.getPortNumber(Server.class, 2);
    private static final String KDC_UDP_PORT = TestUtil.getPortNumber(Server.class, 3);
    private static KerbyServer kerbyServer;

    @BeforeClass
    public static void setUp() throws Exception {

        WSSConfig.init();

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        updatePort(basedir);

        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos/kerberos.jaas");

        KrbRuntime.setTokenProvider(new JwtTokenProvider());
        kerbyServer = new KerbyServer();

        kerbyServer.setKdcHost("localhost");
        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        kerbyServer.setAllowUdp(true);
        kerbyServer.setKdcUdpPort(Integer.parseInt(KDC_UDP_PORT));
        
        kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.getKdcConfig().setString(KdcConfigKey.TOKEN_ISSUERS, "DoubleItSTSIssuer");
        kerbyServer.getKdcConfig().setString(KdcConfigKey.TOKEN_VERIFY_KEYS, "myclient.cer");
        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.createPrincipal("krbtgt/service.ws.apache.org@service.ws.apache.org", "krbtgt");
        kerbyServer.start();
    }

    @AfterClass
    public static void tearDown() throws KrbException {
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    private static void updatePort(String basedir) throws Exception {

        // Read in krb5.conf and substitute in the correct port
        File f = new File(basedir + "/src/test/resources/kerberos/krb5.conf");

        FileInputStream inputStream = new FileInputStream(f);
        String content = IOUtils.toString(inputStream, "UTF-8");
        inputStream.close();
        // content = content.replaceAll("port", KDC_PORT);
        content = content.replaceAll("port", KDC_UDP_PORT);

        File f2 = new File(basedir + "/target/test-classes/kerberos/krb5.conf");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        System.setProperty("java.security.krb5.conf", f2.getPath());
    }
    
    // Use the TokenAuthLoginModule in Kerby to log in to the KDC using a JWT token
    @org.junit.Test
    public void unitTokenAuthGSSTest() throws Exception {
        
        // 1. Get a TGT from the KDC for the client + create an armor cache
        KrbClient client = new KrbClient();

        client.setKdcHost("localhost");
        client.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        client.setAllowUdp(false);

        client.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();
        
        TgtTicket tgt = client.requestTgt("alice@service.ws.apache.org", "alice");
        assertNotNull(tgt);
        
        // Write to cache
        Credential credential = new Credential(tgt);
        CredentialCache cCache = new CredentialCache();
        cCache.addCredential(credential);
        cCache.setPrimaryPrincipal(tgt.getClientPrincipal());

        File cCacheFile = File.createTempFile("krb5_alice@service.ws.apache.org", "cc");
        cCache.store(cCacheFile);
        
        // Now read in JAAS config + substitute in the armor cache file path value
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File f = new File(basedir + "/target/test-classes/kerberos/kerberos.jaas");

        FileInputStream inputStream = new FileInputStream(f);
        String content = IOUtils.toString(inputStream, "UTF-8");
        inputStream.close();
        content = content.replaceAll("armorCacheVal", cCacheFile.getPath());

        File f2 = new File(basedir + "/target/test-classes/kerberos/kerberos.jaas");
        FileOutputStream outputStream = new FileOutputStream(f2);
        IOUtils.write(content, outputStream, "UTF-8");
        outputStream.close();

        // 2. Create a JWT token using CXF
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setExpiryTime(new Date().getTime() + (60L + 1000L));
        String address = "krbtgt/service.ws.apache.org@service.ws.apache.org";
        claims.setAudiences(Collections.singletonList(address));
        
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(Loader.getResourceAsStream("clientstore.jks"), "cspass".toCharArray());
        
        Properties signingProperties = new Properties();
        signingProperties.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, SignatureAlgorithm.RS256.name());
        signingProperties.put(JoseConstants.RSSEC_KEY_STORE, keystore);
        signingProperties.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "myclientkey");
        signingProperties.put(JoseConstants.RSSEC_KEY_PSWD, "ckpass");

        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);

        JwsSignatureProvider sigProvider =
            JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);

        String signedToken = jws.signWith(sigProvider);
        
        TokenCache.writeToken(signedToken);
        
        // 3. Now log in using JAAS
        LoginContext loginContext = new LoginContext("aliceTokenAuth", new KerberosCallbackHandler());
        loginContext.login();

        Subject clientSubject = loginContext.getSubject();
        //Set<Principal> clientPrincipals = clientSubject.getPrincipals();
        //assertFalse(clientPrincipals.isEmpty());
        
        // Get the TGT
        Set<KerberosTicket> privateCredentials = 
            clientSubject.getPrivateCredentials(KerberosTicket.class);
        assertFalse(privateCredentials.isEmpty());

        // Get the service ticket using GSS
        KerberosClientExceptionAction action =
            new KerberosClientExceptionAction(new KerberosPrincipal("alice@service.ws.apache.org"), "bob@service.ws.apache.org");
        byte[] ticket = (byte[]) Subject.doAs(clientSubject, action);
        assertNotNull(ticket);

        loginContext.logout();

        validateServiceTicket(ticket);

        cCacheFile.delete();
    }
    
    @org.junit.Test
    public void jwtUnitTestAccess() throws Exception {
        
        // Get a TGT
        KrbClient client = new KrbClient();

        client.setKdcHost("localhost");
        client.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        client.setAllowUdp(false);

        client.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();
        
        TgtTicket tgt = client.requestTgt("alice@service.ws.apache.org", "alice");
        assertNotNull(tgt);
        
        // Write to cache
        Credential credential = new Credential(tgt);
        CredentialCache cCache = new CredentialCache();
        cCache.addCredential(credential);
        cCache.setPrimaryPrincipal(tgt.getClientPrincipal());

        File cCacheFile = File.createTempFile("krb5_alice@service.ws.apache.org", "cc");
        cCache.store(cCacheFile);
        
        KrbTokenClient tokenClient = new KrbTokenClient(client);

        tokenClient.setKdcHost("localhost");
        tokenClient.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        tokenClient.setAllowUdp(false);

        tokenClient.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();
        
        // Create a JWT token using CXF
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setExpiryTime(new Date().getTime() + (60L + 1000L));
        String address = "bob/service.ws.apache.org@service.ws.apache.org";
        claims.setAudiences(Collections.singletonList(address));
        
        // Wrap it in a KrbToken + sign it
        CXFKrbToken krbToken = new CXFKrbToken(claims, false);
        krbToken.sign();
        
        // Now get a SGT using the JWT
        SgtTicket tkt;
        try {
            tkt = tokenClient.requestSgt(krbToken, "bob/service.ws.apache.org@service.ws.apache.org", cCacheFile.getPath());
            assertTrue(tkt != null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        
        cCacheFile.delete();
    }
    
    @org.junit.Test
    public void jwtUnitTestIdentity() throws Exception {
        
        // Get a TGT
        KrbClient client = new KrbClient();

        client.setKdcHost("localhost");
        client.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        client.setAllowUdp(false);

        client.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();
        
        TgtTicket tgt = client.requestTgt("alice@service.ws.apache.org", "alice");
        assertNotNull(tgt);
        
        // Write to cache
        Credential credential = new Credential(tgt);
        CredentialCache cCache = new CredentialCache();
        cCache.addCredential(credential);
        cCache.setPrimaryPrincipal(tgt.getClientPrincipal());

        File cCacheFile = File.createTempFile("krb5_alice@service.ws.apache.org", "cc");
        cCache.store(cCacheFile);
        
        KrbTokenClient tokenClient = new KrbTokenClient(client);

        tokenClient.setKdcHost("localhost");
        tokenClient.setKdcTcpPort(Integer.parseInt(KDC_PORT));
        tokenClient.setAllowUdp(false);

        tokenClient.setKdcRealm(kerbyServer.getKdcSetting().getKdcRealm());
        client.init();
        
        // Create a JWT token using CXF
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setExpiryTime(new Date().getTime() + (60L + 1000L));
        String address = "krbtgt/service.ws.apache.org@service.ws.apache.org";
        claims.setAudiences(Collections.singletonList(address));
        
        // Wrap it in a KrbToken + sign it
        CXFKrbToken krbToken = new CXFKrbToken(claims, false);
        krbToken.isIdToken(true);
        krbToken.sign();
        
        // Now get a TGT using the JWT token
        tgt = tokenClient.requestTgt(krbToken, cCacheFile.getPath());

        // Now get a SGT using the TGT
        SgtTicket tkt;
        try {
            tkt = tokenClient.requestSgt(tgt, "bob/service.ws.apache.org@service.ws.apache.org");
            assertTrue(tkt != null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        
        cCacheFile.delete();
    }

    private void validateServiceTicket(byte[] ticket) throws Exception {
        // Get the TGT for the service
        LoginContext loginContext = new LoginContext("bob", new KerberosCallbackHandler());
        loginContext.login();

        Subject serviceSubject = loginContext.getSubject();
        Set<Principal> servicePrincipals = serviceSubject.getPrincipals();
        assertFalse(servicePrincipals.isEmpty());

        // Handle the service ticket
        KerberosServiceExceptionAction serviceAction = 
            new KerberosServiceExceptionAction(ticket, "bob@service.ws.apache.org");

        Subject.doAs(serviceSubject, serviceAction);
    }

    /**
     * This class represents a PrivilegedExceptionAction implementation to obtain a service ticket from a Kerberos
     * Key Distribution Center.
     */
    private static class KerberosClientExceptionAction implements PrivilegedExceptionAction<byte[]> {

        private static final String JGSS_KERBEROS_TICKET_OID = "1.2.840.113554.1.2.2";

        private Principal clientPrincipal;
        private String serviceName;

        public KerberosClientExceptionAction(Principal clientPrincipal, String serviceName) { 
            this.clientPrincipal = clientPrincipal;
            this.serviceName = serviceName;
        }

        public byte[] run() throws GSSException {
            GSSManager gssManager = GSSManager.getInstance();

            GSSName gssService = gssManager.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);
            Oid oid = new Oid(JGSS_KERBEROS_TICKET_OID);
            GSSName gssClient = gssManager.createName(clientPrincipal.getName(), GSSName.NT_USER_NAME);
            GSSCredential credentials = 
                gssManager.createCredential(
                                            gssClient, GSSCredential.DEFAULT_LIFETIME, oid, GSSCredential.INITIATE_ONLY
                    );

            GSSContext secContext =
                gssManager.createContext(
                                         gssService, oid, credentials, GSSContext.DEFAULT_LIFETIME
                    );

            secContext.requestMutualAuth(false);
            secContext.requestCredDeleg(false);

            byte[] token = new byte[0];
            byte[] returnedToken = secContext.initSecContext(token, 0, token.length);

            secContext.dispose();

            return returnedToken;
        }
    }

    private static class KerberosServiceExceptionAction implements PrivilegedExceptionAction<byte[]> {

        private static final String JGSS_KERBEROS_TICKET_OID = "1.2.840.113554.1.2.2";

        private byte[] ticket;
        private String serviceName;

        public KerberosServiceExceptionAction(byte[] ticket, String serviceName) {
            this.ticket = ticket;
            this.serviceName = serviceName;
        }

        public byte[] run() throws GSSException {

            GSSManager gssManager = GSSManager.getInstance();

            GSSContext secContext = null;
            GSSName gssService = gssManager.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);

            Oid oid = new Oid(JGSS_KERBEROS_TICKET_OID);
            GSSCredential credentials = 
                gssManager.createCredential(
                                            gssService, GSSCredential.DEFAULT_LIFETIME, oid, GSSCredential.ACCEPT_ONLY
                    );
            secContext = gssManager.createContext(credentials);

            try {
                byte[] outputToken = secContext.acceptSecContext(ticket, 0, ticket.length);
                /*
                if (secContext instanceof com.sun.security.jgss.ExtendedGSSContext) {
                    com.sun.security.jgss.ExtendedGSSContext ex = (com.sun.security.jgss.ExtendedGSSContext)secContext;
                    
                    com.sun.security.jgss.AuthorizationDataEntry[] authzDataEntries =
                        (com.sun.security.jgss.AuthorizationDataEntry[])ex.inquireSecContext(com.sun.security.jgss.InquireType.KRB5_GET_AUTHZ_DATA);
                    System.out.println("AUTHZ DATA LEN: " + authzDataEntries.length);
                    if (authzDataEntries != null && authzDataEntries.length > 0) {
                        System.out.println("REC AUTHZ DATA: " + new String(authzDataEntries[0].getData()));
                    }
                }
                */
                return outputToken;
            } finally {
                if (null != secContext) {
                    secContext.dispose();    
                }
            }               
        }

    }
}
