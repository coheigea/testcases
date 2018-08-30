# Project to deploy a CXF REST STS web application in docker

This project contains a web application which is an Apache CXF Security Token
Service (STS). Clients can authenticate to the STS by HTTP Basic
Authentication (user: "alice", password: "security"), and obtain a SAML or
JWT token in return via the REST interface of the STS.

To build:

 * mvn clean install
 * docker build -t coheigea/cxf-sts-rest .

To run:

 * docker run -p 8080:8080 coheigea/cxf-sts-rest

To test:
 * To get a SAML token: 

   http://localhost:8080/cxf-sts-rest/SecurityTokenService/token/saml 

 * To get a JWT:

   http://localhost:8080/cxf-sts-rest/SecurityTokenService/token/jwt

 * To get a (plain) JWT without any wrapping using curl:

   curl -u alice:security -H "Accept: text/plain" -k http://localhost:8080/cxf-sts-rest/SecurityTokenService/token/jwt

This project is provided as a quick and easy way to play around with the
Apache CXF STS. It should not be deployed in production as it uses default
security credentials, etc.

