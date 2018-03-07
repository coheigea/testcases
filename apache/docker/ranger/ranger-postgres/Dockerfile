
FROM postgres:10.3

LABEL maintainer-twitter="@coheigea"
LABEL version=0.7.1

COPY create_users.sql /docker-entrypoint-initdb.d/

EXPOSE 6080
