cxf-ldap
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using LDAP.

1) Pre-requisites

An LDAP directory server must be configured and started for these tests to run.
Follow section "1) Apache DS" at the following link to start Apache Directory
Server with the given ldif configuration file:

http://coheigea.blogspot.ie/2013/08/apache-syncope-tutorial-part-iii.html

The tests require two users in the directory username/password/roles:

 - "alice/security/boss+employee"
 - "bob/security/employee"

2) AuthenticationTest

This tests using LDAP for authentication. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which dispatches the username/passwords to the
directory server for authentication via LDAP. 

Tests are added for authentication using both Sun and Jetty's LdapLoginModules.

3) AuthorizationTest

This tests using LDAP for authorization. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which authenticates the Username/Password to the
directory and retrieves the roles for authorization.

The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not). 

