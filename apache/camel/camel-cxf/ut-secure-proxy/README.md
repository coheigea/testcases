camel-cxf-ut-secure-proxy
===========

A secure proxy test. A CXF JAX-WS "double-it" service requires TLS with client
authentication. A CXF proxy service also requires TLS, but with a UsernameToken
instead of client authentication. A Camel route is configured to route messages
from the proxy to the service. A client can invoke on the proxy with a
UsernameToken over TLS, the proxy authenticates the UsernameToken, and passes
the call + response to the backend service. In this way, we are delegating the
security requirements to the proxy (and ensuring the service is secure against
external calls by requiring 2-way TLS). 

