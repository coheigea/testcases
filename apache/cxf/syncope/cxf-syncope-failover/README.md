cxf-syncope-failover
====================

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using Apache Syncope. 

1) Pre-requisites

The tests assume two Apache Syncope instances (tested with 1.2.6) running on
localhost ports 8080 + 9080. The tests will deploy user data to the first
Syncope instance (8080). As the Syncope instances will be clustered, the
second Syncope instance should work as well.

Download the Syncope standalone distribution and unzip it. Change the port in
conf/server.xml from "9080" to "8080". 

Open webapps/syncope/WEB-INF/classes/content.xml + delete from "sample policies"
down to "Authentication and authorization".

Open webapps/syncope/WEB-INF/classes/persistence.properties + change the 
"jpa.url" to store the database in a file rather than in-memory:

jpa.url=jdbc:h2:/tmp/syncopedb;DB_CLOSE_DELAY=-1


2) AuthenticationTest

This tests using Syncope as an IDM for authentication. A CXF client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
SyncopeUTValidator, which dispatches the username/passwords to Syncope for
authentication via Syncope's REST API. A test that passes username/passwords 
via Basic Authentication to the CXF endpoint is also added.

