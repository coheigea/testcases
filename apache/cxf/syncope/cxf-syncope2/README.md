cxf-syncope2
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Apache Syncope. Earlier
versions of this test-case required that you download and deploy an Apache
Syncope instance with specific users + roles installed. However, the latest
version creates and launches an Apache Syncope instance, and populates it
with the correct data for the tests to run.

Note that this differs from the cxf-syncope project in that this project 
loads up a Syncope 2.0.0-SNAPSHOT instance.

1) AuthenticationTest

This tests using Syncope as an IDM for authentication. A CXF client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
SyncopeUTValidator, which dispatches the username/passwords to Syncope for
authentication via Syncope's REST API. A test that passes username/passwords 
via Basic Authentication to the CXF endpoint is also added.

2) AuthorizationTest

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

3) KarafLoginModuleTest

This test does the same as the AuthorizationTest above, except that it uses
the new SyncopeLoginModule in Apache Karaf instead. The CXF
JAASAuthenticationFeature is set on the service bus, which selects the "karaf"
JAAS realm by default. The JAAS configuration file is simply:

karaf {
    org.apache.karaf.jaas.modules.syncope.SyncopeLoginModule required
    debug="true"
    address="http://localhost:${syncope.port}/syncope/cxf";
};

