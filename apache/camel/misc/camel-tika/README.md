camel-tika
===========

A test-case for the camel-tika component. Some files (XML + PDF) are read
in from src/test/resources/data, and their content is "detected" by Apache
Tika. 

The original filename and associated file type are stored in target/results.txt, e.g:

plaintext.xml - application/xml
plaintext.pdf - application/pdf

