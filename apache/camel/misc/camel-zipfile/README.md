camel-zipfile
===========

A test-case for the camel-zipfile component using Spring-Boot:

 * Build: mvn clean install
 * Run: mvn spring-boot:run
 
XML files are read in from src/main/resources/data and then converted to zip files, which are stored in
target/results. In addition, a zip file is read in from src/main/resources/zippeddata and unzipped, with
the file contents placed into target/plaintextresults.

