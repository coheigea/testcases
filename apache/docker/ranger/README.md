# Project to create an Apache Ranger admin server via docker

This project contains the configuration required to run the Apache Ranger
admin server in docker. The "ranger-postgres" directory contains a Docker File
to set up a Postgres database for Apache Ranger. The "ranger-admin" directory
contains a Docker File to set up the Apache Ranger admin server.

The simplest way to get started is to do the following:

 * docker-compose up

Alternatively, you can build the docker images as follows:

 * cd ranger-postgres; docker build . -t coheigea/ranger-postgres; cd ..
 * cd ranger-admin; docker build . -t coheigea/ranger-admin; cd ..
   (Note: ranger-admin takes a long time due to having to build the source
          code using maven - and hence it needs to download a load of 
          dependencies)

To run manually:

 * Create a network so that we can link containers: docker network create my-network
 * Run with: docker run -p 5432:5432 --name postgres-server --network my-network coheigea/ranger-postgres
 * Run with: docker run -p 6080:6080 -it --network my-network coheigea/ranger-admin

Once the Ranger admin server is started then open a browser and navigate to:

 * http://localhost:6080 (credentials: admin/admin)

This project is provided as a quick and easy way to play around with the
Apache Ranger admin service. It should not be deployed in production as
it uses default security credentials, it is not secure with kerberos, auditing
is not enabled, etc.
