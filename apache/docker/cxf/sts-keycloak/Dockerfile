FROM tomcat:9-jdk11-corretto

LABEL maintainer-twitter="@coheigea"
LABEL version=0.0.2

ARG TOMCAT_HOME=/usr/local/tomcat

COPY target/cxf-sts-keycloak.war $TOMCAT_HOME/webapps

