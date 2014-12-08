camel-crypto
===========

Some test-cases for the camel-crypto component/data-format.

1) CryptoEncryptionTest

This test uses the Apache Camel File component to take plaintext xml files from
src/test/resources/data and to encrypt them using the Camel Crypto data format.
The encrypted files are placed in target/encrypted-data. Another route takes
files found in target/encrypted-data, decrypts them and stores the plaintext
files in target/decrypted-data.

