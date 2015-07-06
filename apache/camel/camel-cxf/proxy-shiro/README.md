camel-cxf-proxy-shiro-demo
===========

Some authentication and authorization tests for an Apache CXF proxy service
using the Apache Camel Shiro component.

The tests use the following set up. A CXF JAX-WS "double-it" service requires
TLS with client authentication. A CXF proxy service also requires TLS, but
with a UsernameToken instead of client authentication. A Camel route is
configured to route messages from the proxy to the service. A client can
invoke on the proxy with a UsernameToken over TLS, however the proxy is
configured not to authenticate the UsernameToken, but just to process it and
store the Subject on the message. The tests then use Apache Shiro to
authenticate or authorize the call, and if successful pass the call and
response to/from the backend service.

1) AuthenticationTest

Here the Camel route instantiates a Processor that takes the Subject name +
password from the appropriate Camel header and authenticates it to Apache
Shiro.

2) AuthorizationTest

Does the same as the AuthenticationTest, except that the ShiroSecurityPolicy
associated with the route also required a role of "boss". User "alice" has this
role, and user "bob" does not.

