
# A sample project to deploy the Fediz IdP

This directory contains a way of easily deploying a sample Apache CXF Fediz
IdP to docker for testing purposes.

Build it via:

 * cd sts; docker build -t coheigea/fediz-sts .
 * cd idp; docker build -t coheigea/fediz-idp .
 * cd oidc; docker build -t coheigea/fediz-oidc .
 * docker-compose up

This project is provided as a quick and easy way to play around with the
Apache CXF Fediz IdP. It should not be deployed in production as it uses
default security credentials, etc.
