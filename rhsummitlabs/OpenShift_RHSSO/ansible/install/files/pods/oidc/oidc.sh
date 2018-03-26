#!/bin/bash
oc login -u developer
oc project demo
oc create serviceaccount sa-oidc
oc policy add-role-to-user view system:serviceaccount:demo:sa-oidc
oc login -u system:admin
oc adm policy add-scc-to-user anyuid -z sa-oidc
oc login -u developer
oc project demo
oc apply -f oidc
