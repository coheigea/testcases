
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=1.4.5

ARG FEDIZ_VERSION=1.4.5
ARG TOMCAT_HOME=/usr/local/tomcat

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
COPY server.xml $TOMCAT_HOME/conf
COPY idp-ssl-key.jks $TOMCAT_HOME
COPY idp-ssl-trust.jks $TOMCAT_HOME
RUN unzip -q *.zip \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/idp/war/fediz-idp-sts.war $TOMCAT_HOME/webapps

