cxf-amazon-kms
===========

This project contains a number of tests that show how to use the AWS
Key Management Service with Apache CXF.

Prerequisities:

You must have registered for Amazon AWS, and created a user in the IAM service,
saving the access key id and secret key. You then need to create a customer
master key for this user, and save the key id. See this article for more
information:

http://coheigea.blogspot.ie/2015/06/integrating-aws-key-management-service.html

1) SymmetricActionTest

Two test-cases for a CXF endpoint using symmetric encryption (AES-128 +
AES-256). Each test-case asks a CallbackHandler for the secret key to encrypt
the request. In this case, the CallbackHandler asks the Amazon KMS (Key
Management Service) for a data encryption key, using the master key for
"alice".

Edit org/apache/coheigea/cxf/kms/symmetric/cxf-client.xml, and insert the
relevant values here:

  <bean id="amazonCallbackHandler" class="org.apache.coheigea.cxf.kms.common.CommonCallbackHandler">
       <property name="endpoint" value="https://kms.eu-west-1.amazonaws.com" />
       <property name="accessKey" value="<access key>" />
       <property name="secretKey" value="<secret key>" />
       <property name="masterKeyId" value="<master key id>" />
  </bean>

Similarly, edit org/apache/coheigea/cxf/kms/symmetric/cxf-service.xml:

 <bean id="amazonCallbackHandler" class="org.apache.coheigea.cxf.kms.common.CommonCallbackHandler">
       <property name="endpoint" value="https://kms.eu-west-1.amazonaws.com" />
       <property name="accessKey" value="<access key>" />
       <property name="secretKey" value="<secret key>" />
 </bean>

2) AsymmetricTest

A test-case for a CXF based web service using Asymmetric encryption via
WS-SecurityPolicy. The keystore password for both signature and encryption is
stored encrypted in the crypto properties files using the AWS KMS (Key
Management Service) via the KMSPasswordEncryptor class.

Edit org/apache/coheigea/cxf/kms/asymmetric/cxf-client.xml, and insert the
relevant values here:

 <bean id="kmsEncryptor" class="org.apache.coheigea.cxf.kms.asymmetric.KMSPasswordEncryptor">
       <property name="endpoint" value="https://kms.eu-west-1.amazonaws.com" />
       <property name="accessKey" value="<access key>" />
       <property name="secretKey" value="<secret key>" />
       <property name="masterKeyId" value="<master key id> " />
 </bean>

The same goes for the cxf-service.xml file for this test.



