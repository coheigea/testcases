
FROM tomcat:8.5

LABEL maintainer-twitter="@coheigea"
LABEL version=1.4.5

ARG FEDIZ_VERSION=1.4.5
ARG TOMCAT_HOME=/usr/local/tomcat

WORKDIR /tmp
ADD https://repo.maven.apache.org/maven2/org/apache/cxf/fediz/apache-fediz/$FEDIZ_VERSION/apache-fediz-$FEDIZ_VERSION.zip /tmp
COPY server.xml $TOMCAT_HOME/conf
COPY idp-ssl-key.jks $TOMCAT_HOME
ADD https://repo.maven.apache.org/maven2/org/hsqldb/hsqldb/2.4.1/hsqldb-2.4.1.jar $TOMCAT_HOME/lib
RUN unzip -q *.zip \
    && cp /tmp/apache-fediz-$FEDIZ_VERSION/idp/war/fediz-idp.war $TOMCAT_HOME/webapps \
    && unzip -q $TOMCAT_HOME/webapps/fediz-idp.war -d $TOMCAT_HOME/webapps/fediz-idp \
    && sed -i 's/localhost:0/sts:8443/' $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/config/* \
    && sed -i 's/localhost:0/sts:8443/' $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/idp-servlet.xml

COPY oidc.cert $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/classes
COPY entities-realmb.xml $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/classes

# Uncomment to support using SAML SSO instead with the RP application
#COPY entities-realma.xml $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/classes
#COPY mytomrpkey.cert $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/classes

# Uncomment to support switching the IdP to use entities-realmb.xml instead
#COPY realm.properties $TOMCAT_HOME/webapps/fediz-idp/WEB-INF/classes


