#!/usr/bin/env bash

source ~/overcloudrc
openstack user create --password redhat rdo
openstack project create demo
openstack role add --user rdo --project demo admin
export USERNAME=rdo
export PASSWORD=redhat
cat > ~/demorc <<EOF
export OS_BAREMETAL_API_VERSION=1.34
export NOVA_VERSION=1.1
export OS_NO_CACHE=True
export COMPUTE_API_VERSION=1.1
export no_proxy=,192.168.122.100,172.16.0.25
export OS_VOLUME_API_VERSION=3
export OS_AUTH_URL=http://192.168.122.100:5000/v2.0
export IRONIC_API_VERSION=1.34
export OS_IMAGE_API_VERSION=2
export OS_AUTH_TYPE=password
export PYTHONWARNINGS="ignore:Certificate has no, ignore:A true SSLContext object is not available"
export PS1="(demo) $PS1"
export OS_USERNAME=$USERNAME
export OS_PROJECT_NAME=demo
export OS_PASSWORD=$PASSWORD
EOF
source ~/demorc
openstack token issue
openstack image create rhel7 --public --disk-format qcow2 --container-format bare --file ~/labs/rhel-server-7.4-x86_64-kvm.qcow2
openstack image list
openstack network create external --external --provider-physical-network datacentre --provider-network-type flat --project admin
openstack subnet create external_subnet --network external --subnet-range 192.168.122.0/24 --allocation-pool start=192.168.122.200,end=192.168.122.249 --no-dhcp --dns-nameserver 192.168.122.1 --gateway 192.168.122.1 --project admin
openstack network create internal
openstack subnet create internal_subnet --network internal --subnet-range 172.16.1.0/24 --dns-nameserver 192.168.122.1
openstack router create demo_router
neutron router-gateway-set demo_router external
openstack router add subnet demo_router internal_subnet
openstack flavor create --ram 2048 --disk 10 --vcpus 2 --id 6 m1.labs
export MY_PROJECT=$(openstack project list | awk '$4 == "demo" {print $2};')
export SEC_GROUP_ID=$(openstack security group list | grep $MY_PROJECT | awk '{print $2;}')
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

cat > ~/user-script.sh << EOF
#!/usr/bin/env bash
uname -a > /tmp/uname
date > /tmp/date
EOF
openstack volume create --size 5 my_volume
export MY_NAME=Rhys
cat > ~/summit.yaml << EOF
parameter_defaults:
  key_name: my_keypair
  my_name: $MY_NAME
EOF
openstack stack create --template ~/labs/osp/multiple-instances.yaml --environment ~/summit.yaml multiple
sleep 180
export MY_PROJECT=$(openstack project list | awk '$4 == "demo" {print $2};')
export SEC_GROUP_ID=$(openstack security group list | grep default | grep $MY_PROJECT | awk '{print $2;}')
openstack security group rule create --proto tcp --dst-port 80:80 $SEC_GROUP_ID
neutron lbaas-loadbalancer-create --name demo_lb internal_subnet
sleep 10
neutron lbaas-listener-create --name demo_lb_http --loadbalancer demo_lb --protocol HTTP --protocol-port 80
sleep 10
neutron lbaas-pool-create --name demo_lb_pool_http --lb-algorithm ROUND_ROBIN --listener demo_lb_http --protocol HTTP
sleep 10
neutron lbaas-healthmonitor-create --name demo_lb_http_monitor --delay 5 --max-retries 2 --timeout 5 --type HTTP --pool demo_lb_pool_http
sleep 10

export counter=1
for i in $(openstack server list | grep -v ID | awk '{print $8;}' | cut -d'=' -f2 | sed 's/.$//' | sed '/^\s*$/d'); do export NODE"$counter"_INTERNAL=$i; counter=$((counter+1)); done

neutron lbaas-member-create --name demo_lb_member1 --subnet internal_subnet --address $NODE1_INTERNAL --protocol-port 80 demo_lb_pool_http
sleep 5
neutron lbaas-member-create --name demo_lb_member2 --subnet internal_subnet --address $NODE2_INTERNAL --protocol-port 80 demo_lb_pool_http

export LBAAS_PORT=$(neutron lbaas-loadbalancer-show demo_lb 2>/dev/null | grep vip_port_id | awk '{print $4;}')
openstack floating ip create external --port $LBAAS_PORT
