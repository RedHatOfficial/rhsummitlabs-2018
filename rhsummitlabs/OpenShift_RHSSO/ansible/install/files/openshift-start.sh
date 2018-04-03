#!/bin/bash
oc cluster up --host-data-dir=/opt/openshift_data/ --host-config-dir=/var/lib/origin/openshift.local.config/ --host-pv-dir=/var/lib/origin/openshift.local.pv --host-volumes-dir=/var/lib/origin/openshift.local.volumes --use-existing-config=true --routing-suffix='paas.local' --version='v3.7.23-3' --public-hostname='openshift.local'
