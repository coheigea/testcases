santuario-serializer-benchmark
===========

This project contains two Junit tests used for benchmarking XML Encryption. In
particular, they measure memory and timing performance for both encryption
and decryption, ranging from small to very large XML files.

Santuario supports the Serializer interface, which allows you to plug in
custom implementations to serialize data for XML encryption. The getXMLCipher()
method in AbstractPerformanceTest shows how to plug in different serializer
implementations, from the default (TransformSerializer), to DocumentSerializer,
to the new StaxSerialize in CXF.

