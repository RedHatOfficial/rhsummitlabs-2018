#!/usr/bin/env bash

mkdir -p ~/templates
mkdir -p ~/images

cd ~/images
tar xvf /usr/share/rhosp-director-images/ironic-python-agent-latest-12.0.tar -C .
tar xvf /usr/share/rhosp-director-images/overcloud-full-latest-12.0.tar -C .
virt-customize -a ~/images/overcloud-full.qcow2 --root-password password:redhat

cp -rf ~/labs/director/complete-config/* ~/templates
source ~/stackrc
openstack overcloud image upload --update-existing

openstack baremetal instackenv validate -f ~/templates/instackenv.json
openstack overcloud node import ~/templates/instackenv.json

openstack overcloud node introspect --all-manageable --provide

cd ~
openstack overcloud deploy --templates -r ~/templates/custom_roles.yaml -e ~/templates/config.yaml -e ~/templates/network-config.yaml -e ~/templates/extra-config.yaml -e ~/templates/docker_registry.yaml -e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml
