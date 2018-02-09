
FROM openjdk:8u121-jdk-alpine

LABEL maintainer-twitter="@coheigea"
LABEL version=2.0.0

ARG SENTRY_VERSION=2.0.0
ARG HADOOP_VERSION=2.7.5

WORKDIR /opt
ADD http://repo1.maven.org/maven2/org/apache/sentry/sentry-dist/${SENTRY_VERSION}/sentry-dist-${SENTRY_VERSION}-bin.tar.gz /opt
ADD http://ftp.heanet.ie/mirrors/www.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz /opt

RUN tar zxvf sentry-dist-${SENTRY_VERSION}-bin.tar.gz \
 && tar zxvf hadoop-${HADOOP_VERSION}.tar.gz \
 && apk add --no-cache bash

COPY sentry-site.xml /opt/apache-sentry-${SENTRY_VERSION}-bin
COPY sentry.ini /opt/apache-sentry-${SENTRY_VERSION}-bin

WORKDIR /opt/apache-sentry-${SENTRY_VERSION}-bin

ENV HADOOP_HOME=/opt/hadoop-${HADOOP_VERSION}
RUN bin/sentry --command schema-tool --conffile sentry-site.xml --dbType derby --initSchema

EXPOSE 8038
ENTRYPOINT ["bin/sentry", "--command", "service", "-c", "sentry-site.xml"]
