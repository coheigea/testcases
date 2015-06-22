cxf-jdbc
===========

This project contains a number of tests that show how an Apache CXF service
endpoint can authenticate and authorize a client using JDBC. 

Prerequisites:

a) Launch Apache Derby

Download Apache Derby and extract it into a new directory ($DERBY_HOME). Create a directory to use to store Apache Derby databases ($DERBY_DATA). In $DERBY_DATA, create a file called 'derby.properties' with the content:

derby.connection.requireAuthentication=true
derby.user.admin=security

In other words, authentication is required, and a valid user is "admin" with password "security". Now launch Apache Derby in network mode via:

java -Dderby.system.home=$DERBY_DATA/ -jar $DERBY_HOME/lib/derbyrun.jar server start

b) Create user data

Create a new file called 'create-users.sql' with the following content:

SET SCHEMA APP;
DROP TABLE USERS;
DROP TABLE ROLES;

CREATE TABLE USERS (
  NAME   VARCHAR(20) NOT NULL PRIMARY KEY,
  PASSWORD  VARCHAR(20) NOT NULL,
  STATUS  VARCHAR(20) NOT NULL,
  SURNAME  VARCHAR(20) NOT NULL
);

INSERT INTO USERS VALUES('alice', 'security', 'true', 'yellow');
INSERT INTO USERS VALUES('bob', 'security', 'true', 'blue');

CREATE TABLE ROLES (
  NAME   VARCHAR(20) NOT NULL PRIMARY KEY,
  ROLE   VARCHAR(20) NOT NULL
);

INSERT INTO ROLES VALUES('alice', 'boss');

Launch Apache Derby via $DERBY_HOME/bin/ij. Then connect to the server via:

connect 'jdbc:derby://localhost:1527/SYNCOPE;create=true;user=admin;password=security;';

Populate user data via: run 'create-users.sql';

You can now see the user data via: select * from users;

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

