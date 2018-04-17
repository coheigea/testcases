# Project to create an Apache Ranger admin server via docker

This project contains the configuration required to run the Apache Ranger
admin server in docker. The "ranger-postgres" directory contains a Docker File
to set up a Postgres database for Apache Ranger. The "ranger-admin" directory
contains a Docker File to set up the Apache Ranger admin server.

Build the docker images as follows:

 * (In ranger-postgres) docker build . -t coheigea/ranger-postgres
 * (In ranger-admin) docker build . -t coheigea/ranger-admin

Note: The ranger-admin docker image takes a long time due to having to build
the source code using Apache Maven - and hence it needs to download a large
amount of dependencies.

The simplest way to run the project is to use Docker Compose as follows:

 * docker-compose up

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
