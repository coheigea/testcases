
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=1.4.5

ARG FEDIZ_VERSION=1.4.5
ARG TOMCAT_HOME=/usr/local/tomcat

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
COPY server.xml $TOMCAT_HOME/conf
COPY idp-ssl-key.jks $TOMCAT_HOME
COPY ststrust.jks $TOMCAT_HOME
COPY fediz_config.xml $TOMCAT_HOME/conf
COPY catalina.properties $TOMCAT_HOME/conf
ADD https://repo.maven.apache.org/maven2/org/hsqldb/hsqldb/2.4.1/hsqldb-2.4.1.jar $TOMCAT_HOME/lib
RUN unzip -q *.zip \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/idp/war/fediz-oidc.war $TOMCAT_HOME/webapps \
    && unzip -q $TOMCAT_HOME/webapps/fediz-oidc.war -d $TOMCAT_HOME/webapps/fediz-oidc \
    && mkdir -p $TOMCAT_HOME/lib/fediz \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/plugins/tomcat8/lib/* $TOMCAT_HOME/lib/fediz

COPY data-manager.xml $TOMCAT_HOME/webapps/fediz-oidc/WEB-INF

#ADD http://repo1.maven.org/maven2/org/slf4j/slf4j-jdk14/1.7.25/slf4j-jdk14-1.7.25.jar $TOMCAT_HOME/lib/fediz
#COPY logging.properties $TOMCAT_HOME/webapps/fediz-oidc/WEB-INF/classes

