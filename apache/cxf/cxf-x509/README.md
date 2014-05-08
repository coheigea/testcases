cxf-x509
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using X.509 tokens. The
service endpoints use a TransportBinding policy, with an Endorsing X.509 token.
Essentially what this means is that the service uses TLS, and the client must
sign a Timestamp in the security header of the request using an X.509 token.
Authentication takes place via trust verification of the X.509 certificate. In
the authorization test, ...

