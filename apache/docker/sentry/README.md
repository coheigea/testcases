# DockerFile to create an Apache Sentry security server

This directory contains a Docker File to create an Apache Sentry security
server.

 * Build with: docker build . -t coheigea/sentry
 * Run with: docker run -p 8038:8038 coheigea/sentry

This image ships with two sample configuration files (sentry.ini and 
sentry-site.xml). 'sentry.ini' allows a user called "user" the permission to
do anything. 'sentry-site.xml' defines where the metadata database is that
holds the various permissions and defines that 'sentry.ini' is to be used
to retrieve user groups etc.

Once the container is running, you'll need to connect to the container and
update 'sentry.ini' to replace 'user' with the username you are using to
invoke on the Sentry security service:

 * docker exec -it <id> bash

This DockerFile is provided as a quick and easy way to play around with the
Apache Sentry security service. It should not be deployed in production as
it uses hard-coded security credentials in the configuration files.
