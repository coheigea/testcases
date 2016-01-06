cxf-syncope-failover
====================

This project contains a number of tests that show how an Apache CXF service
endpoint can use the CXF Failover feature, to authenticate to different Apache
Syncope instances.

1) Pre-requisites

1.a) Mysql

Install MySQL in $SQL_HOME and create a new user for Apache Syncope. We will create a new user "syncope_user" with password "syncope_pass". Start MySQL and create a new Syncope database:

    Start: sudo $SQL_HOME/bin/mysqld_safe --user=mysql
    Log on: $SQL_HOME/bin/mysql -u syncope_user -p
    Create a Syncope database: create database syncope; 

1.b) Apache Tomcat

Download Apache Tomcat + extract it twice (calling it first-instance + 
second-instance). In both, edit 'conf/context.xml', and uncomment the the
"<Manager pathname="" />" configuration. Add the following configuration:

<Resource name="jdbc/syncopeDataSource" auth="Container"
    type="javax.sql.DataSource"
    factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
    testWhileIdle="true" testOnBorrow="true" testOnReturn="true"
    validationQuery="SELECT 1" validationInterval="30000"
    maxActive="50" minIdle="2" maxWait="10000" initialSize="2"
    removeAbandonedTimeout="20000" removeAbandoned="true"
    logAbandoned="true" suspectTimeout="20000"
    timeBetweenEvictionRunsMillis="5000" minEvictableIdleTimeMillis="5000"
    jdbcInterceptors="org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;
    org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer"
    username="syncope_user" password="syncope_pass"
    driverClassName="com.mysql.jdbc.Driver"
    url="jdbc:mysql://localhost:3306/syncope?characterEncoding=UTF-8"/>

Edit 'conf/server.xml' of the second instance, and change the port to "9080",
and change the other ports to avoid conflict with the first Tomcat instance.

Edit 'conf/tomcat-users.xml' of both instances and add the following:

<role rolename="manager-script"/>
<user username="manager" password="s3cret" roles="manager-script"/>

Next download the MySQL JDBC jar and place it in the lib directory of both
Tomcat instances.

Next edit 'webapps/syncope/WEB-INF/classes/persistenceContextEMFactory.xml' and
change the openjpa.RemoteCommitProvider to:

<entry key="openjpa.RemoteCommitProvider" value="tcp(Port=12345, Addresses=127.0.0.1:12345;127.0.0.1:12346)"/>

and for the second instance:

<entry key="openjpa.RemoteCommitProvider" value="tcp(Port=12346, Addresses=127.0.0.1:12345;127.0.0.1:12346)"/>

1.c) Install Syncope

Download and run the Syncope installer:

www.apache.org/dyn/closer.cgi/syncope/1.2.6/syncope-installer-1.2.6-uber.jar

Install it to Tomcat using MySQL as the database. For more info on this, follow this link:

coheigea.blogspot.ie/2014/11/apache-syncope-12-tutorial-part-i.html

Run this twice to install Syncope in both Apache Tomcat instances.

1.d) Add users

In the first Tomcat instance running on 8080, go to http://localhost:8080/syncope-console, and add two new roles "employee" and "boss". Add two new users, "alice" and "bob" both with password "security". "alice" has both roles, but "bob" is only an "employee".

2) FailoverAuthenticationTest

This tests using Syncope as an IDM for authentication. A CXF client sends a
SOAP UsernameToken to a CXF Endpoint. The CXF Endpoint has been configured
(see cxf-service.xml) to validate the UsernameToken via the
SyncopeUTValidator, which dispatches the username/passwords to Syncope for
authentication via Syncope's REST API. 

The SyncopeUTValidator is configured with the address of the primary Syncope
instance ("first-instance" above running on 8080). It is also configured with
a list of alternative addresses to try if the first instance is down (in this
case the "second-instance" running on 9080). 

The test makes two invocations. The first should successfully authenticate to
the first Syncope instance. Now the test sleeps for 10 seconds after prompting
you to kill the first Syncope instance. It should successfully failover to the
second Syncope instance.

