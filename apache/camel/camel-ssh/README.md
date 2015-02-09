camel-ssh
===========

Some test-cases for the camel-ssh component. The tests set up a dummy SSH
server based on Apache Mina, which allows the user to run arbitrary commands
and to return the results.

The test-cases also use the Camel JASYPT component, as the user "alice"'s 
password is stored encrypted in the "passwords.properties" file. The encrypted 
passwords are generated using a Camel distribution, e.g.:

java -jar camel-jasypt-2.12.2.jar -c encrypt -p master-secret -i security

The master password is specifies in the pom.xml and read as a system property
in the configuration.

1) SSHTest

This test uses the Apache Camel File component to take plaintext files from
src/test/resources/data which contain various unix commands. These commands
are run on the SSH server, and the results are stored in target/results.

