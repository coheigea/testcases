cxf-ldap
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using LDAP. An Apache DS
LDAP instance is started with both tests with some pre-existing user and
group information.

1) AuthenticationTest

This tests using LDAP for authentication. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which dispatches the username/passwords to the
directory server for authentication via LDAP. 

Tests are added for authentication using Sun, Jetty and Karaf's
LdapLoginModules.

2) AuthorizationTest

This tests using LDAP for authorization. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which authenticates the Username/Password to the
directory and retrieves the roles for authorization.

The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not). 

Tests have been added for both the Jetty and Karaf LdapLoginModules.
