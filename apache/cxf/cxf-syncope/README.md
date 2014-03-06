cxf-syncope
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Apache Syncope.

1) Pre-requisites

The tests assume an Apache Syncope instance (tested with 1.1.6) with a REST
interface accessible at "http://localhost:9080/syncope/rest/" (this can
be changed in 'cxf-service.xml' for each test). The administrator
username/password is the default username/password used in a Syncope instance
("admin"/"password"). 

See the following tutorial for an example of how to set up Apache Syncope
in a standalone deployment:

http://coheigea.blogspot.ie/2013/07/apache-syncope-tutorial-part-i_26.html

The tutorial referenced above goes on to synchronize user information from an
SQL and LDAP backend. The latter contains the information used for 
authentication and authorization in this test. Alternatively, you can 
start the Syncope console and enter the following:

You must create new roles called "boss" and "employee".

You must then create two users in Syncope with username/password/roles:
 - "alice/security/boss+employee"
 - "bob/security/employee"

2) AuthenticationTest

This tests using Syncope as an IDM for authentication. A CXF client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
SyncopeUTValidator, which dispatches the username/passwords to Syncope for
authentication via Syncope's REST API. A test that passes username/passwords 
via Basic Authentication to the CXF endpoint is also added.

3) AuthorizationTest

This tests using Syncope as an IDM for authorization. A CXF client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has configured the
SyncopeRolesInterceptor, which authenticates the Username/Password to Syncope
as per the authentication test. If authentication is successful, it then gets
the roles of the user and populates a CXF SecurityContext with the user's name
+ roles.

The CXF Endpoint has also configured the SimpleAuthorizingInterceptor, which
reads the current Subject's roles from the SecurityContext, and requires that
a user must have role "boss" to access the "doubleIt" operation ("alice" has
this role, "bob" does not). 

