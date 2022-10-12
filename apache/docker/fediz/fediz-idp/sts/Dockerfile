
FROM tomcat:9-jdk8-corretto

LABEL maintainer-twitter="@coheigea"
LABEL version=1.6.0

ARG FEDIZ_VERSION=1.6.0
ARG TOMCAT_HOME=/usr/local/tomcat

RUN yum -y install unzip && yum -y clean all && rm -rf /var/cache

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
COPY server.xml $TOMCAT_HOME/conf
COPY idp-ssl-key.jks $TOMCAT_HOME
COPY idp-ssl-trust.jks $TOMCAT_HOME
RUN unzip -q *.zip \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/idp/war/fediz-idp-sts.war $TOMCAT_HOME/webapps

