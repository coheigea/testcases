camel-jmx
===========

A spring-boot deployment that doesn't deploy any routes - it's just to check the Camel JMX stuff. 

Compile with: "mvn clean install"
Set: export MAVEN_OPTS="-Dcom.sun.management.jmxremote.port=9913 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
Run: mvn spring-boot:run

Then open jconsole and connect to a remote process via either: 
 * localhost:9913
 * service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi

You should then be able to see the Camel MBeans.
