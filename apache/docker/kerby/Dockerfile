
FROM maven:3.5.2-jdk-8-alpine

LABEL maintainer-twitter="@coheigea"

ARG KERBY_VERSION=1.1.0
ARG KERBY_HOME=/usr/local/kerby-${KERBY_VERSION}
ARG KERBY_DATA=/usr/local/kerby-data

WORKDIR /tmp
ADD http://repo1.maven.org/maven2/org/apache/kerby/kerby-all/${KERBY_VERSION}/kerby-all-${KERBY_VERSION}-source-release.zip /tmp
RUN unzip -q *.zip
WORKDIR /tmp/kerby-all-${KERBY_VERSION}/kerby-dist/kdc-dist
RUN mvn clean install -Pdist

WORKDIR ${KERBY_HOME}
RUN cp -r /tmp/kerby-all-${KERBY_VERSION}/kerby-dist/kdc-dist/* . \
  && rm -rf /tmp/kerby-all-${KERBY_VERSION} \
  && mkdir -p runtime ${KERBY_DATA}/keytabs ${KERBY_DATA}/conf \
  && cp conf/* ${KERBY_DATA}/conf \
  && chmod +x bin/kdcinit.sh bin/start-kdc.sh \
  && sed -i 's/\/tmp\/kerby\/jsonbackend/\/usr\/local\/kerby-data\/jsonbackend/' ${KERBY_DATA}/conf/backend.conf

RUN bin/kdcinit.sh ${KERBY_DATA}/conf ${KERBY_DATA}/keytabs

EXPOSE 88

ENTRYPOINT ["bin/start-kdc.sh"]
CMD ["/usr/local/kerby-data/conf", "runtime"]

