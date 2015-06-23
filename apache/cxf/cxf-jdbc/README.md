cxf-jdbc
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using JDBC. Each test
launches an in-memory Apache Derby instance to use as the database, and
populates it with a user and roles table in 'create-users.sql'.

1) AuthenticationTest

This tests using JDBC for authentication. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which dispatches the username/passwords to the
database for authentication via JDBC. 

2) AuthorizationTest

This tests using JDBC for authorization. A CXF client sends a SOAP
UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
JAASUsernameTokenValidator, which authenticates the Username/Password to the
database and retrieves the roles for authorization.

The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not). 

