camel-xmlsecurity
===========

Some test-cases for the camel-xmlsecurity component.

The test-cases also use the Camel JASYPT component, as all of the JKS store
and key passwords are stored encrypted in the "passwords.properties" file. The
encrypted passwords are generated using a Camel distribution, e.g.:

java -jar camel-jasypt-2.12.2.jar -c encrypt -p master-secret -i sspass

The master password is specifies in the pom.xml and read as a system property
in the configuration.

1) XMLEncryptionTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to encrypt (the credit card information contained
in the files using the Camel XML Security component. The encrypted files are
places in target/encrypted-data. Another route takes files found in
target/encrypted-data, decrypts the credit card information and stores the
plaintext files in target/decrypted-data.

2) XMLSignatureTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to sign (the credit card information contained
in the files using the Camel XML Security component. The signed files are
places in target/signed-data. Another route takes files found in
target/signed-data, verifies the credit card information and stores the
plaintext files in target/verified-data.

