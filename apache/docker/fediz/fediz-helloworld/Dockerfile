
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=0.0.2

ARG FEDIZ_VERSION=1.4.5
ARG TOMCAT_HOME=/usr/local/tomcat

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
#ADD https://repository.apache.org/content/groups/snapshots/org/apache/cxf/fediz/apache-fediz/1.4.5-SNAPSHOT/apache-fediz-1.4.5-20180905.171716-24.zip /tmp
COPY catalina.properties $TOMCAT_HOME/conf
COPY rp-ssl-key.jks $TOMCAT_HOME
COPY ststrust.jks $TOMCAT_HOME
COPY fedizhelloworld.war $TOMCAT_HOME/webapps
COPY fediz_config.xml $TOMCAT_HOME/conf
COPY server.xml $TOMCAT_HOME/conf
RUN unzip -q *.zip \
    && mkdir -p $TOMCAT_HOME/lib/fediz \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/plugins/tomcat8/lib/* $TOMCAT_HOME/lib/fediz

