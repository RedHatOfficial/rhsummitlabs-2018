#!/usr/bin/env bash
source ~/stackrc
mkdir -p ~/images/
tar xvf /usr/share/rhosp-director-images/ironic-python-agent-latest-12.0.tar -C ~/images
tar xvf /usr/share/rhosp-director-images/overcloud-full-latest-12.0.tar -C ~/images/
virt-customize -a ~/images/overcloud-full.qcow2 --root-password password:redhat
openstack overcloud image upload --image-path ~/images/
openstack baremetal instackenv validate -f ~/labs/director/instackenv.json
openstack overcloud node import ~/labs/director/instackenv.json
openstack overcloud node introspect --all-manageable --provide
openstack baremetal node set --property capabilities='profile:control,boot_option:local' summit-controller1
openstack baremetal node set --property capabilities='profile:compute,boot_option:local' summit-compute1
openstack baremetal node set --property capabilities='profile:compute,boot_option:local' summit-compute2
openstack baremetal node set --property capabilities='profile:networker,boot_option:local' summit-networker1
mkdir -p ~/templates/
cp ~/labs/director/complete-config/custom_roles.yaml ~/templates/
cat > ~/templates/config.yaml <<EOF
parameter_defaults:
  HostnameMap:
    overcloud-controller-0: summit-controller
    overcloud-novacompute-0: summit-compute1
    overcloud-novacompute-1: summit-compute2
    overcloud-networker-0: summit-networker
EOF
cat >> ~/templates/config.yaml <<EOF
  NetworkerCount: 1
  ControllerCount: 1
  ComputeCount: 1
  OvercloudNetworkerFlavor: networker
  OvercloudComputeFlavor: compute
  OvercloudControllerFlavor: control
EOF
cat >> ~/templates/config.yaml << EOF
  NovaReservedHostMemory: 1024
EOF
cp ~/labs/director/templates/pre-deployment.yaml ~/templates/pre-deployment.yaml
cat > ~/templates/extra-config.yaml << EOF
resource_registry:
  OS::TripleO::NodeExtraConfig: /home/stack/templates/pre-deployment.yaml
EOF
cp -rf ~/labs/director/templates/nic-configs ~/templates/
cat > ~/templates/network-config.yaml << EOF
resource_registry:
  OS::TripleO::Compute::Net::SoftwareConfig: /home/stack/templates/nic-configs/compute.yaml
  OS::TripleO::Controller::Net::SoftwareConfig: /home/stack/templates/nic-configs/controller.yaml
  OS::TripleO::Networker::Net::SoftwareConfig: /home/stack/templates/nic-configs/networker.yaml
EOF
cat >> ~/templates/network-config.yaml << EOF
  OS::TripleO::Networker::Ports::ExternalPort: /usr/share/openstack-tripleo-heat-templates/network/ports/external.yaml
  OS::TripleO::Networker::Ports::InternalApiPort: /usr/share/openstack-tripleo-heat-templates/network/ports/internal_api.yaml
  OS::TripleO::Networker::Ports::TenantPort: /usr/share/openstack-tripleo-heat-templates/network/ports/tenant.yaml
EOF
cat >> ~/templates/network-config.yaml << EOF
parameter_defaults:
  NeutronExternalNetworkBridge: "''"
  NeutronNetworkType: 'vxlan,vlan'
  ControlPlaneSubnetCidr: "24"
  ControlPlaneDefaultRoute: 172.16.0.1
  ControlPlaneIP: 172.16.0.250
  EC2MetadataIp: 172.16.0.1
  DnsServers: ['192.168.122.1', '8.8.8.8']

  # Internal API used for internal OpenStack communication
  InternalApiNetCidr: 172.17.1.0/24
  InternalApiAllocationPools: [{'start': '172.17.1.10', 'end': '172.17.1.200'}]
  InternalApiNetworkVlanID: 101
  InternalApiNetworkVip: 172.17.1.150

  # Tenant Network Traffic - will be used for VXLAN over VLAN
  TenantNetCidr: 172.17.2.0/24
  TenantAllocationPools: [{'start': '172.17.2.10', 'end': '172.17.2.200'}]
  TenantNetworkVlanID: 201

  # Public Storage Access
  StorageNetCidr: 172.17.3.0/24
  StorageAllocationPools: [{'start': '172.17.3.10', 'end': '172.17.3.200'}]
  StorageNetworkVlanID: 301
  StorageNetworkVip: 172.17.3.150

  # Private Storage Access - e.g. storage replication
  StorageMgmtNetCidr: 172.17.4.0/24
  StorageMgmtAllocationPools: [{'start': '172.17.4.10', 'end': '172.17.4.200'}]
  StorageMgmtNetworkVlanID: 401
  StorageMgmtNetworkVip: 172.17.4.150

  # External Networking Access - Public API Access
  ExternalNetCidr: 192.168.122.0/24
  ExternalAllocationPools: [{'start': '192.168.122.102', 'end': '192.168.122.129'}]
  ExternalInterfaceDefaultRoute: 192.168.122.1
  ExternalNetworkVip: 192.168.122.100
EOF
echo "  PublicVirtualFixedIPs: [{'ip_address':'192.168.122.100'}]" >> ~/templates/network-config.yaml
echo "  NeutronTunnelTypes: 'vxlan'" >> ~/templates/network-config.yaml
cp ~/labs/director/templates/docker_registry.yaml ~/templates/
cd && source ~/stackrc
openstack overcloud deploy --templates \
    -r ~/templates/custom_roles.yaml \
    -e ~/templates/config.yaml \
    -e ~/templates/network-config.yaml \
    -e ~/templates/extra-config.yaml \
    -e ~/templates/docker_registry.yaml \
    -e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml
source ~/overcloudrc
openstack hypervisor list

