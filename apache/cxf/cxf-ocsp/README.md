cxf-ocsp
===========

This project contains a number of tests that show how a CXF service can
validate client certificates using OCSP.

1) WSSecurityOCSPTest

A test-case an asymmetric WS-Security client request, where the service uses
OCSP to validate that the client's certificate is valid.
   
Prerequisite: Launch OpenSSL via (pass phrase: security):
     
openssl ocsp -index ca.db.index -port 12345 -text -rkey wss40CAKey.pem -CA wss40CA.pem -rsigner wss40CA.pem

2) WSSecurityOCSPCertTest

A test-case an asymmetric WS-Security client request, where the service uses
OCSP to validate that the client's certificate is valid. This test differs
from WSSecurityOCSPTest in that the OCSP uses a certificate to sign the OCSP
response that is different from the client's CA.

Ordinarily this will fail trust validation but we will configure the service
(OCSP client) to accept the cert via a Java Security property.
 
Prerequisite: Launch OpenSSL via (pass phrase: security):
 
openssl ocsp -index ca.db.index -port 12345 -text -rkey wss40key.pem -CA wss40CA.pem -rsigner wss40.pem

3) TLSOCSPTest

Some test-cases where a SOAP client request over TLS, where the client uses
OCSP to validate that the server's certificate is valid. It contains two
test-cases, one where TLS is configured in code, and one where it is
configured in a spring configuration file.
   
Prerequisite: Launch OpenSSL via (pass phrase: security):
     
openssl ocsp -index ca.db.index -port 12345 -text -rkey wss40CAKey.pem -CA wss40CA.pem -rsigner wss40CA.pem

4) TLSOCSPCertTest

A test-case for a SOAP client request over TLS, where the client uses OCSP to
validate that the server's certificate is valid. This test differs from
TLSOCSPTest in that the OCSP uses a certificate to sign the OCSP response that
is different from the service's CA. 

Ordinarily this will fail trust validation but we will configure the client
to accept the cert via a Java Security property.
 
Prerequisite: Launch OpenSSL via (pass phrase: security):
 
openssl ocsp -index ca.db.index -port 12345 -text -rkey wss40key.pem -CA wss40CA.pem -rsigner wss40.pem


