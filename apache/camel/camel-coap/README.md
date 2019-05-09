camel-coap
===========

A test-case for the camel-coap component. It shows how to use the coap component with the Camel REST DSL + TLS.

camel-coap.xml defines the following:

 * A REST server using CoAP and TLS.
 * The REST logic is defined using the Camel REST DSL. It directs all GET requests to /data to a separate Camel route, which reads in a file 
   stored in the filesystem and returns it to the client.
 * A client route uses the Camel CoAP component to invoke on the REST endpoint and logs the response message to the console.

