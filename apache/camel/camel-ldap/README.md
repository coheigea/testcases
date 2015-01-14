camel-ldap
===========

Some test-cases for the camel-ldap component.

1) LDAPTest

This test uses the Apache Camel File component to take files from
src/test/resources/queries. The files contain LDAP search queries. These 
search queries are then made against an Apache DS LDAP implementation started
by the test class, using the Apache Camel LDAP component. The results are then
processed, and (the attributes are) placed in files stored in target/results.

