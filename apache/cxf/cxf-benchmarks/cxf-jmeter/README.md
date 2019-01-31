cxf-benchmarks-jmeter
===========

This project builds a doubleit.war file which can be deployed to an application
container such as Apache Tomcat. It contains a simple "double it" SOAP service
which is protected by various security mechanisms. The purpose of this project
is to provide a simple way of measuring throughput by a tool such as Apache
JMeter.

It contains the following endpoints:

1) /doubleit/services/doubleitnosecurity

No security at all. This can be used to test throughput over plaintext and
TLS.

2) /doubleit/services/doubleittransport

This uses a Transport binding with a UsernameToken supporting token. The
UsernameToken is just validated by a simple CallbackHandler on the service
side. 

3) /doubleit/services/doubleitsymmetric

This uses a Symmetric binding with a UsernameToken supporting token. The
UsernameToken is just validated by a simple CallbackHandler on the service
side. 

4) /doubleit/services/doubleitsymmetricstreaming

This is the same as per the symmetric case above, except that it uses the 
new streaming WS-Security implementation available in Apache CXF 3.0.0.

5) /doubleit/services/doubleitsymmetricmtom

This is the same as per the symmetric case (3), except that it enabled MTOM
and stores the encryption bytes in attachments, instead of BASE-64 encoding
them.

6) /doubleit/services/doubleitasymmetric

This uses a Asymmetric binding. Authentication is established by a certificate
chain.

7) /doubleit/services/doubleitasymmetricstreaming

Same as for the asymmetric case above, except that it uses the new streaming
WS-Security implementation available in Apache CXF 3.0.0.

8) /doubleit/services/doubleitasymmetricmtom

Same as (6) above, except that MTOM is enabled, and cipherdata is stored in
the attachments rather than BASE-64 encoded in the request.

9) /doubleit/services/doubleitwsaddr

WS-Addressing is enabled.

10) /services/STS

A SecurityTokenService endpoint, which can issue SAML Tokens. It's secured
using a UsernameToken.

11) /services/STSREST

A REST SecurityTokenService endpoint, which can issue SAML Tokens. It's secured
using HTTP BasicAuthentication.

12) /services/JWT

A REST "double-it" implementation that is secured using a JWT token.

Build the project via "mvn clean install" and copy target/doubleit.war to the
webapps folder of a container such as Tomcat. Note that some of the endpoints
need TLS enabled in Tomcat. By default, JMeter trusts all certs, so any
self-signed cert will do in Tomcat. Edit 'conf/server.xml' and add in 
something like:

 <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol" maxThreads="150" SSLEnabled="true" scheme="https" secure="true" clientAuth="false" sslProtocol="TLS" keystoreFile="idp-ssl-key.jks" keystorePass="tompass" />

Then open up JMeter and import the "DoubleIt_Users.jmx" in the root directory
of this project. Run the project and look at the Summary Report for
throughput, etc.
