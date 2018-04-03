#!/bin/bash
oc login -u system:admin
oc project openshift
cat Dockerfile| oc new-build -D - --name=summitdemo-sso
oc login -u developer
oc new-project demo
oc project demo
oc create serviceaccount sso-service-account
oc policy add-role-to-user view system:serviceaccount:demo:sso-service-account
oc secret new sso-jgroup-secret /etc/certs/certs/jgroups.jceks
oc secret new sso-ssl-secret /etc/certs/certs/sso-https.jks /etc/certs/certs/truststore.jks
oc secrets link sso-service-account sso-jgroup-secret sso-ssl-secret
oc process openshift//sso71-https -p SSO_ADMIN_PASSWORD='RHSummit2018IAM!' -p APPLICATION_NAME="tmpsso" -p HTTPS_SECRET="sso-ssl-secret" -p HTTPS_KEYSTORE="sso-https.jks" -p HTTPS_KEYSTORE_TYPE="JKS" -p HTTPS_NAME="sso-https-key" -p HTTPS_PASSWORD="test1234" -p JGROUPS_ENCRYPT_SECRET="sso-jgroup-secret" -p JGROUPS_ENCRYPT_KEYSTORE="jgroups.jceks" -p JGROUPS_ENCRYPT_NAME="jgroups" -p JGROUPS_ENCRYPT_PASSWORD="test1234" -p SSO_TRUSTSTORE="truststore.jks" -p SSO_TRUSTSTORE_PASSWORD="test1234" -p SSO_TRUSTSTORE_SECRET="sso-ssl-secret" -p SERVICE_ACCOUNT_NAME="sso-service-account" -p SSO_ADMIN_USERNAME="admin" | oc create -n demo -f -
