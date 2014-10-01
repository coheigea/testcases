cxf-saml-sso
===========

This project contains a number of tests that show how to use the SAML SSO to
authenticate and authorize a JAX-RS client. The test contains a (very) basic
SAML SSO server based on CXF.

The SAML SSO server associates the following users with roles:

 - "alice/boss+employee"
 - "bob/employee"

1) AuthenticationTest

Here the JAX-RS service uses the HTTP redirect binding of SAML SSO to redirect
the client to the IdP for authentication. The client is then redirected to the
RACS (Request Assertion Consumer Service), which parses the response from the
IdP and then redirects again to the original service.

2) AuthorizationTest

Authentication is the same as for the AuthenticationTest. This time however,
the IdP is configured to add an AttributeStatement to the SAML Assertion in
the Response, containing the roles of the authenticated user. This is used by
the endpoint to set up the security context.

The CXF Endpoint has also configured the SecureAnnotationsInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not).

