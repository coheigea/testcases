cxf-jaxrs-sts
===========

This project contains a number of tests that show how to use the CXF STS to
authenticate and authorize a JAX-RS client.

The CXF associates the following users with roles:

 - "alice/boss+employee"
 - "bob/employee"

1) AuthenticationTest

This tests using the CXF STS for authentication. The JAX-RS client sends a
HTTP/BA request to the service, which sends it to the STS for validation
(along with its own UsernameToken for authentication to the STS). 

2) AuthorizationTest

This tests using the CXF STS for authorization. The client authenticates to
the STS using a username/password, and gets a signed holder-of-key SAML
Assertion in return. This is presented to the service, who verifies
proof-of-possession + the signature of the STS on the assertion. The CXF
endpoint extracts roles from the Assertion + populates the security context.
Note that the CXF endpoint requires a "role" Claim via the security policy.
  
The CXF Endpoint has configured the SimpleAuthorizingInterceptor, which
requires that a user must have role "boss" to access the "doubleIt" operation
("alice" has this role, "bob" does not).

