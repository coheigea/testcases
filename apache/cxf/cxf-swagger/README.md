
cxf-jaxrs-swagger-demo
===========

This project contains a test which creates a trivial JAX-RS endpoint using
Apache CXF which is decribed by Swagger.

Open CXFSwaggerTest and remove the "@org.junit.Ignore" annotation to run. It
prints out 3 URLs and sleeps for 2 minutes to allow time to explore the URLs:

 * http://localhost:9001/swagger.json - Swagger 2 JSON
 * http://localhost:9001/openapi.json - OpenAPI JSON
 * http://localhost:9001/api-docs?url=/swagger.json - Swagger UI

