cxf-jaxrs-jaas-sts
===========

This project demonstrates how to use the new STS JAAS LoginModule in CXF to 
authenticate and authorize a user. It contains a "double-it" module which 
contains a "double-it" JAX-RS service. It is secured with JAAS at the container
level, and requires a role of "boss" to access the service.

The "sts" module contains a Apache CXF STS web application which can 
authenticate users and issue SAML tokens with embedded roles.

Build the project with "mvn clean install" and copy both war files to Tomcat.
Copy the "jaas.conf" from the double-it module into $CATALINA_HOME/conf. Next
set the System property:

"JAVA_OPTS=-Djava.security.auth.login.config=$CATALINA_HOME/conf/jaas.conf".

Start Tomcat, open a web browser and navigate to:

http://localhost:8080/cxf-double-it/doubleit/services/100

Use credentials "alice/security" when prompted. The STS JAAS LoginModule takes
the username and password, and dispatches them to the STS for validation.
