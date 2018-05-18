
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=0.0.1

ARG FEDIZ_VERSION=1.4.4
ARG TOMCAT_HOME=/usr/local/tomcat

WORKDIR /tmp
# TODO - Add official 1.4.4 zip when it is released
ADD https://repository.apache.org/content/groups/snapshots/org/apache/cxf/fediz/apache-fediz/1.4.4-SNAPSHOT/apache-fediz-1.4.4-20180517.181940-18.zip /tmp
COPY catalina.properties $TOMCAT_HOME/conf
COPY rp-ssl-key.jks $TOMCAT_HOME
COPY fedizhelloworld.war $TOMCAT_HOME/webapps
COPY fediz_config.xml $TOMCAT_HOME/conf
COPY server.xml $TOMCAT_HOME/conf
COPY keycloak.cert $TOMCAT_HOME
RUN unzip -q *.zip \
    && mkdir -p $TOMCAT_HOME/lib/fediz \
    && cp /tmp/apache-fediz-1.4.4-SNAPSHOT/plugins/tomcat8/lib/* $TOMCAT_HOME/lib/fediz 

