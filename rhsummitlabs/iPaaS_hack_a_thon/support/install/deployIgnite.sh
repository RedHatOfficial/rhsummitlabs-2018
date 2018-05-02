#!/bin/bash

MASTER=https://master.hackathon.openshiftworkshop.com/
OCP_PWD=r3dh4t1!
ROUTE=apps.hackathon.openshiftworkshop.com


for userno in {1..35}
do

 echo Setup user$userno 

 oc login $MASTER -u user$userno -p $OCP_PWD 
 oc new-project user$userno
 oc create -f syndesis.yml
 oc create -f support/serviceaccount-as-oauthclient-restricted.yml


 oc new-app syndesis \
    -p ROUTE_HOSTNAME=user$userno-syndesis.$ROUTE \
    -p OPENSHIFT_MASTER=$(oc whoami --show-server) \
    -p OPENSHIFT_PROJECT=$(oc project -q) \
    -p OPENSHIFT_OAUTH_CLIENT_SECRET=$(oc sa get-token syndesis-oauth-client)


 oc create -f support/syndesis-amq.yml
 
 oc new-app amq63-basic \
    -p MQ_USERNAME=amq \
    -p MQ_PASSWORD=topSecret
    

done
exit 0
