camel-crypto
===========

Some test-cases for the camel-crypto component/data-format.

The test-cases also use the Camel JASYPT component, as all of the JKS store
and key passwords are stored encrypted in the "passwords.properties" file. The
encrypted passwords are generated using a Camel distribution, e.g.:

java -jar camel-jasypt-2.12.2.jar -c encrypt -p master-secret -i sspass

The master password is specifies in the pom.xml and read as a system property
in the configuration.

The PGP tests use keys generated via gnupg.

1) CryptoEncryptionTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to encrypt them using the Camel Crypto data format.
The encrypted files are placed in target/encrypted-data. Another route takes
files found in target/encrypted-data, decrypts them and stores the plaintext
files in target/decrypted-data.

2) CryptoSignatureTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to sign and verify them using the Camel Crypto
component. The verified files are placed in target/verified-data. The
signatures are also logged at INFO level (to the console).

3) PGPEncryptionTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to encrypt them using the Camel PGP data format.
The encrypted files are places in target/encrypted-pgp-data. Another route 
takes the files found in target/encrypted-pgp-data, decrypts them and stores
the plaintext files in target/decrypted-pgp-data.

4) PGPSignatureEncryptionTest

The same as the above, except that it also signs/verifies the documents, and
places them in target/signed-pgp-data + target/verified-pgp-data.

