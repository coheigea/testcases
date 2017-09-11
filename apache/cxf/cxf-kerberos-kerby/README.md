cxf-kerberos
===========

This project contains a number of tests that show how to use Kerberos with
Apache CXF, where the KDC used in the tests is based on Apache Kerby. 

1) AuthenticationTest

Thie test shows how an Apache CXF JAX-WS service endpoint can authenticate a
client using Kerberos. There are two test-cases contained in
"AuthenticationTest", one that uses a WS-SecurityPolicy KerberosToken policy,
and the other that uses a SpnegoContextToken policy.

Both testcases start up a KDC locally using Apache Kerby. In each case, the
service endpoint has a TransportBinding policy, with a corresponding
EndorsingSupportingToken which is either a KerberosToken or SpnegoContextToken.
The client will obtain a service ticket from the KDC and include it in the
security header of the service request.

2) JAXRSAuthenticationTest

This test shows how to use Kerberos with a JAX-RS service. 

3) JWTJAXRSAuthenticationTest

This is a test-case that shows how to use Kerberos and JWT tokens with a JAX-RS service.

The JAX-RS client first obtains a JWT token from the CXF STS via the REST API and uses this token to get a Kerberos ticket
from the KDC to invoke on the service. The service authenticates the Kerberos ticket and uses the embedded JWT token
to ensure that only users with role "boss" can access the "doubleIt" operation ("alice" has this role, "dave" does not)

