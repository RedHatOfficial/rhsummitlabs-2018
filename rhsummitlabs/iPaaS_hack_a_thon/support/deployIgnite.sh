#!/bin/bash

MASTER=https://master.3f1c.openshift.opentlc.com/
OCP_PWD=r3dh4t1!
ROUTE=apps.3f1c.openshift.opentlc.com


for userno in {1..3}
do

 echo Setup user$userno 

 oc login $MASTER -u user$userno -p $OCP_PWD 
 oc new-project user$userno
 oc create -f https://raw.githubusercontent.com/syndesisio/syndesis/master/install/syndesis.yml
 oc create -f https://raw.githubusercontent.com/syndesisio/syndesis/master/install/support/serviceaccount-as-oauthclient-restricted.yml


 oc new-app syndesis \
    -p ROUTE_HOSTNAME=user$userno-syndesis.$ROUTE \
    -p OPENSHIFT_MASTER=$(oc whoami --show-server) \
    -p OPENSHIFT_PROJECT=$(oc project -q) \
    -p OPENSHIFT_OAUTH_CLIENT_SECRET=$(oc sa get-token syndesis-oauth-client)


 oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/amq/amq63-basic.json
 
 oc new-app amq63-basic \
    -p MQ_USERNAME=amq \
    -p MQ_PASSWORD=topSecret
    

done
exit 0
