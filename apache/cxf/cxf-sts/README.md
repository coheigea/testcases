cxf-sts
===========

This project contains a number of tests that show how to use the CXF STS to
authenticate and authorize a JAX-WS client.

1) AuthenticationTest

This tests using the CXF STS for authentication. The client authenticates to
the STS using a username/password, and gets a signed holder-of-key SAML
Assertion in return. This is presented to the service, who verifies
proof-of-possession + the signature of the STS on the assertion.

2) AuthorizationTest

3) SSOTest

