cxf-failover
===========

This project contains a number of tests that show how to use the CXF failover
and load distribution features.

1) FailoverTest

This contains some tests relating to failover.

The first uses the CXF FailoverFeature with a SequentialStrategy. The (JAX-RS)
service endpoint is designed to return 404 on every second iteration. The
first call succeeds to PORT1 as expected. The second call fails, but "fails
over" to PORT2 successfully.

This class also contains some JAX-WS tests.

2) LoadBalancerTest

This demonstrates how to use the LoadDistributorFeature with a
SequentialStrategy. A number of invocations are made and can be seen from the
console to be distributed to the different endpoints that are configured.

