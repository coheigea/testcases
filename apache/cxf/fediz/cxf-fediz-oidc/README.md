cxf-fediz-oidc
===========

This project shows how to use interceptors of Apache CXF to authenticate and
authorize clients of a JAX-RS service using OpenId Connect. It consists of two
submodules:

a) double-it:

This is a war containing the service. The service is a JAX-RS service, with a
single GET method which returns the doubled number. The method is secured with
a @RolesAllowed annotation, meaning that only a user in roles "User", "Admin",
or "Manager" can access the service. This is enforced via CXF's
SecureAnnotationsInterceptor.

In addition, the OidcRpAuthenticationFilter is used to redirect the user to an
OpenId Connnect IdP for authentication, using the OpenId Connect authorization
code flow. It also requests that the role of the user be inserted into the
IdToken, so that RBAC authorization can take place on the service.

b) fediz-sso-tomcat:

This module deploys the IdP (both OpenId Connect and WS-Federation) + STS from
Fediz in Apache Tomcat. The OIDC IdP redirects the user to the WS-Federation
based IdP for authentication.

The test code first registers a new client in the OIDC IdP, the credentials of
which are used to authenticate the client (the JAX-RS service).

To run the tests, uncomment the @Ignore annotation of the "testInBrowser"
method, and copy the printed out URL into a browser to test the service 
directly (user credentials: "alice/ecila").

