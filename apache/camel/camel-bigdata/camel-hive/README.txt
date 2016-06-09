camel-hive
==========

This demo shows how to use Apache Camel to invoke on Apache Hive. The test
creates a new table in Apache Hive called "words" in the "default" database.
It has two columns, "word" and "count". 

The demo first populates the "words" table using a text file.

The demo then loads in files from a "queries" directory using Camel and runs
the SQL queries they contain, and then outputs the result from HIVE in
target/results.

