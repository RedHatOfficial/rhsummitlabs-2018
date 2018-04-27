#**Lab 7: Network Setup**

In the previous section we defined our overcloud configuration and ensured that it was configured to support a dedicated networker node via the composable roles extension implemented in OSP director. We also set some specific parameters to demonstrate how we can allow for customisations in the overcloud deployment via a Heat **environment** file. In this next stage we're going to be showing how that same environment file can be used to advise OSP director of the networking configuration of the nodes that are to be deployed, including (but not limited to):

* Physical interface configurations
* Virtual bridge setup (e.g. with Open vSwitch)
* Network traffic types and associated subnets
* VLAN-based isolation
* Neutron specific bridge settings

We're going to be looking at our current networking configuration for our overcloud nodes, showing the available out of the box options, and demonstrating the example/pre-prepared templates that we'll use for deployment in the next lab.

# Physical Topology

Each of our overcloud nodes has been configured with **two** "physical" (only quoted because they're virtualised baremetal machines) network interfaces:

* The first network interface (**eth0**) is attached to a dedicated, isolated network that is used for bare metal provisioning, known as the **control plane** (ctlplane), i.e. this is the network that is used by Ironic to deploy the nodes via DHCP/PXE, and is used by the nodes to communicate back to the undercloud during initial deployment and any ongoing maintenance tasks. OSP director is assumed to have full control over this network.

* The second network interface (**eth1**) is attached to a network provided by your workstation (in our case the libvirt default network, but in the real world this would likely be the corporate network) in which we can both gain routable access to our overcloud nodes when they're provisioned and also run a number of different VLAN's on-top of for other OpenStack network traffic types, e.g. internal API communication, or storage access.

Or visually represented:

<img src="images/network-topology.png" style="width: 1000px;"/>

> **NOTE**: In the above diagram, the green network (where our workstation is connected to, just incase you're viewing this without colour) represents what would be the corporate network in a real-world environment, one that's routable. But within our virtual lab, this is the default network within the  - 192.168.122.0/24, so we can easily access the nodes and the OpenStack API's once the overcloud is deployed.

When we registered our nodes into Ironic we provided the MAC address of **eth0** for each node as the unique identifier, this ensures that when it does DHCP/PXE boot, Ironic knows exactly which machine it's dealing with, and during the initial bootstrap of the image onto the booting nodes, this is the only interface we care about. However, once the machine reboots into the image for the next stage of deployment OSP director needs to configure all of the networking interfaces. To do this, OSP director relies on a set of templates known as '**nic-configs**' to set the configuration for each interface, including any additional VLANs, bonds, or bridges. The primary requirement in our lab environment is going to be defining what happens with **eth1** - how the necessary VLANs are defined on-top of this interface, and how OpenStack can be deployed to use them.

# Out of the Box Options

OSP director ships with a number of **nic-config** templates that can be used as examples to build specific templates for the given environment that they're being deployed into. Examples include (and can be found in this directory):

	$ ll /usr/share/openstack-tripleo-heat-templates/network/config/
	total 0
	drwxr-xr-x. 2 root root 230 Apr  7 08:17 bond-with-vlans
	drwxr-xr-x. 2 root root 170 Apr  7 08:17 multiple-nics
	drwxr-xr-x. 2 root root 170 Apr  7 08:17 single-nic-linux-bridge-vlans
	drwxr-xr-x. 2 root root 205 Apr  7 08:17 single-nic-vlans

In each directory there's a template for each role (at least out of the box roles, **not** taking into consideration composable roles) and slight variations of the roles, e.g. compute with DPDK enabled, or controller with IPv6 support:

	$ ll /usr/share/openstack-tripleo-heat-templates/network/config/bond-with-vlans/
	total 68
	-rw-r--r--. 1 root root 5810 Jan  2 19:14 ceph-storage.yaml
	-rw-r--r--. 1 root root 6074 Jan  2 19:14 cinder-storage.yaml
	-rw-r--r--. 1 root root 6544 Jan  2 19:14 compute-dpdk.yaml
	-rw-r--r--. 1 root root 6074 Jan  2 19:14 compute.yaml
	-rw-r--r--. 1 root root 6259 Jan  2 19:14 controller-no-external.yaml
	-rw-r--r--. 1 root root 7041 Jan  2 19:14 controller-v6.yaml
	-rw-r--r--. 1 root root 6706 Jan  2 19:14 controller.yaml
	-rw-r--r--. 1 root root 2128 Jan  2 19:14 README.md
	-rw-r--r--. 1 root root 6073 Jan  2 19:14 swift-storage.yaml

Unfortunately, none of these out of the box templates suit our specific environment, i.e. multiple network interfaces but with a dedicated provisioning interface and a dedicated interface for everything else. These templates either assume that you have just a single network interface, or each network interface is for a different traffic type (common in Cisco UCS environments) or if you want all interfaces bonded together for resilience. Within our virtualised baremetal environment we've gone for something slightly different - certainly not recommended for production, but makes it easier to explain how the templates work. As none of these out of the box templates work for us, we're going to have to use custom nic-config templates to suit our requirements.

# Pre-prepared nic-config Templates

To save some time, we've provided a set of pre-prepared and validated nic-config templates for us to use that match our exact requirements based on the lab environment we're using. Let's copy this directory of nic-configs into our recently created ~/templates directory:

	$ cp -rf ~/labs/director/templates/nic-configs ~/templates/

We'll explore these files in a lot more detail below, but at a high-level these files define two network interfaces, the first interface being for provisioning (via the OSP director control plane network) and a second interface for running all OpenStack traffic on, including for providing external networking access, i.e. floating IP access, and also OpenStack API access via a routable network from our workstation. Despite these being custom templates they're heavily built from the "**single-nic-vlans**" example.

Let's verify that we have these in place:

	$ ll ~/templates/nic-configs/
	total 24
	-rw-rw-r--. 1 stack stack 4393 Apr 20 04:33 compute.yaml
	-rw-rw-r--. 1 stack stack 4949 Apr 20 04:33 controller.yaml
	-rw-rw-r--. 1 stack stack 4496 Apr 20 04:34 networker.yaml

You'll notice that this directory is slightly cut down from the examples above. This is primarily because we only have three different roles within our environment - we don't have dedicated storage nodes, nor are we using IPv6, etc. We have a single controller, a dedicated networker, and two compute nodes, hence why we only have templates that represent the roles that we want to deploy.

Let's take a look at what these templates actually look like. Just like all other templates, these are **yaml** formatted, and therefore whitespace is incredibly important in describing the hierarchy. Let's cut the main bit out of the template so we can explain the most important section (but please feel free to look into the entire file with the favourite text editor, just don't make any modifications before we start our deployment). The first command below will print the entire of the main section, but we've split the output to describe each section below.

The top level sector is the "**network_config**" type, where all of the interfaces and sub-interfaces are described programmatically. Underneath this we describe our first interface, **eth0**, and associate it with both an IP address (and netmask) from our **ControlPlane**, the network we use for provisioning, as well as a static route to the metadata service:


	$ grep -A56 network_config ~/templates/nic-configs/controller.yaml
	          network_config:
	            -
	              type: interface
	              name: eth0
	              use_dhcp: false
	              addresses:
	                -
	                  ip_netmask:
	                    list_join:
	                      - '/'
	                      - - {get_param: ControlPlaneIp}
	                        - {get_param: ControlPlaneSubnetCidr}
	              routes:
	                -
	                  ip_netmask: 169.254.169.254/32
	                  next_hop: {get_param: EC2MetadataIp}

Next, we create an Open vSwitch bridge called **br-ex**, and associate it with an IP address and a default route from the **ExternalNetwork**. We also add **eth1** as the physical interface that backs this bridge. Therefore any interfaces or sub-interfaces associated with this bridge will be able to egress and ingress via **eth1**.

	            -
	              type: ovs_bridge
	              name: br-ex
	              dns_servers: {get_param: DnsServers}
	              addresses:
	                -
	                  ip_netmask: {get_param: ExternalIpSubnet}
	              routes:
	                -
	                  default: true
	                  next_hop: {get_param: ExternalInterfaceDefaultRoute}
	              members:
	                -
	                  type: interface
	                  name: eth1
	                  # force the MAC address of the bridge to this interface
	                  primary: true
	                  
Then, we add multiple **VLAN** sub-interfaces to this bridge, one for each OpenStack network traffic type, noting that via a parameter it looks up the VLAN ID we want to assign to each traffic type:
	                  
	                -
	                  type: vlan
	                  vlan_id: {get_param: InternalApiNetworkVlanID}
	                  addresses:
	                    -
	                      ip_netmask: {get_param: InternalApiIpSubnet}
	                -
	                  type: vlan
	                  vlan_id: {get_param: StorageNetworkVlanID}
	                  addresses:
	                    -
	                      ip_netmask: {get_param: StorageIpSubnet}
	                -
	                  type: vlan
	                  vlan_id: {get_param: StorageMgmtNetworkVlanID}
	                  addresses:
	                    -
	                      ip_netmask: {get_param: StorageMgmtIpSubnet}
	                -
	                  type: vlan
	                  vlan_id: {get_param: TenantNetworkVlanID}
	                  addresses:
	                    -
	                      ip_netmask: {get_param: TenantIpSubnet}

Remember, whitespace is incredibly important for the network interface hierarchy, and the parameters that should be associated to each interface. When the machine boots up for the first time, this network template is provided to a tool called **os-net-config** which applies this template to the local machine. The above example is for the controller model, in which all VLANs are present, have a look at the compute one to see that there are a limited number of VLANs present.

> **NOTE**: OSP director has a lot of granularity when it comes to the physical networks that it utilises for network traffic types, mapped by the **ServiceNetMap**, in which you choose which network is used for each type. For example, you could combine certain traffic types onto one network if the number of VLANs are limited, or you want to minimise the number of network interfaces used. The templates used here satisfy the default network traffic types.

So, we've got these nic-config templates, but how do we tell OSP director that we want to actually use them? We can specify this in an environment file, by overriding specific TripleO Heat resource types - unless these are overriden, OSP director assumes that your nodes only have one network interface and carries all traffic over this interface. For this, let's create a dedicated network-config.yaml file in our ~/templates directory and update the **"resource_registry"** to point override the network configuration resources by specifying the location of our nic-config files:

	$ cat > ~/templates/network-config.yaml << EOF
	resource_registry:
	  OS::TripleO::Compute::Net::SoftwareConfig: /home/stack/templates/nic-configs/compute.yaml
	  OS::TripleO::Controller::Net::SoftwareConfig: /home/stack/templates/nic-configs/controller.yaml
	  OS::TripleO::Networker::Net::SoftwareConfig: /home/stack/templates/nic-configs/networker.yaml
	EOF

> **NOTE**: To reiterate, if you omit to advise OSP director of your nic-configs, the default model is to assume that your systems only have a single network interface and that it should be used for provisioning, control plane functionality, and all OpenStack service traffic. This is not a likely deployment in production, but it can suffice for testing.

Now we should override some additional resource definitions, ensuring that our networker node has ports (and therefore IP addresses and other configuration) for each of the additional network types that we want it to be on. By default it will just have the **ctlplane** network, but as it's going to be serving network requests we need it to also have network connectivity to the external, internal API, and tenant interfaces:

	$ cat >> ~/templates/network-config.yaml << EOF
	  OS::TripleO::Networker::Ports::ExternalPort: /usr/share/openstack-tripleo-heat-templates/network/ports/external.yaml
	  OS::TripleO::Networker::Ports::InternalApiPort: /usr/share/openstack-tripleo-heat-templates/network/ports/internal_api.yaml
	  OS::TripleO::Networker::Ports::TenantPort: /usr/share/openstack-tripleo-heat-templates/network/ports/tenant.yaml
	EOF

# Specifying the Network Details

We've got the nic-config templates ready, but they don't actually specify what IP addresses, VLAN's, DNS servers, default routes, etc, to use for each of the interface; all it shows is a link to a parameter which we haven't yet specified. Where do we specify these? Well, just like some of the other parameters, we need to specify these in our environment file. We need to make sure that OSP director knows how to satisfy all of the different network traffic types being requested and therefore need to provide information for each subnet, i.e. which network address range to use, what the default route is (if applicable) and if you're using VLAN isolation, what VLAN ID it should use for each network. There are also a few additional parameters that are advisable to set during this process.

Let's add this configuration to our existing network-config.yaml file, noting that we highly recommend that you use the options provided below given that we're using VLANs within our environment. Here we're setting the subnet size, the VLAN ID, and a range of IP's to use for each network traffic type. In addition, we've specified the network size and default route of the control plane network, as well as the DNS servers to use within the environment:

	$ cat >> ~/templates/network-config.yaml << EOF
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

As an example, you can see that the **external** network has been configured such that the subnet specification is **192.168.122.0/24**, with IP's available between 192.168.122.102 and 192.168.122.129, and that the **default route** is 192.168.122.1 (so it can route onto the public cloud network that we're using). Note also that this doesn't have a VLAN associated with it as it's a flat network.

Let's add some additional parameters to our network-config file, first let's set the public endpoint IP to be **192.168.122.100**, this tells OSP director that we want our virtual IP for reaching our overcloud to be on a specific IP address and not randomly assigned. In our setup we're using a single controller node, but in a production configuration this virtual IP would allow requests to be load balanced across a set of controller nodes for throughput and resiliency. We set this with the **PublicVirtualFixedIPs** parameter:

	$ echo "  PublicVirtualFixedIPs: [{'ip_address':'192.168.122.100'}]" >> ~/templates/network-config.yaml

Also, let's ensure that Neutron utilises VXLAN based tenant networks (the other options include VLAN and GRE, but our environment is particularly suited for VXLAN):

	$ echo "  NeutronTunnelTypes: 'vxlan'" >> ~/templates/network-config.yaml

> **NOTE**: There is expected whitespace at the start of this file as these entries are under a section called **parameter_defaults**, which allows us to overwrite the default parameters provided by the out of the box configuration. If you look into the entire file you'll see why we put the whitespace here to conform to YAML formatting requirements.
