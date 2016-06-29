cxf-fediz-saml-sso
===========

This project shows how to use the SAML SSO interceptors of Apache CXF to
authenticate and authorize clients of a JAX-RS service.
It consists of two submodules:

a) double-it:

This is a war containing the service. The service is a JAX-RS service, with a
single GET method which returns the doubled number. The method is secured with
a @RolesAllowed annotation, meaning that only a user in roles "User", "Admin",
or "Manager" can access the service. This is enforced via CXF's
SecureAnnotationsInterceptor.

In addition, the SamlRedirectBindingFilter is used to redirect the user to an
IdP for authentication, and subsequently parse the response + set up a
security context. The user can then access the service repeatedly without
having to re-authenticate.

b) fediz-sso-tomcat:

This module deploys the IdP + STS from Fediz in Apache Tomcat. It then takes
the "double-it" war above and also deployed it in Tomcat.

Finally, it uses Htmlunit to make an invocation on the service, and checks
that access is granted to the service. Alternatively, you can comment the
@Ignore annotation of the "testInBrowser" method, and copy the printed out
URL into a browser to test the service directly (user credentials:
"alice/ecila").

