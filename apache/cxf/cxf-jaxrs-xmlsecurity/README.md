cxf-jaxrs-xmlsecurity
===========

This project contains a number of tests that show how to use the XML Security
functionality in Apache CXF to sign and encrypt XML payloads to/from a 
JAX-RS service.

1) XML Signature

There are a number of tests that demonstrate how to use XML Signature with
Apache CXF JAX-RS clients and endpoints:

 - XMLSignatureDOMTest - Use the DOM implementation of XML Signature
 - XMLSignatureStaxTest - Use the StAX implementation of XML Signature
 - XMLSignatureInteropTest - An interop test for the different stacks

