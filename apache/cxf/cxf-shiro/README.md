cxf-shiro
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Apache Shiro.

The shiro.ini file has two users with the following usernames/passwords/roles:

 - "alice/security/boss+employee"
 - "bob/security/employee"

1) AuthenticationTest

This tests using Shiro for authentication. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured (see
cxf-service.xml) to validate the UsernameToken via the ShiroUTValidator. A
test that passes username/passwords via Basic Authentication to the CXF
endpoint is also added.

2) AuthorizationTest

This tests using Shiro for authorization. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has configured the
ShiroUTValidator, which authenticates the Username/Password and stores the
roles.

The CXF Endpoint has also configured the ShiroUTValidator with a list of
required roles. In this case the role of "boss" is required to access the 
endpoint, which ("alice" has this role, "bob" does not). 

3) SSOTest

This test builds on the AuthenticationTest to show how SingleSignOn (SSO) can
be achieved using WS-SecureConversation. In this scenario an STS is co-located
with the endpoint. The client sends the UsernameToken to the STS for 
authentication using Apache Shiro. The STS returns a token and a secret key
to the client. The client then makes the service request including the token
and using the secret key to sign a portion of the request, thus proving
proof-of-possession. The client can then make repeated invocations without 
having to re-authenticate the UsernameToken credentials.

4) AnnotationTest

This tests using Shiro for authorization. A cxf client sends a SOAP UsernameToken to a CXF
Endpoint. The CXF Endpoint has been configured (see cxf-service.xml) to validate the UsernameToken 
via the ShiroUTValidator. 

Instead of passing the requires roles through to the ShiroUTValidator, like the authorization test
does, here we use the Shiro RequiresRoles annotation on the service endpoint implementation. The
Spring config for the service sets up the necessary interceptors so that Shiro picks up the
annotation and does the authorization check.


