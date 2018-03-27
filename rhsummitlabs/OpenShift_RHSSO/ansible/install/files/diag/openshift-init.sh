#!/bin/bash
set -x

check_and_do () {
eval "${1}"
if [ $? -ne 0 ]; then
  eval "${2}"
  if [ $? -ne 0 ]; then
   echo "${3}"
   exit 1
  fi
fi
}


check_and_do "oc status" "oc cluster up --host-data-dir=/opt/openshift_data/ --routing-suffix='paas.local' --version='v3.7.23-3' --public-hostname='openshift.local' && sleep 60 && oc login -u system:admin && sleep 10" "error bringing up openshift cluster"
check_and_do "oc describe -n openshift is/redhat-sso71-openshift" "oc create -n openshift -f /root/images/jboss-image-streams.json && sleep 10" "error loading image stream"
check_and_do "oc describe -n openshift template/sso71-https" "oc create -n openshift -f /root/images/sso71-https.json && sleep 10" "error loading template"
check_and_do "docker inspect registry.access.redhat.com/openshift3/ose-haproxy-router:v3.7.23-3" "docker pull registry.access.redhat.com/openshift3/ose-haproxy-router:v3.7.23-3" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/openshift3/ose-deployer:v3.7.23-3" "docker pull registry.access.redhat.com/openshift3/ose-deployer:v3.7.23-3" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/openshift3/ose-docker-registry:v3.7.23-3" "docker pull registry.access.redhat.com/openshift3/ose-docker-registry:v3.7.23-3" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/openshift3/ose:v3.7.23-3" "docker pull registry.access.redhat.com/openshift3/ose:v3.7.23-3" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/openshift3/ose-pod:v3.7.23-3" "docker pull registry.access.redhat.com/openshift3/ose-pod:v3.7.23-3" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/redhat-sso-7/sso71-openshift:1.0" "docker pull registry.access.redhat.com/redhat-sso-7/sso71-openshift:1.0" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/rhel:7.4-164" "docker pull registry.access.redhat.com/rhel:7.4-164" "error pulling docker image"
check_and_do "docker inspect registry.access.redhat.com/jboss-eap-7/eap71-openshift:1.2-7" "docker pull registry.access.redhat.com/jboss-eap-7/eap71-openshift:1.2-7" "error pulling docker image"
check_and_do "docker inspect summitdemo/saml:latest" "cd /root/pods/saml/ && docker build --rm --no-cache --force-rm -t summitdemo/saml:latest ." "error building docker image"
check_and_do "docker inspect summitdemo/oidc:latest" "cd /root/pods/oidc/ && docker build --rm --no-cache --force-rm -t summitdemo/oidc:latest ." "error building docker image"

touch /root/openshift.init
exit 0

