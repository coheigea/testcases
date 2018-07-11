# Dockerfile to deploy a WS-Federation secured 'fedizhelloworld' application

This directory contains a Docker File to deploy a 'fedizhelloworld'
application to Apache Tomcat, where Tomcat is configured with the Apache CXF
Fediz RP plugin for WS-Federation. The plugin is configured to redirect the
user to the Fediz IdP for authentication, before redirecting back to the plugin.

 * Build with: docker build -t coheigea/fediz-helloworld .
 * Run with: docker run -p 8443:8443 coheigea/fediz-helloworld
 * Open in a browser: https://localhost:8443/fedizhelloworld/secure/fedservlet
   (Credentials: alice/ecila)


