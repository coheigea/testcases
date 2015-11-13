cxf-oauth1
===========

This is a demo that shows how OAuth 1 can be used to secure a CXF JAX-RS
service.

The service in question is a BalanceService, which keeps an internal map of 
usernames to balances. It provides two services, a GET to balance/{user} returns
the balance for that particular user, and a POST to balance/{user} with an
integer amount in the body creates the user + sets the balance.

The BalanceService is secured by two access points. A "customers" access point
is secured by HTTP/BA. A "partners" access point is secured by CXF's
OAuthRequestFilter.

In addition, there is an OAuth service that is used to issue tokens to customer
applications. The OAuth service uses a trivial "in-memory" OAuthDataProvider
implementation to manage tokens.

