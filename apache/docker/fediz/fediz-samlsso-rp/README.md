# Dockerfile to deploy a SAML-SSO secured 'fedizhelloworld' application

This directory contains a Docker File to deploy a 'fedizhelloworld'
application to Apache Tomcat, where Tomcat is configured with the Apache CXF
Fediz SAML SSO RP plugin. The plugin is configured to redirect the user to
Keycloak for authentication, before redirecting back to the plugin.

 * Build with: docker build -t coheigea/fediz-samlsso-rp .
 * Run with: docker run -p 9443:8443 coheigea/fediz-samlsso-rp
 * See here for instructions as to how to set up Keycloak: http://coheigea.blogspot.ie/2018/05/saml-sso-support-for-apache-cxf-fediz.html


