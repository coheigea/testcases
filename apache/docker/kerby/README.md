# DockerFile to create an Apache Kerby KDC server

This directory contains a Docker File to create an Apache Kerby KDC server.

 * Build with: docker build . -t coheigea/kerby
 * Run with: docker run -it 4000:88 -v `pwd`/kerby-data:/kerby-data coheigea/kerby

Once the container is running, you'll need to connect to the container to
create some principals for testing, or to update the domain:

 * docker exec -it <id> bash
 * stty rows 24 columns 80 (required to run jline in docker)
 * sh bin/kadmin.sh /kerby-data/conf/ -k /kerby-data/keytabs/admin.keytab
 * Then: addprinc -pw password alice@EXAMPLE.COM

To test the KDC you can use the MIT kinit tool. Set the KRB5_CONFIG environment
variable to point to the "krb5.conf" file included in this repository. Then:

 * kinit alice 

will get you a ticket, that can be inspected via "klist".

This DockerFile is provided as a quick and easy way to play around with the
Apache Kerby KDC. It is not intended to be used in production.

