#!/bin/bash
oc login -u developer
oc project demo
curl -L -v -k  https://secure-tmpsso-demo.paas.local/auth/realms/master/protocol/saml/descriptor -o /tmp/metadata.xml
cat /tmp/metadata.xml
oc create configmap metadata --from-file=/tmp/metadata.xml
oc create serviceaccount sa-saml
oc login -u system:admin
oc adm policy add-scc-to-user anyuid -z sa-saml
oc login -u developer
oc project demo
oc apply -f saml
