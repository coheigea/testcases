# Project to create an Apache Ranger admin server via docker

The ranger-postgres directory contains a Docker File to set up a Postgres
database for Apache Ranger.

The simplest way to get started is simply to run the following command:

 * docker-compose up

If you want to build + run manually:

 * Build with: cd ranger-postgres; docker build . -t coheigea/ranger-postgres
 * Create a network so that we can link containers: docker network create my-network
 * Run with: docker run -p 5432:5432 --name postgres-server --network my-network coheigea/ranger-postgres

The ranger-admin directory contains a Docker File to set up the Apache Ranger
admin server.

 * Build with: cd ranger-admin; docker build . -t coheigea/ranger-admin
 * Run with: docker run -p 6080:6080 -it --network my-network coheigea/ranger-admin

TODO:

Put this part into a script:

 * ./setup.sh
 * ranger-admin start

Once the Ranger admin server is started then open a browser and navigate to:

 * http://localhost:6080 (credentials: admin/admin)

This project is provided as a quick and easy way to play around with the
Apache Ranger admin service. It should not be deployed in production as
it uses default security credentials, it is not secure with kerberos, auditing
is not enabled, etc.
