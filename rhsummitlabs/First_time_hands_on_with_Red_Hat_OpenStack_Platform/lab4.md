#**Lab 4: Creation of Tenant Networks in Neutron (Networking Service)**

##**Introduction**

Neutron is OpenStack's Networking service and provides an abstract virtual networking API, enabling administrators and end-users to manage both virtual and physical networks for their instances on-top of OpenStack. Neutron simply provides an API for self-service and management but relies on underlying technologies for the actual implementation via Neutron-plugins. The class environment makes use of Open vSwitch, but there are many other plugins available upstream such as Nuage, Juniper Contrail, etc. Neutron allows users to create rich networking topologies within their own projects - having full control over their networks.

We're going to be starting our first instances in the next lab, but for an instance to start, it must be assigned a network to attach to (amongst other configuration requirements, such as the image you want to use). These are typically private networks, i.e. have no public connectivity and is primarily used for virtual machine interconnects and private networking - called a "**tenant network**". Within OpenStack we bridge the private network out to the real world via a public (or 'external') network, it is simply the network interface in which public traffic will connect into, and is typically where you'd assign "**floating IP's**", i.e. IP addresses that are dynamically assigned to instances so that external traffic can be routed through correctly. Instances don't typically have direct access to the public network interface, they only see the private network and the user is responsible for optionally connecting a virtual router to interlink the two networks for both external access and inbound access from outside the private network.

There are many other options for networking configuration with Neutron that does allow instances to be directly connected to an external network (e.g. provider networks, or SR/IOV), but for the purpose of this lab we're going to keep things relatively simple and use the tenant network construct with floating IP's for inbound routing via Neutron's L3 agent.

Neutron's internal to external network architecture (with the L3 agent) looks like the following:

<img src="images/neutron.png" style="width: 1000px;" border="2"/>

##**Configuring the External Network**

As we mentioned before, external networks provide a mechanism for OpenStack instances to communicate to the outside, but critically to allow instances to have inbound connectivity from the outside via the mechanism of a floating IP. These external networks are typically existing datacentre networks, and OpenStack administrators will need to tell Neutron how to utilise such networks. An administrator must define a name for the network, a subnet (including address ranges), and advise Neutron of which networking bridge (which would be associated with a physical interface) to route external traffic to. It's possible to have multiple external networks within OpenStack, and users can select which networks they want their instance to be able to see, and also which networks they want a floating IP to be listening on.

For the purpose of this lab, we're going to create an external network that is connected to the dedicated public-cloud based environment that you're using (**192.168.122.0/24**). This will allow us to connect directly into instances via floating IP's during a later lab. The first thing we need to do is make sure that we've sourced our environment file for our demo user, noting that the default security policy only allows administrators to define external networks, but we gave our demo user 'admin' privileges in an earlier lab (even though that's not strictly best practice):

	$ source ~/demorc

Next, we need to start defining our external network. This follows the same logical steps as creating _any_ network, regardless of whether you're an administrator or not. However, we need to specify a number of parameters, specifically advising Neutron that this is an external network, and defining exactly how it connects to the outside world. Let's first see all of the networking agents and services that are running within our environment:

	$ openstack network agent list
	+--------------------------------------+----------------------+------------------------------+-------------------+-------+-------+---------------------------+
	| ID                                   | Agent Type           | Host                         | Availability Zone | Alive | State | Binary                    |
	+--------------------------------------+----------------------+------------------------------+-------------------+-------+-------+---------------------------+
	| 124371fc-1bce-48a8-af61-cde99b83e74f | Open vSwitch agent   | summit-networker.localdomain | None              | :-)   | UP    | neutron-openvswitch-agent |
	| 247d49b7-537e-44d9-a3c6-19844be5da88 | Loadbalancerv2 agent | summit-networker.localdomain | None              | :-)   | UP    | neutron-lbaasv2-agent     |
	| 25bd6fe6-b29c-4d0f-9d49-7d7ee0f7d820 | L3 agent             | summit-networker.localdomain | nova              | :-)   | UP    | neutron-l3-agent          |
	| 7bbb6b31-c10c-49f5-a6da-fe50e1670ee7 | Open vSwitch agent   | summit-compute2.localdomain  | None              | :-)   | UP    | neutron-openvswitch-agent |
	| a0ec6b56-bb99-449b-829b-0ee0ab9cf138 | Open vSwitch agent   | summit-compute1.localdomain  | None              | :-)   | UP    | neutron-openvswitch-agent |
	| b5c90ce6-af7f-44c4-aaf5-5beb5afddddd | DHCP agent           | summit-networker.localdomain | nova              | :-)   | UP    | neutron-dhcp-agent        |
	| fd82f7f7-e7fe-4741-93e9-db0484192531 | Metadata agent       | summit-networker.localdomain | None              | :-)   | UP    | neutron-metadata-agent    |
	+--------------------------------------+----------------------+------------------------------+-------------------+-------+-------+---------------------------+

What you'll see is that we have three systems that have networking functions, the '**summit-networker**' machine, and the '**summit-computeX**' machines. The **compute** machines only need to worry about connectivity of virtual machine networks, whereas the dedicated networker machine has to also worry about providing things like external network routing and DHCP services for OpenStack networks and the instances that run on-top of them.

On each of these nodes you'll see '**neutron-openvswitch-agent**' running - this is the agent that's responsible for configuring Open vSwitch on each machine, and setting up traffic flows for each virtual machine and all related services like DHCP, etc. As the **networker** machine is also responsible for network routing (via the L3 agent), it needs to know how to route traffic between administratively-defined external networks and internal tenant networks that the users have control over, hence why you can see the '**neutron-l3-agent**' running on the networker node.

The important thing to understand here is that Neutron only understands **logical** "physical network" names - these logical network names are translated into real underlying networks via a network bridge by the plugin that we're using (in our case, Open vSwitch). To understand how this works, we need to look at the defined logical networks that Open vSwitch is exposing to Neutron via the '**bridge_mappings**' extension. Select the 'id' for the Open vSwitch agent running on '**summit-networker**' in the following command, and it will show you the current bridge mappings:

	$ openstack network agent show 9c2d624f-e009-4910-bb20-36a5973f2a9e -f json | grep -A2 bridge_mappings
		"bridge_mappings": {
			"datacentre": "br-ex"
		}

What this shows it that we have a physical network name of "**datacentre**", which if used, would tell Neutron to route all external traffic for that network onto the Open vSwitch bridge "**br-ex**". If we look at the "br-ex" bridge on our **summit-networker** machine we can see that it is already attached to a real world physical network interface, **"eth1"**. To do this, we'll need to briefly ssh to that machine:

	$ ssh root@summit-networker sudo ovs-vsctl show | grep -A20 "Bridge br-ex"
    Bridge br-ex                                                        <--- the bridge name
        Controller "tcp:127.0.0.1:6633"
            is_connected: true
        fail_mode: secure
        Port br-ex
            Interface br-ex
                type: internal
        Port "eth1"                                                     <--- the physical ethernet connection
            Interface "eth1"
        Port "vlan201"                                                  <--- additional VLANs for other traffic
            tag: 201
            Interface "vlan201"
                type: internal
        Port phy-br-ex
            Interface phy-br-ex
                type: patch
                options: {peer=int-br-ex}
        Port "vlan101"                                                 <--- additional VLANs for other traffic
            tag: 101
            Interface "vlan101"
                type: internal

> **NOTE:** We don't need to specify a password to execute remote commands on the networker node - we've already pre-populated the overcloud nodes with a secure shell key for convenience. We've also highlighted certain sections in the OVS output above to make things clear.

To summarise, if we define an external network with the Neutron logical name **"datacentre"**, the traffic will utilise **"eth1"** as a physical network via the Open vSwitch bridge **"br-ex"**.

Let's tie this together and create our external network:

	$ openstack network create external --external \
	--provider-physical-network datacentre \
	--provider-network-type flat \
	--project admin
	
	+---------------------------+--------------------------------------+
	| Field                     | Value                                |
	+---------------------------+--------------------------------------+
	| admin_state_up            | UP                                   |
	| availability_zone_hints   |                                      |
	| availability_zones        |                                      |
	| created_at                | 2018-04-09T14:10:15Z                 |
	| description               |                                      |
	| dns_domain                | None                                 |
	| id                        | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| ipv4_address_scope        | None                                 |
	| ipv6_address_scope        | None                                 |
	| is_default                | False                                |
	| is_vlan_transparent       | None                                 |
	| mtu                       | 1500                                 |
	| name                      | external                             |
	| port_security_enabled     | True                                 |
	| project_id                | 9eb95e04cff34482b44b8672b65caac9     |
	| provider:network_type     | flat                                 |   <--- Note the 'flat' type
	| provider:physical_network | datacentre                           |   <--- Note the 'datacentre' mapping
	| provider:segmentation_id  | None                                 |
	| qos_policy_id             | None                                 |
	| revision_number           | 4                                    |
	| router:external           | External                             |
	| segments                  | None                                 |
	| shared                    | False                                |
	| status                    | ACTIVE                               |
	| subnets                   |                                      |
	| tags                      |                                      |
	| updated_at                | 2018-04-09T14:10:16Z                 |
	+---------------------------+--------------------------------------+	
There are a number of key parameters here, for reference use the table below:

| Parameter  | Details  |
|---|---|
| --provider-physical-network  |  This defines the **logical** physical network name that Neutron uses to look up the bridge mapping |
| --provider-network-type  | This sets the network type, e.g. if it's flat, or VLAN tagged  |
| --external  | This tells Neutron that this is an **external** network and can be used for routing (e.g. floating IP's) |
| --project X | We specifially tell Neutron to assign ownership of this network to the '**admin**' project, i.e. not our new users project. External networks should always be owned by the admin project, or a dedicated project, not a normal user project. |

Let's associate a subnet to our external network so that Neutron knows what IP ranges to use for outbound routing (SNAT) and inbound routing via floating IP's (DNAT), noting that this coincides with our virtualised default network, accessible from our workstation:

	$ openstack subnet create external_subnet --network external \
	--subnet-range 192.168.122.0/24 \
	--allocation-pool start=192.168.122.200,end=192.168.122.249 \
	--no-dhcp --dns-nameserver 192.168.122.1 --gateway 192.168.122.1 \
	--project admin

	+-------------------------+--------------------------------------+
	| Field                   | Value                                |
	+-------------------------+--------------------------------------+
	| allocation_pools        | 192.168.122.200-192.168.122.249      |
	| cidr                    | 192.168.122.0/24                     |
	| created_at              | 2018-04-09T14:11:09Z                 |
	| description             |                                      |
	| dns_nameservers         | 192.168.122.1                        |
	| enable_dhcp             | False                                |
	| gateway_ip              | 192.168.122.1                        |
	| host_routes             |                                      |
	| id                      | f1b165b1-f839-469f-b649-4ed2cc71b873 |
	| ip_version              | 4                                    |
	| ipv6_address_mode       | None                                 |
	| ipv6_ra_mode            | None                                 |
	| name                    | external_subnet                      |
	| network_id              | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| project_id              | 9eb95e04cff34482b44b8672b65caac9     |
	| revision_number         | 0                                    |
	| segment_id              | None                                 |
	| service_types           |                                      |
	| subnetpool_id           | None                                 |
	| tags                    |                                      |
	| updated_at              | 2018-04-09T14:11:09Z                 |
	| use_default_subnet_pool | None                                 |
	+-------------------------+--------------------------------------+

As previously, there are a number of key parameters here, for reference use the table below:

| Parameter  | Details  |
|---|---|
|--subnet-range  |  This defines the CIDR of the network that we're wanting to create |
|--allocation-pool  |  This sets the range of IP's within the CIDR that we can use to allocate as floating IP's, or to use as SNAT IP's |
|--no-dhcp  |  This disables any form of DHCP service for this network - this is an external network that we use for bridging traffic between internal and external networks. In this lab, instances cannot be directly connected to the external network without routing (NAT) taking place via the L3 agent, although it is possible to establish this type of alternative configuration. Therefore, as instances won't be DHCP'ing on this network, we disable this functionality. |
|--dns-nameserver  |  This defines the nameserver that instances can use, although this is not required as it's provided only when DHCP is enabled. It's shown here for completeness. |
|--gateway  |  This defines the upstream network gateway for the external network, i.e. the next-hop that would be used within that external network. This corresponds to the virtualised network gateway provided by the underlying host |
| --project X | We specifially tell Neutron to assign ownership of this subnet to the '**admin**' project, i.e. not our new users project. Just like external networks, their associated subnets should always be owned by the admin project, or a dedicated project, not a normal user project. |

Now that we've got the external network configured we need to create internal networks for our instances as they won't have direct access to this network. This is the responsibility of a user within a project, the external network is just exposed to all of the projects that are created; whilst they cannot modify it, they can attach a virtual router to it for routing and floating IP access.

##**Creating Tenant Networks**

Next, let's create a tenant network for our instances to use. If we create a network and don't specify any additional parameters, Neutron assumes that it's a private tenant network that uses some form of network isolation mechanism (to isolate tenant networks between other projects) such as VLAN, or VXLAN. In our environment we're defaulting to **VXLAN**:

	$ openstack network create internal
	+---------------------------+--------------------------------------+
	| Field                     | Value                                |
	+---------------------------+--------------------------------------+
	| admin_state_up            | UP                                   |
	| availability_zone_hints   |                                      |
	| availability_zones        |                                      |
	| created_at                | 2018-04-09T14:11:52Z                 |
	| description               |                                      |
	| dns_domain                | None                                 |
	| id                        | 68abade7-ce4c-4592-9128-25d52e95a21a |
	| ipv4_address_scope        | None                                 |
	| ipv6_address_scope        | None                                 |
	| is_default                | False                                |
	| is_vlan_transparent       | None                                 |
	| mtu                       | 1450                                 |
	| name                      | internal                             |
	| port_security_enabled     | True                                 |
	| project_id                | f991d44fac91419c8e6016184381871a     |
	| provider:network_type     | vxlan                                |   <--- Note the 'vxlan' type
	| provider:physical_network | None                                 |
	| provider:segmentation_id  | 3                                    |
	| qos_policy_id             | None                                 |
	| revision_number           | 3                                    |
	| router:external           | Internal                             |
	| segments                  | None                                 |
	| shared                    | False                                |
	| status                    | ACTIVE                               |
	| subnets                   |                                      |
	| tags                      |                                      |
	| updated_at                | 2018-04-09T14:11:53Z                 |
	+---------------------------+--------------------------------------+

> **NOTE:** As we're using VXLAN, it's assumed that we're using the default frame size of 1500bytes, and has automatically reduced the MTU given to instances to 1450 to accomodate the VXLAN header.
	
Next, associate a subnet with this network as before, but using a completely different subnet CIDR:
	
	$ openstack subnet create internal_subnet --network internal \
		--subnet-range 172.16.1.0/24 --dns-nameserver 192.168.122.1
	
	+-------------------------+--------------------------------------+
	| Field                   | Value                                |
	+-------------------------+--------------------------------------+
	| allocation_pools        | 172.16.1.2-172.16.1.254              |
	| cidr                    | 172.16.1.0/24                        |
	| created_at              | 2018-04-09T14:12:36Z                 |
	| description             |                                      |
	| dns_nameservers         | 192.168.122.1                        |
	| enable_dhcp             | True                                 |
	| gateway_ip              | 172.16.1.1                           |
	| host_routes             |                                      |
	| id                      | 4d38f43b-402e-48a5-b048-3092d6f7da02 |
	| ip_version              | 4                                    |
	| ipv6_address_mode       | None                                 |
	| ipv6_ra_mode            | None                                 |
	| name                    | internal_subnet                      |
	| network_id              | 68abade7-ce4c-4592-9128-25d52e95a21a |
	| project_id              | f991d44fac91419c8e6016184381871a     |
	| revision_number         | 0                                    |
	| segment_id              | None                                 |
	| service_types           |                                      |
	| subnetpool_id           | None                                 |
	| tags                    |                                      |
	| updated_at              | 2018-04-09T14:12:36Z                 |
	| use_default_subnet_pool | None                                 |
	+-------------------------+--------------------------------------+	
This means that any instances that we start on this internal network will receive an IP address (via DHCP) on the **172.16.1.0/24** network, noting that by default, Neutron assumes that you want to have DHCP enabled, and will automatically assign an address to use for the default gateway (this will be the address that the L3 agent uses as a gateway for NAT).

At the moment, any instances attached to this network will be completely **isolated**; there's no routing between the internal and the external network - despite having a gateway defined we cannot get out, and we cannot get in. Neutron allows you to bridge tenant networks and external networks via the concept of virtual **routers**, and is what gets implemented by the L3 agent to perform such capabilities. Let's create a virtual router for our network:

	$ openstack router create demo_router
	+-------------------------+--------------------------------------+
	| Field                   | Value                                |
	+-------------------------+--------------------------------------+
	| admin_state_up          | UP                                   |
	| availability_zone_hints |                                      |
	| availability_zones      |                                      |
	| created_at              | 2018-04-09T14:13:18Z                 |
	| description             |                                      |
	| distributed             | False                                |
	| external_gateway_info   | None                                 |
	| flavor_id               | None                                 |
	| ha                      | False                                |
	| id                      | f8d34761-f57b-4697-8e37-741f274c4ff4 |
	| name                    | demo_router                          |
	| project_id              | f991d44fac91419c8e6016184381871a     |
	| revision_number         | None                                 |
	| routes                  |                                      |
	| status                  | ACTIVE                               |
	| tags                    |                                      |
	| updated_at              | 2018-04-09T14:13:18Z                 |
	+-------------------------+--------------------------------------+	
Now let's associate a gateway for this router, in other words, to which external network do we want outbound traffic to route through? That'll be our '**external**' network...

	$ openstack router set demo_router --external-gateway external
	
Then we need to add an interface to the internal network that we created in a previous step, noting that we need to associate it to the subnet, and not the network (as a network may have multiple subnets associated with it):
	
	$ openstack router add subnet demo_router internal_subnet

> **NOTE**: The above two commands produce no output unless there have been any errors.

In conclusion, we've created a private 'tenant network', called **'internal'** for our instances to use and have created a virtual router for our internal network to route traffic to the outside; onto the administratively defined external network called **"external"**. In a later lab we'll demonstrate how routing works and how we can associate floating IP's to our instances for inbound connectivity.
