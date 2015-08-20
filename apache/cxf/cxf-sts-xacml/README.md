cxf-sts-xacml
===========

This project contains a number of tests that show how to use XACML with CXF to
authorize a client request. It contains both XACML 2.0 tests and XACML 3.0
tests.

In both cases, the client obtains a SAML Token from the STS with the roles of
the client embedded in the token. The service provider extracts the roles, and
creates a XACML request. For the XACML 2.0 case, OpenSAML is used to create
a XML XACML 2.0 request. This is then sent to a mocked PDP JAX-RS service. For
the XACML 3.0 case, OpenAz is used to create a JSON XACML 3.0 request. This 
is evaluated by a OpenAz-based PDP which is co-located with the service. After
evaluating the request, the PDP response is then enforced at the service side.

The CXF associates the following users with roles:

 - "alice/boss+employee"
 - "bob/employee"

The client authenticates to the STS using a username/password, and gets a
signed holder-of-key SAML Assertion in return. This is presented to the
service, who verifies proof-of-possession + the signature of the STS on the
assertion. The CXF endpoint extracts roles from the Assertion + populates the
security context. Note that the CXF endpoint requires a "role" Claim via the
security policy.
  
The CXF Endpoints set up either the XACML 2.0 or XACML 3.0 interceptors to
create a XACML request for dispatch to the PDP, and then enforces the PDP's
decision. The mocked XACML 2.0 PDP is a REST service that accepts a POST
request to authorization/pdp, requires that a user must have role "boss" to
access the "doubleIt" operation ("alice" has this role, "bob" does not).

The XACML 3.0 PDP evaluates the requests against some policies which are
loaded into the PDP. It is more sophisticated than the mocked PDP above, in
that it uses policies based on the RBAC profile of XACML. The user must have
role "boss" to "execute" on the service "{http://www.example.org/contract/DoubleIt}DoubleItService#DoubleIt".



