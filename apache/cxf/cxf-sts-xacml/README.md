cxf-sts-xacml
===========

This project contains a number of tests that show how to use XACML with CXF to
authorize a client request. The client obtains a SAML Token from the STS
with the roles of the client embedded in the token. The service provider 
extracts the roles, and creates a XACML request which is sent to a mocked
PDP service. The PDP response is then enforced at the service side.

The CXF associates the following users with roles:

 - "alice/boss+employee"
 - "bob/employee"

The client authenticates to the STS using a username/password, and gets a
signed holder-of-key SAML Assertion in return. This is presented to the
service, who verifies proof-of-possession + the signature of the STS on the
assertion. The CXF endpoint extracts roles from the Assertion + populates the
security context. Note that the CXF endpoint requires a "role" Claim via the
security policy.
  
The CXF Endpoint has configured the XACMLAuthorizingInterceptor, which creates
a XACML request for dispatch to the PDP, and then enforces the PDP's decision.
The mocked PDP is a REST service, requires that a user must have role "boss"
to access the "doubleIt" operation ("alice" has this role, "bob" does not).

