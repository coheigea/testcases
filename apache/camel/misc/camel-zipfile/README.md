camel-zipfile
===========

A test-case for the camel-zipfile component using Spring-Boot. You can build and run the tests via 
"mvn clean install".
 
XML files are read in from target/plaintextdata and then converted to zip files, which are stored in
target/results. In addition, a zip file is read in from target/zippeddata and unzipped, with
the file contents placed into target/plaintextresults.

The test copies some plaintext + zipped files into the relevant target directories to trigger the
routes.

