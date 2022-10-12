
FROM tomcat:9-jdk8-corretto

LABEL maintainer-twitter="@coheigea"
LABEL version=0.0.3

ARG TOMCAT_HOME=/usr/local/tomcat

COPY target/cxf-sts-rest.war $TOMCAT_HOME/webapps

