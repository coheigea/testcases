camel-ldap
===========

Some test-cases for LDAP support in Camel via both the camel-ldap and
camel-spring-ldap components.

1) LDAPTest

This test uses the Apache Camel File component to take files from
src/test/resources/queries. The files contain LDAP search queries. These 
search queries are then made against an Apache DS LDAP implementation started
by the test class, using the Apache Camel LDAP component. The results are then
processed, and (the attributes are) placed in files stored in target/results.

2) SpringLDAPTest

This is the same as the test above, except that it uses the Camel Spring LDAP
component. This is a bit more advanced than the LDAP component above, as it
can do other things apart from searches.

