# OpenShift + single sign-on = Happy security teams and happy users

## Some Housekeeping

 - LAB Number:  L1019
 - Password: RHSummit2018IAM!
 - Docs: [https://bit.ly/2Gv8D36](https://bit.ly/2Gv8D36)
 - Slides: [https://bit.ly/2ISTZAX](https://bit.ly/2ISTZAX)
 - Code: [https://bit.ly/2pCZpYP](https://bit.ly/2pCZpYP)
 - Homepage on laptop has useful info


## Intro

Hi!

We are some of the Identity and Access Management IT team inside of Red Hat.  Today we will be demonstrating how [Red Hat Single Sign-On](https://access.redhat.com/products/red-hat-single-sign-on) can be used to give your users a single set of credentials that gets them into several different sites seamlessly.  This is both a security and usability win.  

The two most common interoperability protocols for Single Sign-On are [SAML](https://en.wikipedia.org/wiki/SAML_2.0) and [OIDC](http://openid.net/connect/).  Part of this lab will entail us giving an overview of these protocols and discussing the best use cases for each of them.

As an added bonus we will be running Red Hat Single Sign-On server and our test applications on top of [OpenShift](https://www.openshift.com/).  As we touch OpenShift in this lab, we will be briefly describing terms and explaining what is happening on its backend so that you can leave with a basic familiarity of that technology as well.  

Finally, if time permits, we will be standing up an [IDM](https://access.redhat.com/products/identity-management) server and configuring Red Hat Single Sign-On server to authenticate and look up information from a centralized directory.  



## Some Demo Caveats

 - Passwords and SSH keys are shared.  
 - SSH password authentication is enabled.
 - Self-signed TLS certs and keys are used and shared.
 - Lots of ports are open.
 - A GUI and tons of packages are installed.
 - We are running OpenShift in a local prototying mode and without any authentication.
 - RHSSO is using and in memory database
 - Docker containers and some packages are pinned to specific versions for guaranteed predictability.  
 - While Red Hat SSO uses open standard APIs and protocols making it compatible with many other products - Red Hat's initial focus is on integrating it's own products and properties and that is what we continuously test against and fully support.

In large lab environments like this one, you have to balance security and supportability against usability and time constraints.  What is demonstrated here should be viewed as a learning experience of how things work and not as a set of instructions to get something running up in production in your environment.  


## OpenShift

[OpenShift](https://www.openshift.com/) is Red Hat's Platform As A Service offering. It is based off of [Docker](https://www.docker.com/) and [Kubernetes](https://kubernetes.io/).  Not only do we package up and support specific versions of these components we add onto them by creating new features.  Some of our most prevalent additional features are: out of the box CI/CD/Devops constructs, reverse proxying, security and quotas and the ability for developers to rapidly deploy microsevices using  source-to-image strategies.

Put simply, OpenShift allows for the following -- when a developer commits a code change a new immutable container (think VM that is only running the application in question and not the OS) will be built, tested, and then rolled out into a live environment in a manner that is non-disruptive to the applications users.  All of this without any networking folks, operational folks, hardware folks or anybody else involved.  Oh, and did I mention that if the container doesn't pass its checks the previous version will automatically be rolled back to?  Pretty sweet from an "idea to go live" perspective.

Day to day care and feeding is also awesome.  Should your application crash or an entire OpenShift hosting node die your application will come back on its own with no interaction needed.  Since things are so portable and immutable it also means that there are no more discussions of "this worked on my laptop, why won't it work on the server?".  

Lets dig in:

    chmod 600 /home/lab-user/.ssh/id_rsa
    ssh rhsso-GUID.rhpds.opentlc.com
    sudo su -
    /root/openshift-start.sh

This script will bring up OpenShift in a prototyping like environment by running Docker containers of all of its components locally on your machine.  Your developers can do the same thing on their laptops and use an OpenShift setup that should behave similar to what you offer them on much beefier high-availability and supported OpenShift installs you have in production.  It is truly an amazing feat that something so complex can be ran locally with so little effort.  

Lets explore a bit:

    docker ps
    oc login -u system:admin
    oc get nodes
    oc get pods --all-namespaces
    oc get is --all-namespaces
    oc get templates --all-namespaces
    oc get cm --all-namespaces
    oc get secret --all-namespaces
    oc get dc --all-namespaces
    oc get services --all-namespaces
    oc get route --all-namespaces
    oc get pv
    oc get all
    oc status
    oc describe dc/docker-registry -n default
    oc get dc/docker-registry -o yaml -n default
    oc rsh pod/pod-name
    oc logs -F pod/pod-name
    oc get events



`docker ps` will show us that all of the OpenShift services are running as docker pods on this machine.  

` oc login -u system:admin`  logs us in as the default root like account inside of OpenShift.  Once you login OpenShift will list all the projects (collection of resources that can have access and quota controls applied to them) that you have access to.  

`oc get nodes`  will show all the nodes in the cluster.  In this case there will be only one.

`oc get pods --all-namespaces`  will show all pods (a tightly coupled set of containers) running in all projects, which is denoted by the --all-namespaces. You should see a registry and a router pod.  The registry pod can host docker images.  You can use it just for OpenShift internal stuff like images you build there or you can expose it and use it for general purpose stuff as well.  Pods inside of OpenShift run on private IP addresses. To get access to them from the outside world the router pod exposes a public (or at least more public) IP address for them.  The router also load balances against as many copies of the pod you have running on the OpenShift cluster.  

`oc get is --all-namespaces`  will show what docker images have had their configs preseeded for ease of use.  You should see a redhat-sso71-openshift  in the list.

`oc get templates --all-namespaces`  templates are basically freeze dried full application stacks inside of OpenShift.  You fill in a few variables, supply a bit of data and you get pods running your application, networking configured and everything else you need to have a working application stack.  You should see sso71-https  in this list.  

`oc get cm --all-namespaces`  and `oc get secret --all-namespaces` configmaps and secrets are raw key value stores or files that can be exposed to pods as mount points or environment variables.  Secrets are base64 encoded.  This stuff is needed because pods should be immutable and reusable.  You wouldn't want to bake a dev and prod database connection password into the same container image and you wouldn't want to build two as the environment can change over time.  These constructs allow for dynamic runtime data to land in a pod a launch.

`oc get dc --all-namespaces`  Deploymentconfigs define what your pods look like and tell replication controllers how many copies of the pod should be running at any given time.  You should see a docker-registry and router in your list.  

`oc get services --all-namespaces` and `oc get route --all-namespaces` will show you internal cluster load balancers for your pods and information about services that are exposed "to the world".  You should see some services but no routes yet.

`oc get pv` will show you the persistent volumes available for pod usage.  Pods are ephemeral by default; which is fine for some services.  But you definitely don't want your database loosing all of its data when it restarts and that is what a pv is for.  It is a writable and persistent mount that follows your pod around for its whole life.

`oc get all` and `oc status` are shorthands to give all common objects and their status in your current project.  Try adding a `-n default` to see some data.

`oc describe dc/docker-registry -n default`  and `oc get dc/docker-registry -o yaml -n default` substitute your object of choice, will give you more detailed information about the object.  

`oc rsh pod/pod-name` will get you a console into a running pod.  Very useful for debugging.  

`oc logs -F pod/pod-name` will show you the streaming logs of the running pod or other object.  Very useful for debugging.   

`oc get events`  will get you low level cluster debugging info. Hopefully there isn't anything here right now.  

This is barely scratching the surface of what OpenShift can do but it should be enough to get you through this lab.  

Before we jump onto the next part though, lets take a quick peak at the OpenShift web GUI.  

 1. ssh rhsso-GUID.rhpds.opentlc.com
 2. sudo su -
 3. /root/start-vnc.sh
 4. https://rhsso-GUID.rhpds.opentlc.com:9000
 5. Accept any cert warnings
 6. Click computer screen top right
 7. Enter password and click Connect
 8. You now see the VM in your browser
 9. Go through the new user screens
 10. Open firefox in the VM
 11. Edit > Preferences > Advanced > Certificates > View Certificates > Authorities > Import > /etc/certs/myca.crt
 12. Click all the boxes in the trust pop-up. And click ok.  
 13. https://openshift.local:8443  in the now nested browser
 14. Login as developer/developer
 15. Click around and explore a bit


Some of the other stuff you see in the GUI like `builds`, `pipelines`, `images`, `quotas`, etc do what you would expect.  We won't be exploring any of these during this lab though.  



## Red Hat Single Sign-On Server

[Red Hat Single Sign-On Server](https://access.redhat.com/products/red-hat-single-sign-on) is a standards compliant full featured and extendable SSO product based on Red Hat's JBoss middleware stack.  It supports SAML, OIDC and OAuth protocols. It can federate information with LDAP and kerberos user stores and it supports brokered identity flows for things like social auth.  It comes with an easy to use GUI and an API for automation.  The RHSSO team also provides adapters to make integrating with other internally written applications easier.  

In short, if you already have all your employee information in a central place like LDAP or AD  then you can have RHSSO use that data and act as the SSO bridge that will allow your employees to login to countless applications and vendors using a single set of credentials.  

Lets bring it up using an OpenShift template:

    ## as root on rhsso box
    cd /root/pods/sso/
    oc login -u system:admin
    oc project openshift
    cat Dockerfile| oc new-build -D - --name=summitdemo-sso
    oc login -u developer https://openshift.local:8443
    oc new-project demo
    oc project demo
    oc create serviceaccount sso-service-account
    oc policy add-role-to-user view system:serviceaccount:demo:sso-service-account
    oc secret new sso-jgroup-secret /etc/certs/certs/jgroups.jceks
    oc secret new sso-ssl-secret /etc/certs/certs/sso-https.jks /etc/certs/certs/truststore.jks
    oc secrets link sso-service-account sso-jgroup-secret sso-ssl-secret
    oc process openshift//sso71-https -p SSO_ADMIN_PASSWORD='RHSummit2018IAM!' -p APPLICATION_NAME="tmpsso" -p HTTPS_SECRET="sso-ssl-secret" -p HTTPS_KEYSTORE="sso-https.jks" -p HTTPS_KEYSTORE_TYPE="JKS" -p HTTPS_NAME="sso-https-key" -p HTTPS_PASSWORD="test1234" -p JGROUPS_ENCRYPT_SECRET="sso-jgroup-secret" -p JGROUPS_ENCRYPT_KEYSTORE="jgroups.jceks" -p JGROUPS_ENCRYPT_NAME="jgroups" -p JGROUPS_ENCRYPT_PASSWORD="test1234" -p SSO_TRUSTSTORE="truststore.jks" -p SSO_TRUSTSTORE_PASSWORD="test1234" -p SSO_TRUSTSTORE_SECRET="sso-ssl-secret" -p SERVICE_ACCOUNT_NAME="sso-service-account" -p SSO_ADMIN_USERNAME="admin" | oc create -n demo -f -
    oc get pods -w

Above we:
 - Logged in as the admin and built a custom container image to work around a small bug with RHSSO and certain docker storage types.
 - Logged in as a standard user.  
 - Created a new project and switched to it  
 - Created a service account and gave it special OpenShift permissions it needs for clustering
 - Created some secrets  
 - Filled in a template with some parameters and instantiated the results of that filled in template.
 - Watched and waited for it to come up.



Lets explore a bit

 1.  In firefox in your VM go to https://secure-tmpsso-demo.paas.local/auth/admin/
 2.  Login with admin and the shared password
 3.  Click around

`Realms` are logical collections of items and can have separate configuration options and access permissions.

`Clients` are the properties you have SSO integrated with. Each client can have their own SSO related settings and their own statically defined `Roles` and their own dynamic and statically assigned `Mappers`.

`Roles` are what you would imagine.  You can think of them as groups a person is in or properties a person has that you can pass to an SSO integrated property so they can make their own authorization calls based on that.

`Mappers` translate data from a `User Federation` source into something accessible and usable by RHSSO.  It could for instance decide to read and store my *sn* from LDAP as *Last Name* in RHSSO.  

`User Federations` are the backends RHSSO hooks into to get user information and to do authentication against.  

`Client templates` Are collections of `Mappers`.  So maybe you have a template that allows internal applications can get all information about your employees and then another template that allows external vendors to only see name and email address.  

`Identity Providers` are for brokering auth and linking identities from different central sources of authority.  Social and other complex authentication flows fall under this.  Basically your internal RHSSO needs to create a link between X github user and Y internal user and similar.

`Authentication` Allows you do to fine grained PAM like module stacking and directing of actions.  You can for instance say try to authenticate somebody with a GSSAPI kerberos ticket and if that fails prompt them for a username and password.

The rest of the things do what you would imagine they would.  RHSSO is very extendable and in one of our environments we have defined our own `User Federation` SPIs to talk to a custom database and have defined custom `Mappers` to let us use data more flexibly.  

Before we setup and SSO enable some clients, lets add a user:
 1.  In firefox in your VM go to https://secure-tmpsso-demo.paas.local/auth/admin/
 2.  Login with admin and the shared password
 3. Users > Add User
     1. Username: testlocal
     2. email: testlocal@example.com
     3. First Name: Test
     4. Last Name: Local
     5. Enabled
     6. Email verified off
     7. No actions
     8. Save  
 4. Users > View all users > testlocal > edit
     1. Credentials
     2. New Password: test1234
     3. Password Confirmation: test1234
     4. Temporary off
     5. Reset Password
     6. Change  Password
 5. Roles > Add Role
     1. Role name: authenticated
     2. Description: empty
     3. Scope Param required: off
     4. Save
 6. Users > View all users > testlocal > edit
     1. Role Mappings
     2. Add authenticated to assigned roles



## SAML

 [Security Assertion Markup Language ](https://en.wikipedia.org/wiki/SAML_2.0) is a well established XML based SSO protocol.  It is almost exclusively used in web based SSO flows where a full featured browser is available.  This is because parts of its communications happen over self-posting javascript rendered forms.  In SAML it is rare for the client integration (Service Provider) and the SSO server (Identity Provider) to talk to each other directly.  SAML does not offer any form of delegated authentication and the client integration is expected to fully track the user session and permissions itself.  

There are two main SAML web flows.  SP-initiated and IDP-initiated.  SP-initiated is generally the preferred flow because it allows for deep linking and end user bookmarking, whereas IDP-initiated forces your users to always click on buttons in a company portal or similar.  

`Service Provider / SP`  - The application or vendor that is integrating with your RHSSO server.

`Identity Provider / IDP` - Your RHSSO server.

`Issuer / EntityID`  The unique URI that identifies the IDP or SP.  Whitelisting can take place on this.

`Metadata` XML definition documents that you, the runner of the SSO server and the client running the application that wants integration swap with each other.  They include endpoint information on where data should flow.

`SingleSignOnService` A key in the IDP metadata that tells the client application where to send `AuthnStatement` login requests.  

`AssertionConsumerService` A key in the SP metadata that tells the IDP where to send login response to.  

`AuthnStatement` The first step in an SP-initiated SAML flow.  An XML document the SP sends to the IDP telling the IDP that it wants the end user to login.

`Assertion` The second step in an SP-initiated SAML flow.  An XML document from the IDP is sent to the SP telling them that somebody logged in and including information about that person in the assertions NameID and Attribute fields.  

`RelayState` Where to send somebody after a successful login.

`NameID` and `Attributes` and `Roles`  are information about an end user that logged in that are delivered to the SP via an Assertion.  This would be something like your First Name.

![saml flow](https://upload.wikimedia.org/wikipedia/en/0/04/Saml2-browser-sso-redirect-post.png)

 source: [wikipedia](https://www.wikipedia.org/)

 1. End user attempts to access protected content from a vendor
     1. End user does not have an existing session with the vendor
     2. End user did not come over with a SAML Assertion POST to an AssertionConsumerService URL
 2. End user is directed to the RHSSO IDP SingleSignOnService endpoint with a AuthnStatement. This is usually base64 encoded and stuck onto a URL as a URI parameter but it could also be POSTed by a javascript form.
     1. RHSSO makes sure its a valid vendor and checks signatures
     2. End user does not have an existing RHSSO session
     3. End user logs in
 3. End user is sent to the vendors AssertionConsumerService endpoint with an Assertion that contains several pieces of personal data in its Attributes.  This is sent by javascript form POSTing of base64 data.  
 4. Vendor establishes a session for the end user and sends them back to the RelayState where they started


Lets build it:

    ## as root on rhsso box
    cd /root/pods/saml/
    oc login -u developer https://openshift.local:8443
    oc project demo
    curl -L -v -k  https://secure-tmpsso-demo.paas.local/auth/realms/master/protocol/saml/descriptor -o /tmp/metadata.xml
    cat /tmp/metadata.xml
    oc create configmap metadata --from-file=/tmp/metadata.xml
    oc create serviceaccount sa-saml
    oc login -u system:admin
    oc adm policy add-scc-to-user anyuid -z sa-saml
    oc login -u developer https://openshift.local:8443
    oc project demo
    oc apply -f saml
    oc get pods -w


Above we:
 - Logged in as a standard user and chose our project.
 - Downloaded the IDP metadata, made sure it looked right and turned it into an OpenShift ConfigMap
 - Created a service account that will run the SAML pod
 - Logged in as admin and gave that service account special abilities that will allow it to run as a non-generated UIDs.  I want to run apache as the apache user for demo simplicity.
 - Logged in as a standard user and applied a preconfigured  template that stood up the pod, service and route
 - Watched for the pod to come up.

Lets SSO enable it:
 1.  In firefox in your VM go to https://secure-tmpsso-demo.paas.local/auth/admin/
 2.  Login with admin and the shared password
 3. Clients > Create
     1. ClientID: https://saml-demo.paas.local/secret/endpoint/metadata
     2. Client Protocol: SAML
     3. Client Template: empty
     4. Client SAML Endpoint: https://saml-demo.paas.local/secret/endpoint/postResponse
     5. Save
 4. The general settings screen loads
     1. Client Signature Required: Off
     2. Force Name ID Format: On
     3. Name ID Format: username
     4. Valid Redirect URIs: https://saml-demo.paas.local/*  
     5. Leave everything else the same
     6. Save

Let try out the SSO:
 1. Visit the [SSO Tracer](https://addons.mozilla.org/en-US/firefox/addon/sso-tracer/) Firefox extension page
 2. Add to Firefox > Install > Restart Now
 3. File > New Private Window
 4. Tools > SSO Tracer
 5. Go to https://saml-demo.paas.local/  You will see a splash screen.
 6. Go to https://saml-demo.paas.local/secret/ while watching SSO Tracer
     1. You will be bounced to https://secure-tmpsso-demo.paas.local
     2. login as testlocal/test1234
     3. Watch SSO Tracer as you go back to https://saml-demo.paas.local/secret/
 7. You will see a php page that shows a bunch of stuff including some environmental variables like "MELLON_Role" that were set based on your users information.
 8. Click on the "SAML" flagged bits in SSO Tracer to see what this SP-initiated  SSO flow actually looked like on the wire.

## OIDC

[OpenID Connect](http://openid.net/connect/) is a newer but still well established SSO protocol.  It is an extension of the Oauth2 protocol and adds the ability to handle identity information to the existing spec.  It sends data in JSON JWTs.  It can be used for web, mobile and native application SSO flows.  It offers delegated authentication abilities.  Basic session tracking and authentication information can be sussed out by a backend server based on the tokens returned from RHSSO, without that backend server having to be directly integrated with RHSSO.  Frontend applications do however still need to manage their own sessions for end users. Unlike in SAML, it is common for OIDC clients to talk directly to OpenID Providers.  

There are three main OIDC flows.  

The code flow offers the most security and is the most versatile as it can work with frontend + backend combos (even when those backends are also JS based client applications) .  The code (that doesn't decode into any personal information) is transferred as a URL parameter in the users browser.  The browser then gives that code to the frontend client application which then gives it to the backend server.  The backend server then takes that code and can make an authenticated backchannel call to the OpenID Provider to change that into several tokens that contain user identifiable information.  At this point the backend server will get back a refersh token, an access token and an id token. The access token can further be used to access other backend resource servers when doing delegated auth flows.  

The implicit flow is generally used for more of your one-and-done style clients.  The client redirects the end user for authentication to the OpenID provider and they come back with an id_token and access token that can be immediately used.  Since this information is transferred in the browser it is less safe than the code flow.  Also because of that no refresh tokens are given.

Finally, the Resource Owner Grant flow is where you enter your credentials into a client.  They then 'replay' this information to the OpenID provider and get back an access token, an id_token and an refresh token.  This is useful for mobile and native applications where browser redirects aren't easily possible.  

`Client / Relying Party` - The application or vendor requesting authentication from your RHSSO server.

`OpenID Provider / Authentication server / Authorization Server / Identity Provider` - Your RHSSO Server

`Resource Server / Application` - The backend that is hosting the protected resource.  The client can send an access token via Bearer Header auth to this backend and it can determine who auth is being delegated for, what they should have access to and then return the requested content.

`Claim / Atrribute` - A piece of data that the OpenID provider provides to the client about an end user.  First name for example.  

`client_id`  - The 'username' of your client as seen by the OpenID provider.  Needed for validation and in confidential flows a password will be required as well.

`redirect_uri` - Where to send the end user after a successful auth request.  Whitelists can be formed on this in RHSSO.

`Authorization Code` - A non-human readable unique short lived and one time use string that the OpenID Provider gives the Client in code flows via URI parameters. This is a temporary credential that can be exchanged for tokens that provide access or information about an end user.

`Access token` - A token used by a client to authenticate on behalf of an End User to a resource server to get protected content.

`Refresh token` - A token used by a client to get new access tokens as they expire.

`ID token` - A token sent by the OpenID Provider to the Client that contains claims about who the person logging in is.

`well-known configuration / metdata`  - A collection of endpoints and other data published by the OpenID Provider that the clients will use to interact with it.  


![OpenID Code Flow](https://backstage.forgerock.com/docs/am/5/oidc1-guide/images/thumb_openid-connect-basic.png)

source: [forgerock](https://backstage.forgerock.com/docs/am/5/oidc1-guide/)

 1. End user accesses a client
     1. Has no current session
     2. Didn't come over with an OIDC code as a URI parameter
 2. Client redirects end user to the OpenID Providers authorization_endpoint from its well-known configuration
     1. End user doesn't have a session
 3. End user authenticates and optionally consents to sharing data with the client
 4. End user is redirected back to the clients redirect_uri with a code URL  parameter.  This code is non-human readable and is an Authorization Code.  
 5. The client front end hands the code snippet from the URL to the client backend.
     1. The client front end should establish a local session for the end user at this point.
 6. The client backend POSTS the code to the OpenID Provider token endpoint with its client_id and password (if required).  
 7. The OpenID Provider returns an access_token, id_token, and refresh_token in a JSON document.  Each are base64 encoded and signed JWTs.
     1. The client backend is now responsible for keeping access tokens fresh by resubmitting refresh tokens so long as the clients front end session is still valid.  
 8. The end user interacts with the client front end and calls an action that requires the client backend to use an access token to access a protected resource on an entirely different Resource Server.
 9. The resource server validates the identity and session of the access token in or out of band.  It isn't necessarily required for this server to know anything about the OpenID provider other than its signing key.
 10. The resource server grants access to the data and returns it to the client backend which is then shown to the end user through the client frontend.  

Lets build it:

    ## as root on rhsso box
    cd /root/pods/oidc/
    oc login -u developer https://openshift.local:8443
    oc project demo
    oc create serviceaccount sa-oidc
    oc policy add-role-to-user view system:serviceaccount:demo:sa-oidc
    oc login -u system:admin
    oc adm policy add-scc-to-user anyuid -z sa-oidc
    oc login -u developer https://openshift.local:8443
    oc project demo
    oc apply -f oidc
    oc get pods -w


Above we:
 - Logged in as a standard user and selected our project
 - Created a service account and gave it view permissions for clustering and special privileges to run as the jboss user instead of a random high UID for ease of demo use.
 - Applied a pre-built template.
 - Watched for the pod to come up.

Lets SSO enable it:
 1.  In firefox in your VM go to https://secure-tmpsso-demo.paas.local/auth/admin/
 2.  Login with admin and the shared password
 3. Clients > Create
     1. ClientID: oidc-test
     2. Client Protocol: openid-connect
     3. Client Template: empty
     4. Root URL: empty
     5. Save
 4. The general settings screen loads
     1. Direct Access Grants Enabled: Off
     2. Valid Redirect URIs: https://oidc-demo.paas.local/*  
     3. Web Origins: +
     4. Leave everything else the same
     5. Save



Let try out the SSO:
 1. In firefox in your VM open up a private session (File > New Private Window) and launch Developer Tools (Tools > Web Developer > Toggle Tools > Network)
 2. Go to https://oidc-demo.paas.local/oidc-app/ while watching Network Developer Tools. Click login.
     1. You will be bounced to https://secure-tmpsso-demo.paas.local
     2. login as testlocal/test1234
     3. Watch Developer Tools as you go back to https://oidc-demo.paas.local/oidc-app/authenticated?...
 4. You will see a page showing information about the user that logged in.  
 5. Click on the lines that have "code" entries in Developer Tools.  Note how this isn't a useful piece of information.  The EAP backend itself then exchanges that code for a token that has more of your personal data.  You can't see that in the browsers Developer Tools and that is a good thing for security. Your test users information is printed on this page because our front-end code then queries the backend EAP and fishes out the data to display it.  

#### Debugging OIDC (*Optional*)
If you want to take a more hands-on approach to inspecting token issuance, the following bash command can be used to view the access token.  Note that this particular command makes use of a direct access grant, which is highly discouraged for browser-based flows in production.  However, it can be highly useful for debugging and testing purposes in lower environments.

```
curl -XPOST --data="username=testlocal&password=test1234&grant_type=password&client_id=oidc-test" https://secure-tmpsso-demo.paas.local/auth/realms/master/protocol/openid-connect/token
```

And if you're interested in the contents of the access token:

```
curl -XPOST --data="username=testlocal&password=test1234&grant_type=password&client_id=oidc-test" https://secure-tmpsso-demo.paas.local/auth/realms/master/protocol/openid-connect/token | grep -Po '"access_token":.*?[^\\]",' | cut -d \" -f 4 | cut -d . -f 2 | base64 --decode 2> /dev/null
```


## IDM

[Identity Management](https://access.redhat.com/products/identity-management) is a Red Hat product that provides a way to store and access centralized identity and policy information.  It stores user and group data in LDAP, authentication credentials and Kerberos, does TLS certificate issuing and revoking, hosts DNS, stores secret data and propagates sudo, selinux and HBAC controls.   It does this by tightly coupling Dogtag, 389-DS, MIT Kerberos and BIND.  All of its data is stored in highly available multi-master LDAP backends and it supports geo views for greater redundancy and resiliency.  It has an amazingly easy to use CLI, API and Web UI.  It is also able to integrate with AD setups in different scenarios.  

In short, you can think of IDM as "AD for Linux".  


Lets explore a bit and create a user.  On your SSO host
 1. ssh rhsso-GUID.rhpds.opentlc.com
 2. sudo su -
 3. vim /etc/hosts
 4. 10.0.0.11 idm.local
 5. In firefox in your VM go to https://idm.local
 6. Login with the shared admin credentials
 7. Click around and see the below while we explain it in class
 8. Now lets create a user
     1. Identity > Users > Add
     2. User login: testldap
     3. First Name: Test
     4. Last Name:  LDAP
     5. Class: empty
     6. No private group: unchecked
     7. GID: empty
     8. New Password: password
     9. Verify Password: password
     10. Add
 9. Logout (drop down at top right)
 10. Login with testldap/password
     1. Current password: password
     2. OTP: empty
     3. New Password: RHSummit2018IAM!
     4. Verify Password: RHSummit2018IAM!
     5. Reset password and login


`Users` and `Groups` are what you would expect.  IDM does support groups of groups and it supports life-cycle stages of user accounts.  

`Hosts` are for keytabs and also store SSH keys so clients can SSH in and not have to 'yes' trust a host.  

`Automember` allows you to dynamically create groups based attributes in user objects.  

`ID views` is an advanced feature where you can choose do override returned results for specific circumstances.

`Policy` tab allows for greater security in your environment by centrally managing HBAC, sudo rules, selinux policies, and password policies.  

`Authentication` tab is mainly for the TLS cert component.  You can easily issue and revoke certificates.  It can also hook into backend RADIUS severs and offers basic OTP functionality.

`Network Services` is where you manage global automount entries and also DNS zones and records.  It also has vault functionality where shared secret data can be easily stored and retrieved.

Finally, the `IPA Server` tab is where you can do more of the fine grained configuration of the product itself, like establish replication agreements and configure realms and topologies.  


Now lets configure RHSSO to read users from IDM:
 1.  In firefox in your VM go to https://secure-tmpsso-demo.paas.local/auth/admin/
 2.  Login with admin and the shared password
 3. User Federation
     1. Add LDAP
     2. Console Display Name: ldap
     3. Priority: 0
     4. Edit mode: READ_ONLY
     5. Sync Registrations: off
     6. Vendor: Red Hat Directory Server
     7. Username LDAP Attribute: uid
     8. RDN LDAP Attribute: uid
     9. UUID LDAP Attribute: uid
     10. User Object Classes: inetOrgPerson, organizationalPerson
     11. connection url: ldaps://10.0.0.11
     12. users dn: cn=users,cn=accounts,dc=idm,dc=local
     13. Authentication Type: simple
     14. Bind DN: uid=testldap,cn=users,cn=accounts,dc=idm,dc=local
     15. Bind Credential: RHSummit2018IAM!
     16. Test Connection, Test Authentication
     17. Cusotm User LDAP Filter: empty
     18. Search scope: One Level
     19. Use truststore SPI: Only for ldaps
     20. Connection Pooling: on
     21. Connection timeout: empty
     22. Read timeout: emtpy
     23. Pagination: on
     24. Allow Kerberos: off
     25. User Kerberos for password authentication: off
     26. Batch Size: 1000
     27. Periodic full sync: off
     28. Periodic changed users sync: off
     29. Cache Policy: default
     30. Save
 4. File > new private window go to https://saml-demo.paas.local/secret/
     1. You will be redirected back to RHSSO
     2. Login with testldap/RHSummit2018IAM!
     3. You will go back to https://saml-demo.paas.local/secret/ and see a PHP page.
 5. You just logged in using an account from an centralized identity store!


## Some Final Housekeeping

 - Reset your station from the laptops homepage
 - Please give us feedback
 - Questions?
 - Thanks!  Enjoy the rest of Summit!  
