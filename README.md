cxf-syncope
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Apache Syncope.

1) Prequisites

The tests assume an Apache Syncope instance (tested with 1.0.5) with a REST
interface accessible at "http://localhost:9080/syncope/rest/" (this can be
changed in 'cxf-service.xml' for each test). The administrator
username/password is the default username/password used in a Syncope instance
("admin"/"password"). 

To create a new Syncope project enter:

mvn archetype:generate -DarchetypeGroupId=org.apache.syncope -DarchetypeArtifactId=syncope-archetype -DarchetypeRepository=http://repo1.maven.org/maven2 -DarchetypeVersion=1.0.5

Go into the created project and copy:

cp core/src/main/resources/content.xml core/src/test/resources/

This will remove the test configuration and provide you with a blank slate 
when launching Syncope in embedded mode. Package everything up via "mvn clean
package". Then go into "console" and type "mvn -Pembedded". You can access
the console via "http://localhost:9080/syncope-console", and username/password
"admin/password".

You must create new roles called "boss" and "employee".

You must then create two users in Syncope with username/password/roles:
 - "dave/password/boss"
 - "harry/password/employee"

2) AuthenticationTest

This tests using Syncope as an IDM for authentication. A cxf client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
SyncopeUTValidator, which dispatches the username/passwords to Syncope for
authentication via Syncope's REST API.

3) AuthorizationTest

This tests using Syncope as an IDM for authorization. A cxf client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the SyncopeUTValidator,
which dispatches it to Syncope for authentication.

The CXF Endpoint has configured the SyncopeRolesInterceptor, which gets the
roles of the user and stores them in a Subject.
  
The CXF Endpoint has configured the SimpleAuthorizingInterceptor, which
requires that a user must have role "boss" to access the "doubleIt"
operation ("dave" has this role, "harry" does not).

