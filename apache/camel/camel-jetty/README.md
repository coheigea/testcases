camel-jetty
===========

A test-case for the Camel Jetty component, TLS, the REST DSL + Jasypt.

camel-jetty.xml defines the following:
 * A REST server using Jetty and TLS. Jasypt is used to store the TLS keystore passwords in an encrypted form.
 * The REST logic is defined using the Camel REST DSL. It directs all GET requests to /data to a separate Camel route,
    which reads in a file stored in the filesystem and returns it to the client.
 * A client route uses the Camel HTTP4 component to invoke on the REST endpoint and logs the response message to the console.


