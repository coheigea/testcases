camel-hbase
==========

This demo shows how to use Apache Camel to invoke on Apache HBase. The test
starts a HBase mini cluster, and creates a new table called "temp". It adds
two new rows to the table, "row1" and "row2".

The demo then loads in files from a "queries" directory using Camel and queries
HBase via the Camel HBase component to retrieve the value for the "col1" 
column in column family "colfam1" using the row corresponding to the message
body of the file. The results are output in target/results.

