
FROM tomcat:9-jdk8-corretto

LABEL maintainer-twitter="@coheigea"
LABEL version=1.6.0

ARG FEDIZ_VERSION=1.6.0
ARG TOMCAT_HOME=/usr/local/tomcat

RUN yum -y install unzip && yum -y clean all && rm -rf /var/cache

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
COPY catalina.properties $TOMCAT_HOME/conf
COPY rp-ssl-key.jks $TOMCAT_HOME
COPY fedizhelloworld.war $TOMCAT_HOME/webapps
COPY fediz_config.xml $TOMCAT_HOME/conf
COPY server.xml $TOMCAT_HOME/conf
COPY keycloak.cert $TOMCAT_HOME
RUN unzip -q *.zip \
    && mkdir -p $TOMCAT_HOME/lib/fediz \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/plugins/tomcat/lib/* $TOMCAT_HOME/lib/fediz 

