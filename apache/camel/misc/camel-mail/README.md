camel-mail
===========

A test-case for the camel-mail component. XML files are read in from
src/test/resources/data and then mailed to an smtp mail server. A second
route reads mails via pop3 and stores them in target/results.

Note that to get this to work you will need to specify an SMTP/POP3 server
with valid user credentials.

