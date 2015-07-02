camel-ssh
===========

Some test-cases for the various components in Apache Camel that deal with
SSH/SCP/SFP. All the tests set up a dummy SSH server based on Apache Mina.

The test-cases also use the Camel JASYPT component, as the user "alice"'s 
password is stored encrypted in the "passwords.properties" file. The encrypted 
passwords are generated using a Camel distribution, e.g.:

java -jar camel-jasypt-2.12.2.jar -c encrypt -p master-secret -i security

The master password is specifies in the pom.xml and read as a system property
in the configuration.

1) SSHTest

This test uses the Camel SSH component. The SSH server for this test simply
allows the user to run arbitrary commands and returns the results.

This test uses the Apache Camel File component to take plaintext files from
src/test/resources/ssh_data which contain various unix commands. These commands
are run on the SSH server, and the results are stored in target/ssh_results.

2) SCPTest

This test uses the Camel JSCH component. The SSH server for this test supports
SCP.

This test uses the Apache Camel File component to take XML files from
src/test/resources/scp_data. These files are then copied using SCP to a
directory on the SSH server (target/storage) using the camel-jsch component.

2) SFTPTest

This test uses the Camel FTP component. The SSH server for this test supports
SFTP.

This test uses the Apache Camel File component to take XML files from
src/test/resources/scp_data. These files are then copied using SFTP to a
directory on the SSH server (target/storage_sftp) using the camel-ftp
component.

