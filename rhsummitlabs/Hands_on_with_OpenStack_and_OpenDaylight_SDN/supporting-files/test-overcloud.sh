#!/usr/bin/env bash
# Quick overcloud test script
# Rhys Oxenham <roxenham@redhat.com>

source ~/overcloudrc
openstack token issue
openstack image create rhel7 --public --disk-format qcow2 --container-format bare --file ~/labs/rhel-server-7.4-x86_64-kvm.qcow2
openstack image list
openstack network create external --external --project admin --provider-network-type flat --provider-physical-network datacentre
openstack subnet create external_subnet --network external --subnet-range 192.168.122.0/24 --allocation-pool start=192.168.122.200,end=192.168.122.249 --no-dhcp --dns-nameserver 192.168.122.1 --gateway 192.168.122.1 --project admin
openstack network create internal
openstack subnet create internal_subnet --network internal --subnet-range 172.16.1.0/24 --dns-nameserver 192.168.122.1
openstack router create demo_router
neutron router-gateway-set demo_router external
openstack router add subnet demo_router internal_subnet
openstack flavor create --ram 2048 --disk 10 --vcpus 2 --id auto m1.labs
export MY_PROJECT=$(openstack project list | awk '$4 == "admin" {print $2};')
export SEC_GROUP_ID=$(neutron security-group-list | grep $MY_PROJECT | awk '{print $2;}')
openstack security group rule create --proto icmp $SEC_GROUP_ID
openstack security group rule create --proto tcp --dst-port 22:22 $SEC_GROUP_ID
openstack keypair create my_keypair --public-key ~/.ssh/id_rsa.pub
export NET_ID=$(openstack network list | awk '$4 == "internal" {print $2};')
openstack server create --flavor m1.labs --image rhel7 --key-name my_keypair --nic net-id=$NET_ID test_vm
openstack floating ip create external
echo -e "\n\nWaiting for machine to come up before assigning floating IP..."
sleep 180
export FLOATING_IP=$(openstack floating ip list | awk '$6 == "None" {print $4};')
openstack server add floating ip test_vm $FLOATING_IP
openstack server list


