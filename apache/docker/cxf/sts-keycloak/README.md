# Project to deploy a CXF STS web application secured by Keycloak in docker

This project contains a web application which is an Apache CXF Security Token
Service (STS). Clients can authenticate to the STS by sending a WS-Security
UsernameToken, and receive a SAML token if authentication is successful.

The STS is configured to send the user credentials to Jboss Keycloak for
authentication. In addition, if the client requests the roles, these are 
retrieved from Keycloak and inserted into the SAML Assertion.

To build:

 * mvn clean install
 * docker build -t coheigea/cxf-sts-keycloak .

To run:

 * docker pull jboss/keycloak
 * docker-compose up

To test:
 * Log onto the Keycloak admin console via: http://localhost:9080/auth/ (admin:password)
 * Create a new role and a new user, assigning the role mapping to the new user.
 * Go to the "Credentials" tab for the user you have created, and specify a
   password, unselecting the "Temporary" checkbox, and reset the password.
 * Use SOAP-UI to create a new SOAP project using the WSDL: http://localhost:8080/cxf-sts-keycloak/UT?wsdl
 * Click on the "Issue" Binding and change the SOAP Body content of the request
   message to:

```xml
   <ns:RequestSecurityToken>
     <t:TokenType xmlns:t="http://docs.oasis-open.org/ws-sx/ws-trust/200512">http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</t:TokenType>
     <t:KeyType xmlns:t="http://docs.oasis-open.org/ws-sx/ws-trust/200512">http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer</t:KeyType>
     <t:RequestType xmlns:t="http://docs.oasis-open.org/ws-sx/ws-trust/200512">http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</t:RequestType>
     <t:Claims xmlns:ic="http://schemas.xmlsoap.org/ws/2005/05/identity" xmlns:t="http://docs.oasis-open.org/ws-sx/ws-trust/200512" Dialect="http://schemas.xmlsoap.org/ws/2005/05/identity">
        <ic:ClaimType xmlns:ic="http://schemas.xmlsoap.org/ws/2005/05/identity" Uri="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"/>
     </t:Claims>
   </ns:RequestSecurityToken>
```

 * Click on the Request Properties + add the values you configured in Keycloak
   for the Username + Password.
 * Then right click on the request message and add a WS-Security UsernameToken
   and send the request. If successful, you should see a SAML Assertion in
   the right-hand pane.


