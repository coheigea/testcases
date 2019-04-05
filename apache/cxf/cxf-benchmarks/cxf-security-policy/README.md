cxf-benchmarks-security-policy
===========

This project builds a doubleit.war file which can be deployed to an application
container such as Apache Tomcat. It contains a simple "double it" SOAP service
with two operations:

 *  * doubleIt - this is not secured
 *  * doubleItSecured - secured using a WS-SecurityPolicy AsymmetricBinding.

The purpose of the project is to demonstrate a performance gain in CXF 3.3.2. Up
until then it would always apply the SAAJInInterceptor to convert the request
body to DOM, even for the case that is not secured. Since CXF 3.3.2, if there is
no security header for the case that we have some security policies in effect,
it will not apply the SAAJInInterceptor. 

Then open up JMeter and import the "DoubleItPolicy.jmx" in the root directory
of this project. Run the project and look at the Summary Report for
throughput, etc. The "NoSecurity" testcase uses an encoded version of the
WS-Security spec as part of the payload to demonstrate performance using
a large paylod.
