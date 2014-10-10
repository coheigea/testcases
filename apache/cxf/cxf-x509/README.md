cxf-x509
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using X.509 tokens. The
service endpoints use a TransportBinding policy, with an Endorsing X.509 token.
Essentially what this means is that the service uses TLS, and the client must
sign a Timestamp in the security header of the request using an X.509 token.

1) AuthenticationTest

In this test, the client authenticates to the service provider by signing a
message portion using XML Signature and a X.509 Certificate. The service
provider authenticates the client by the fact that the issuing certificate
of the client certificate is in the keystore/truststore of the service. In
addition, the service provider specifies a constraint on the Subject DN of
the authenticated certificate - it must belong to the "Apache" organisation.

2) AuthorizationTest

This test builds on the authentication test, by adding RBAC authorization.
Authorization is handled by the SimpleAuthorizingInterceptor in CXF, which
requires a role of "boss". The endpoint is configured with a custom WSS4J
Validator, which mocks up a Subject with a role depending on the X.509
certificate principal.

