
FROM openjdk:8u121-jdk

LABEL maintainer-twitter="@coheigea"
LABEL version=0.7.1

ARG RANGER_VERSION=0.7.1

ADD https://jdbc.postgresql.org/download/postgresql-42.2.1.jar /opt

# TODO when I have the below working
#ADD http://ftp.heanet.ie/mirrors/www.apache.org/dist/ranger/0.7.1/apache-ranger-${RANGER_VERSION}.tar.gz /opt

#RUN apk add --no-cache python2 \
  #&& tar zxvf apache-ranger-${RANGER_VERSION}.tar.gz \
  #&& cd apache-ranger-${RANGER_VERSION} \
  #&& mvn assembly:assembly -DskipTests \
  #&& cp target/ranger-${RANGER_VERSION}-admin.tar.gz /opt \
  #&& tar zxvf ranger-${RANGER_VERSION}-admin.tar.gz

# Just work with the built Ranger admin for now
COPY ranger-${RANGER_VERSION}-admin.tar.gz /opt
COPY ranger-entrypoint.sh /opt

WORKDIR /opt

RUN apt-get -q update && apt-get install -y -q python \
 && tar zxvf /opt/ranger-${RANGER_VERSION}-admin.tar.gz \
 && chmod +x /opt/ranger-entrypoint.sh

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV RANGER_HOME=/opt/ranger-${RANGER_VERSION}-admin

COPY install.properties ${RANGER_HOME}

EXPOSE 6080

ENTRYPOINT ["/opt/ranger-entrypoint.sh"]
