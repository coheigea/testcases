
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=0.0.2

ARG TOMCAT_HOME=/usr/local/tomcat

COPY target/cxf-sts-rest.war $TOMCAT_HOME/webapps

