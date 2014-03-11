cxf-shiro
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Spring Security

The security_context.xml file has two users with the following
usernames/passwords/roles:

 - "alice/security/boss+employee"
 - "bob/security/employee"

2) AuthenticationTest

This tests using Spring Security for authentication. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured (see
cxf-service.xml) to validate the UsernameToken via the
SpringSecurityUTValidator. A test that passes username/passwords via Basic
Authentication to the CXF endpoint is also added.

3) AuthorizationTest

This tests using Spring Security for authorization. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has configured the
SpringSecurityUTValidator, which authenticates the Username/Password and
stores the roles.

The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not). 

