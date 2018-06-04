##**Testing, Investigating, and Using ODL**

So far, all we've done is take a basic look at what the architecture looks like; we've not deployed any resources, we've not looked into the OpenDaylight integration, and we certainly don't know if it's even working yet. Right now you're going to have to trust us that it has been configured properly, but we'll start testing that right away.

In this section we're going to be verifying the environment to ensure that it first of all functions properly, but secondly adheres to a well-integrated OpenDaylight setup. The first thing we should do is make sure that OpenStack itself is working; let's build out some resources within our overcloud environment.

### Uploading an Image to Glance

As part of the lab, we'll use an image already residing on the filesystem for you; this is a stripped down version of RHEL 7.4 with a pre-set root password; we'll upload it as our own. Let's verify that the disk image is as expected and has the correct properties:

	$ qemu-img info ~/labs/rhel-server-7.4-x86_64-kvm.qcow2
	image: /home/stack/labs/rhel-server-7.4-x86_64-kvm.qcow2
	file format: qcow2
	virtual size: 10G (10737418240 bytes)
	disk size: 538M
	cluster_size: 65536
	Format specific information:
	    compat: 0.10
	    refcount bits: 16

Next we can create a new image within Glance and import its contents, it may take a few minutes to copy the data. Let's ensure that we've sourced our **overcloudrc** file (noting that it doesn't matter if you've already sourced this file - repeating the source command is safe), and proceed with the image creation:

	$ source ~/overcloudrc

	$ openstack image create rhel7 --public \
		--disk-format qcow2 --container-format bare \
		--file ~/labs/rhel-server-7.4-x86_64-kvm.qcow2

	+------------------+------------------------------------------------------------------------------+
	| Field            | Value                                                                        |
	+------------------+------------------------------------------------------------------------------+
	| checksum         | 2065a01cacd127c2b5f23b1738113325                                             |
	| container_format | bare                                                                         |
	| created_at       | 2018-04-16T20:49:39Z                                                         |
	| disk_format      | qcow2                                                                        |
	| file             | /v2/images/2650782f-e95c-4309-b041-49f79468413d/file                         |
	| id               | 2650782f-e95c-4309-b041-49f79468413d                                         |
	| min_disk         | 0                                                                            |
	| min_ram          | 0                                                                            |
	| name             | rhel7                                                                        |
	| owner            | c14b205e428e43319fe43fb0396bd092                                             |
	| properties       | direct_url='swift+config://ref1/glance/2650782f-e95c-4309-b041-49f79468413d' |
	| protected        | False                                                                        |
	| schema           | /v2/schemas/image                                                            |
	| size             | 564330496                                                                    |
	| status           | active                                                                       |
	| tags             |                                                                              |
	| updated_at       | 2018-04-16T20:49:49Z                                                         |
	| virtual_size     | None                                                                         |
	| visibility       | public                                                                       |
	+------------------+------------------------------------------------------------------------------+

This may take a minute or so, but you can verify that the image is **active** and ready:

	$ openstack image list
	+--------------------------------------+-------+--------+
	| ID                                   | Name  | Status |
	+--------------------------------------+-------+--------+
	| 2650782f-e95c-4309-b041-49f79468413d | rhel7 | active |
	+--------------------------------------+-------+--------+

### Creating a Flavor to use

Next we're going to need a flavor for our environment; as we're relatively resource constrained the out of the box flavors don't quite give us what we need, so let's create an additional flavor for us to use:

	$ openstack flavor create --ram 2048 --disk 10 --vcpus 2 --id 6 m1.labs
	+----------------------------+---------+
	| Field                      | Value   |
	+----------------------------+---------+
	| OS-FLV-DISABLED:disabled   | False   |
	| OS-FLV-EXT-DATA:ephemeral  | 0       |
	| disk                       | 10      |
	| id                         | 6       |
	| name                       | m1.labs |
	| os-flavor-access:is_public | True    |
	| properties                 |         |
	| ram                        | 2048    |
	| rxtx_factor                | 1.0     |
	| swap                       |         |
	| vcpus                      | 2       |
	+----------------------------+---------+

### Networking Configuration

A little bit of background given that this is a networking lab...

When it comes to OpenStack Networking there are many different types of networks to consider; first you have the **infrastructure** networks where OpenStack services communicate across, where storage data is transferred, and how administrators connect to the machines, and then you have the **instance** networks, i.e. those networks that users can attach their workloads onto. These networks also come in all different shapes and sizes, depending on the use-case, e.g. whether instances are directly connected to an existing datacentre network (e.g. **provider** networks), whether they use high performance configurations such as SR-IOV or DPDK, or whether they use the default **tenant** networking model.

Tenant networks provide fully granular control of networking inside of each tenant, i.e. each tenant can create networks, have control over the subnet allocation, and provide additional services such as DHCP without requiring administrative access. Tenant networks are isolated from one another using either overlay technologies such as VXLAN, or via traditional isolation mechanisms such as VLAN, with the assignment and configuration of each type being automated by the OpenStack components. The problem with tenant networks is that they are inherently isolated networks that have no outbound or inbound connectivity - they’re designed to provide networking access between instances residing within said tenant.

To provide routing both ingress and egress to that tenant network requires the attachment of a virtual router, controlled by the chosen OpenStack Networking plugin (in our case OpenDaylight) where said router will, by default, provide SNAT capabilities to allow egress traffic. On-top of this, the virtual router can provide DNAT capabilities for ingress traffic through the concept of a floating-IP, which is attached on a 1:1 basis to an instance, allowing NAT based communication from an external routed network into the tenant network.

In a vanilla OpenStack configuration (e.g. one that uses ML2/OVS), this routing mechanism takes place on either dedicated networker nodes, or via the OpenStack controller nodes themselves in a centralised configuration, which can cause potential bottlenecks in performance as all North/South traffic goes through a centralised set of nodes. But with OpenDaylight, the responsibility for DNAT/SNAT resides with the compute node hosting said virtual machine.  SNAT is implemented by using conntrack (part of Linux Netfilter suite) to track connections and then Netfilter entries handle NAT translation.

We're going to use the default **tenant** network model in this lab with distributed routing on our compute nodes through an **external** network that we're going to define. We'll create these networks here and investigate how they're constructed and enabled through OpenDaylight later on. We're using the provider network extension to advise Neutron on the logical network mapping (i.e. how the external network is physically attached on the underlying nodes); we'll explore this later too.

First define the external network:

	$ openstack network create external --external \
	--provider-physical-network datacentre \
	--provider-network-type flat
	
	+---------------------------+--------------------------------------+
	| Field                     | Value                                |
	+---------------------------+--------------------------------------+
	| admin_state_up            | UP                                   |
	| availability_zone_hints   |                                      |
	| availability_zones        |                                      |
	| created_at                | 2018-04-18T12:52:01Z                 |
	| description               |                                      |
	| dns_domain                | None                                 |
	| id                        | 642f4496-7773-4d17-8f82-52fe8efc2a62 |
	| ipv4_address_scope        | None                                 |
	| ipv6_address_scope        | None                                 |
	| is_default                | False                                |
	| is_vlan_transparent       | None                                 |
	| mtu                       | 1500                                 |
	| name                      | external                             |
	| port_security_enabled     | True                                 |
	| project_id                | c14b205e428e43319fe43fb0396bd092     |
	| provider:network_type     | flat                                 |  <--- Note the 'flat' type
	| provider:physical_network | datacentre                           |  <--- Note the 'datacentre' mapping
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
| --provider-physical-network  |  This defines the **logical** physical network name that OpenDaylight uses to map this virtual network to a physical network on the compute host |
| --provider-network-type  | This sets the network type, e.g. if it's flat, or VLAN tagged  |
| --external  | This tells Neutron that this is an **external** network and can be used for routing (e.g. floating IP's) |

Let's associate a subnet to our external network so that Neutron knows what IP ranges to use for outbound routing (SNAT) and inbound routing via floating IP's (DNAT), noting that this coincides with the network we have routing access from our undercloud machine:

	$ openstack subnet create external_subnet --network external \
	--subnet-range 192.168.122.0/24 \
	--allocation-pool start=192.168.122.200,end=192.168.122.249 \
	--no-dhcp --dns-nameserver 192.168.122.1 --gateway 192.168.122.1

	+-------------------------+--------------------------------------+
	| Field                   | Value                                |
	+-------------------------+--------------------------------------+
	| allocation_pools        | 192.168.122.200-192.168.122.249      |
	| cidr                    | 192.168.122.0/24                     |
	| created_at              | 2018-04-18T12:52:05Z                 |
	| description             |                                      |
	| dns_nameservers         | 192.168.122.1                        |
	| enable_dhcp             | False                                |
	| gateway_ip              | 192.168.122.1                        |
	| host_routes             |                                      |
	| id                      | f771130a-227d-4945-b836-8f80f970a20f |
	| ip_version              | 4                                    |
	| ipv6_address_mode       | None                                 |
	| ipv6_ra_mode            | None                                 |
	| name                    | external_subnet                      |
	| network_id              | 642f4496-7773-4d17-8f82-52fe8efc2a62 |
	| project_id              | c14b205e428e43319fe43fb0396bd092     |
	| revision_number         | 0                                    |
	| segment_id              | None                                 |
	| service_types           |                                      |
	| subnetpool_id           | None                                 |
	| tags                    |                                      |
	| updated_at              | 2018-04-18T12:52:05Z                 |
	| use_default_subnet_pool | None                                 |
	+-------------------------+--------------------------------------+

As previously, there are a number of key parameters here, for reference use the table below:

| Parameter  | Details  |
|---|---|
|--subnet-range  |  This defines the CIDR of the network that we're wanting to create |
|--allocation-pool  |  This sets the range of IP's within the CIDR that we can use to allocate as floating IP's, or to use as SNAT IP's |
|--no-dhcp  |  This disables any form of DHCP service for this network - this is an external network that we use for bridging traffic between internal and external networks. In this lab, instances cannot be directly connected to the external network without routing (NAT) taking place via the L3 agent, although it is possible to establish this type of alternative configuration. Therefore, as instances won't be DHCP'ing on this network, we disable this functionality. |
|--dns-nameserver  |  This defines the nameserver that instances can use, although this is not required as it's provided only when DHCP is enabled. It's shown here for completeness. |
|--gateway  |  This defines the upstream network gateway for the external network, i.e. the next-hop that would be used within that external network. |

Now that we've got the external network configured we need to create **internal** networks for our instances as they won't have direct access to this network. This is the responsibility of a user within a project, the external network is just exposed to all of the projects that are created; whilst they cannot modify it, they can attach a virtual router to it for routing and floating IP access.

If we create a network and don't specify any additional parameters, Neutron assumes that it's a private tenant network that uses some form of network isolation mechanism (to isolate tenant networks between other projects) such as VLAN, or VXLAN. In our environment we're defaulting to **VXLAN**:

	$ openstack network create internal
	+---------------------------+--------------------------------------+
	| Field                     | Value                                |
	+---------------------------+--------------------------------------+
	| admin_state_up            | UP                                   |
	| availability_zone_hints   |                                      |
	| availability_zones        |                                      |
	| created_at                | 2018-04-18T13:13:02Z                 |
	| description               |                                      |
	| dns_domain                | None                                 |
	| id                        | 22f9a9cd-1da0-4c6a-a815-d25da07aa18d |
	| ipv4_address_scope        | None                                 |
	| ipv6_address_scope        | None                                 |
	| is_default                | False                                |
	| is_vlan_transparent       | None                                 |
	| mtu                       | 1450                                 |
	| name                      | internal                             |
	| port_security_enabled     | True                                 |
	| project_id                | c14b205e428e43319fe43fb0396bd092     |
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
	| created_at              | 2018-04-18T13:13:09Z                 |
	| description             |                                      |
	| dns_nameservers         | 192.168.122.1                        |
	| enable_dhcp             | True                                 |
	| gateway_ip              | 172.16.1.1                           |
	| host_routes             |                                      |
	| id                      | 8146d9b3-8c97-436c-88df-3b3cd88ec54f |
	| ip_version              | 4                                    |
	| ipv6_address_mode       | None                                 |
	| ipv6_ra_mode            | None                                 |
	| name                    | internal_subnet                      |
	| network_id              | 22f9a9cd-1da0-4c6a-a815-d25da07aa18d |
	| project_id              | c14b205e428e43319fe43fb0396bd092     |
	| revision_number         | 0                                    |
	| segment_id              | None                                 |
	| service_types           |                                      |
	| subnetpool_id           | None                                 |
	| tags                    |                                      |
	| updated_at              | 2018-04-18T13:13:09Z                 |
	| use_default_subnet_pool | None                                 |
	+-------------------------+--------------------------------------+	
This means that any instances that we start on this internal network will receive an IP address (via DHCP) on the **172.16.1.0/24** network, noting that by default, Neutron assumes that you want to have DHCP enabled, and will automatically assign an address to use for the default gateway (this will be the address that the L3 agent uses as a gateway for NAT).

At the moment, any instances attached to this network will be completely **isolated**; there's no routing between the internal and the external network - despite having a gateway defined we cannot get out, and we cannot get in. Neutron allows you to bridge tenant networks and external networks via virtual **routers**, and is what gets implemented by the OpenDaylight in a distributed function to perform such capabilities. Let's create a virtual router for our network:

	$ openstack router create demo_router
	+-------------------------+--------------------------------------+
	| Field                   | Value                                |
	+-------------------------+--------------------------------------+
	| admin_state_up          | UP                                   |
	| availability_zone_hints | None                                 |
	| availability_zones      | None                                 |
	| created_at              | 2018-04-18T13:13:18Z                 |
	| description             |                                      |
	| distributed             | False                                |
	| external_gateway_info   | None                                 |
	| flavor_id               | None                                 |
	| ha                      | False                                |
	| id                      | fa88e28a-34c4-4b8e-8c33-b67e54c4c9c0 |
	| name                    | demo_router                          |
	| project_id              | c14b205e428e43319fe43fb0396bd092     |
	| revision_number         | None                                 |
	| routes                  |                                      |
	| status                  | ACTIVE                               |
	| tags                    |                                      |
	| updated_at              | 2018-04-18T13:13:18Z                 |
	+-------------------------+--------------------------------------+	
Now let's associate a gateway for this router, in other words, to which external network do we want outbound traffic to route through? That'll be our '**external**' network...

	$ openstack router set demo_router --external-gateway external
	
Then we need to add an interface to the internal network that we created in a previous step, noting that we need to associate it to the subnet, and not the network (as a network may have multiple subnets associated with it):
	
	$ openstack router add subnet demo_router internal_subnet

> **NOTE**: The above two commands produce no output unless there have been any errors.

In conclusion, we've created a private 'tenant network', called **'internal'** for our instances to use and have created a virtual router for our internal network to route traffic to the outside; onto the administratively defined external network called **"external"**.

### Instance Boot up


Next, boot a new instance on OpenStack using our new flavor, the **internal** tenant network, and the image we uploaded earlier ("**rhel7**"), ensuring that you specify a name for the instance, below we use "**my_vm**":

	$ openstack server create --flavor m1.labs --image rhel7 --network internal my_vm
	+-------------------------------------+----------------------------------------------+
	| Field                               | Value                                        |
	+-------------------------------------+----------------------------------------------+
	| OS-DCF:diskConfig                   | MANUAL                                       |
	| OS-EXT-AZ:availability_zone         |                                              |
	| OS-EXT-SRV-ATTR:host                | None                                         |
	| OS-EXT-SRV-ATTR:hypervisor_hostname | None                                         |
	| OS-EXT-SRV-ATTR:instance_name       |                                              |
	| OS-EXT-STS:power_state              | NOSTATE                                      |
	| OS-EXT-STS:task_state               | scheduling                                   |
	| OS-EXT-STS:vm_state                 | building                                     |
	| OS-SRV-USG:launched_at              | None                                         |
	| OS-SRV-USG:terminated_at            | None                                         |
	| accessIPv4                          |                                              |
	| accessIPv6                          |                                              |
	| addresses                           |                                              |
	| adminPass                           | X5txyyS2yZDy                                 |
	| config_drive                        |                                              |
	| created                             | 2018-04-18T13:20:15Z                         |
	| flavor                              | m1.labs (6)                                  |
	| hostId                              |                                              |
	| id                                  | 5d24c6de-2d4d-42d6-bcd0-52dd2770de69         |
	| image                               | rhel7 (81ff217a-38c5-4275-9ad4-f77a10e2a08b) |
	| key_name                            | None                                         |
	| name                                | my_vm                                        |
	| progress                            | 0                                            |
	| project_id                          | c14b205e428e43319fe43fb0396bd092             |
	| properties                          |                                              |
	| security_groups                     | name='default'                               |
	| status                              | BUILD                                        |
	| updated                             | 2018-04-18T13:20:15Z                         |
	| user_id                             | 1f474841c896452592072710e97d9ddc             |
	| volumes_attached                    |                                              |
	+-------------------------------------+----------------------------------------------+
	
We can verify that our system has been started successfully with the following command, noting that it may take a few minutes to become active:

	$ openstack server list
	+--------------------------------------+-------+--------+---------------------+-------+---------+
	| ID                                   | Name  | Status | Networks            | Image | Flavor  |
	+--------------------------------------+-------+--------+---------------------+-------+---------+
	| 5d24c6de-2d4d-42d6-bcd0-52dd2770de69 | my_vm | ACTIVE | internal=172.16.1.7 | rhel7 | m1.labs |
	+--------------------------------------+-------+--------+---------------------+-------+---------+

> **NOTE**: You may see that the system is in a **"SPAWNING"** state for a few minutes, this is to be expected as the machine provisions itself. Please be patient and allow it to start. We want to make sure that it goes into an **"ACTIVE"** state.

### **Creating and Assigning Floating IP's**

As highlighted above, our launched instance is only on the internal network and has no connectivity from the outside world. We'll request a floating IP for our instance on the external network:

	$ openstack floating ip create external
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-18T13:35:49Z                 |
	| description         |                                      |
	| fixed_ip_address    | None                                 |
	| floating_ip_address | 192.168.122.200                      |
	| floating_network_id | 642f4496-7773-4d17-8f82-52fe8efc2a62 |
	| id                  | 4965c561-c97e-48c0-b89e-7f9064d399f6 |
	| name                | 192.168.122.200                      |
	| port_id             | None                                 |
	| project_id          | c14b205e428e43319fe43fb0396bd092     |
	| revision_number     | 0                                    |
	| router_id           | None                                 |
	| status              | DOWN                                 |
	| updated_at          | 2018-04-18T13:35:49Z                 |
	+---------------------+--------------------------------------+	
You can see that it's reserved **192.168.122.200** for our project, although it's not attached to an instance *yet* and therefore is not much use. Next, we can assign our claimed IP address to an instance:

	$ openstack server add floating ip my_vm 192.168.122.200	
> **NOTE:** If the command is successful it has no output, and your IP address may vary from the one displayed above - use the IP address that the create command allocated to you.

You can now verify that the IP address was assigned to your node with the following:

	$ openstack server list
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| ID                                   | Name  | Status | Networks                             | Image | Flavor  |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| 5d24c6de-2d4d-42d6-bcd0-52dd2770de69 | my_vm | ACTIVE | internal=172.16.1.7, 192.168.122.200 | rhel7 | m1.labs |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	
> **NOTE**: It may take a while for the floating IP to show up here, so keep trying the above command. 

For those of you that may have already tried to ping or SSH into the node using this IP address may be confused as to why this is not working. By default, OpenStack's security group rules will prevent **all** inbound access, so we'll need to open these up before we confirm that our instance is working properly.

### **OpenStack Security Groups**

By default, OpenStack Security Groups prevent any inbound access to instances, including ICMP/ping. Therefore, we have to edit the security group policy to ensure that the firewall is opened up for us.

Let's add two rules, firstly for all instances to have ICMP and SSH access. By default, Neutron ships with a 'default' security group, it's possible to create new groups and assign custom rules to these groups and then assign these groups to individual servers. For this lab, we'll just configure the default group. The problem is that there are multiple default groups, and as an administrator you can see them all, noting that they're project-specific. We need to first connect the **admin** project to the correct security group, first by getting the correct project ID:

	$ export MY_PROJECT=$(openstack project list | awk '$4 == "admin" {print $2};')
	$ export SEC_GROUP_ID=$(openstack security group list | grep $MY_PROJECT | awk '{print $2;}')

This should now show you the correct security group ID:

	$ echo $SEC_GROUP_ID
	2641d918-579b-4acd-8fbf-894f4e7be241 

	$ openstack security group list -c ID -c Project
	+--------------------------------------+----------------------------------+
	| ID                                   | Project                          |
	+--------------------------------------+----------------------------------+
	| 2641d918-579b-4acd-8fbf-894f4e7be241 | c14b205e428e43319fe43fb0396bd092 |
	| 9d1ac5cf-52da-4de7-a082-f10e7db88c71 | b86d79c73e604feab8ced0a46fe9738b |
	| bf70f47e-4c58-4372-81e2-5d38e14e3ec6 |                                  |
	+--------------------------------------+----------------------------------+

So, let's look at enabling ICMP within the default group, using the security group ID as the unique identifier for our '**default**' group within the admin project that we're using (and that our instance is booted onto):

	$ openstack security group rule create --proto icmp $SEC_GROUP_ID
	+-------------------+--------------------------------------+
	| Field             | Value                                |
	+-------------------+--------------------------------------+
	| created_at        | 2018-04-18T13:59:15Z                 |
	| description       |                                      |
	| direction         | ingress                              |
	| ether_type        | IPv4                                 |
	| id                | 8671ae28-b52a-4afa-b02b-2a632d55daf6 |
	| name              | None                                 |
	| port_range_max    | None                                 |
	| port_range_min    | None                                 |
	| project_id        | c14b205e428e43319fe43fb0396bd092     |
	| protocol          | icmp                                 |
	| remote_group_id   | None                                 |
	| remote_ip_prefix  | 0.0.0.0/0                            |
	| revision_number   | 0                                    |
	| security_group_id | 2641d918-579b-4acd-8fbf-894f4e7be241 |
	| updated_at        | 2018-04-18T13:59:15Z                 |
	+-------------------+--------------------------------------+

Within a few seconds (for the hypervisor to pick up the changes) you should be able to ping your floating IP:

	$ $ ping -c4 192.168.122.200
	PING 192.168.122.200 (192.168.122.200) 56(84) bytes of data.
	64 bytes from 192.168.122.200: icmp_seq=1 ttl=64 time=1.67 ms
	64 bytes from 192.168.122.200: icmp_seq=2 ttl=64 time=0.857 ms
	64 bytes from 192.168.122.200: icmp_seq=3 ttl=64 time=0.802 ms
	(...)

We can ping, but we can't SSH yet, as that's still not allowed by default. Let's try adding another rule, to allow SSH access for all instances in the 'default' group:

	$ openstack security group rule create --proto tcp --dst-port 22:22 $SEC_GROUP_ID
	+-------------------+--------------------------------------+
	| Field             | Value                                |
	+-------------------+--------------------------------------+
	| created_at        | 2018-04-18T14:00:36Z                 |
	| description       |                                      |
	| direction         | ingress                              |
	| ether_type        | IPv4                                 |
	| id                | 7e514f09-780c-4a70-97bd-14ecf24fb529 |
	| name              | None                                 |
	| port_range_max    | 22                                   |
	| port_range_min    | 22                                   |
	| project_id        | c14b205e428e43319fe43fb0396bd092     |
	| protocol          | tcp                                  |
	| remote_group_id   | None                                 |
	| remote_ip_prefix  | 0.0.0.0/0                            |
	| revision_number   | 0                                    |
	| security_group_id | 2641d918-579b-4acd-8fbf-894f4e7be241 |
	| updated_at        | 2018-04-18T14:00:36Z                 |
	+-------------------+--------------------------------------+

Let's quickly verify connectivity.. (The root password is **'redhat'**)

	$ ssh root@192.168.122.200
	The authenticity of host '192.168.122.200 (192.168.122.200)' can't be established.
	ECDSA key fingerprint is SHA256:maBYZmeAY6go1ynwTpoZ8o3kjWPgzuePT/6QDXe95rY.
	ECDSA key fingerprint is MD5:d3:f4:3d:dc:26:78:44:b2:0a:f1:ee:72:d8:a0:35:d1.
	Are you sure you want to continue connecting (yes/no)? yes
	Warning: Permanently added '192.168.122.200' (ECDSA) to the list of known hosts.
	Password:
	[root@my-vm ~]#
	
When you see the "**[root@my-vm ~]#**" prompt, you're connected into your instance successfully. Check the network configuration within the instance, note that it is *not* aware of the "192.168.122.200" address - this is being NAT'd by the L3 agent from the outside **external** network:

	[root@my-vm ~]# ip address show eth0
	2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc pfifo_fast state UP qlen 1000
	    link/ether fa:16:3e:8e:25:34 brd ff:ff:ff:ff:ff:ff
	    inet 172.16.1.7/24 brd 172.16.1.255 scope global dynamic eth0
	       valid_lft 84123sec preferred_lft 84123sec
	    inet6 fe80::f816:3eff:fe8e:2534/64 scope link
	       valid_lft forever preferred_lft forever

Press "Ctrl+d", or simply type "**exit**" to return to your OpenStack environment:

	[root@my-vm ~]# exit
	Connection to 192.168.122.200 closed.
	$

# **Viewing Console Output (VNC)**

It's critical to ensure that our virtual machine has started successfully. Nova provides us with two main ways of checking this without relying on networking being available - the console output, and the VNC console output. Using a web-browser, we can view the VNC console via an HTML5 viewer. We can get the URL via the following command:

	$ openstack console url show my_vm --novnc
	+-------+--------------------------------------------------------------------------------------+
	| Field | Value                                                                                |
	+-------+--------------------------------------------------------------------------------------+
	| type  | novnc                                                                                |
	| url   | http://129.146.150.142:6080/vnc_auto.html?token=1991f8b8-1de0-427e-a9e4-56effdc938aa |
	+-------+--------------------------------------------------------------------------------------+

**WARNING**: You may find that if you run the above command before the machine has booted, you'll receive the following error. Simply wait a minute or two for the instance to boot.

	$ openstack console url show my_vm --novnc
	ERROR: Instance not yet ready (HTTP 409) (Request-ID: req-da372916-10ba-4158-91a2-0cc10f4d083e)

You should be able to open this URL directly from your workstation and view the VM's VNC console; you can also login should you wish, the **root** password is **'redhat'**.

<img src="images/vnc-console.png" style="width: 1000px;"/>


# **Connectivity in Browser**

This section is not essential, but you may want to verify that you can access the Horizon dashboard for your environment; let's open up a web-browser and ensure that it shows all of our resources that we just built up. The URL for this can be found in the email that was sent to you by RHPDS, see the hyperlink in the middle that looks like this - [http://horizon-REPL.rhpds.opentlc.com/dashboard](http://horizon-REPL.rhpds.opentlc.com/dashboard) (where REPL is your GUID that was allocated to you when we started), once opened you should see the following:

<img src="images/horizon.png" style="width: 1000px;"/>

To login, you'll need to get the automatically generated password from the recently created **~/overcloudrc** file (your password will be different to the output shown below):

	undercloud$ egrep -i '(username|password)' ~/overcloudrc
	export OS_USERNAME=admin
	export OS_PASSWORD=jZJVX3D4xZaKeDJPfWs8CEBUB
	export OS_AUTH_TYPE=password

Make sure you select the '**Project**' tab at the top of the screen, as it should take you to the '**Identity**' tab by default as we're doing everything as the '**admin**' user. Feel free to play around with the OpenStack deployment if you've got plenty of time to spare. We can refer back to the web dashboard at a later step where required.


<br />
## Exploring and Validating the OpenDaylight Connectivity

So far, all we've tested is basic OpenStack functionality, albeit configuring some of the basic networking, so in theory OpenDaylight and its connectivity to Neutron is functioning as expected, but we haven't actually done anything OpenDaylight specific; we would have likely had the exact same functionality with any OpenStack Networking implementation.

Let's explore the current configuration to verify OpenDaylight integration, and how we can investigate how it all fits together. First let's ask OpenStack for a list of networking agents and services that are running within our environment:

	$ openstack network agent list
	+--------------------------------------+----------------+-------------------------------+-------------------+-------+-------+------------------------------+
	| ID                                   | Agent Type     | Host                          | Availability Zone | Alive | State | Binary                       |
	+--------------------------------------+----------------+-------------------------------+-------------------+-------+-------+------------------------------+
	| 1f9dbb3d-17fc-45dd-9a45-861bdef48c6f | DHCP agent     | summit-networker.localdomain  | nova              | :-)   | UP    | neutron-dhcp-agent           |
	| 23e8b46a-bce0-4a94-a8ba-277aa60ed971 | ODL L2         | summit-networker.localdomain  | None              | :-)   | UP    | neutron-odlagent-portbinding |
	| 4350636f-26f6-42b0-a942-640939a48972 | Metadata agent | summit-networker.localdomain  | None              | :-)   | UP    | neutron-metadata-agent       |
	| b4238dec-1f8d-4b7b-84bd-ed5ffbecee79 | ODL L2         | summit-controller.localdomain | None              | :-)   | UP    | neutron-odlagent-portbinding |
	| b4c9fa0b-8c30-46b0-accf-22aff4f2a145 | ODL L2         | summit-compute1.localdomain   | None              | :-)   | UP    | neutron-odlagent-portbinding |
	| f0730552-2bcc-45e9-8ac0-6248359b8cca | ODL L2         | summit-compute2.localdomain   | None              | :-)   | UP    | neutron-odlagent-portbinding |
	+--------------------------------------+----------------+-------------------------------+-------------------+-------+-------+------------------------------+

What you'll see is that we have three systems that have networking functions, the '**summit-networker**' machine, and the '**summit-computeX**' machines; the **controller** is shown above as it also has ODL enabled by default, which it doesn't actually need if you're running dedicated networker nodes like we are.

First thing to note is that the **networker** provides some additional capabilities - DHCP and Metadata. On each of these nodes you'll also see '**ODL-L2**' running, but how is this possible when earlier we stated that OpenDaylight removes the need for any agents other than DHCP/Metadata, and directly programs Open vSwitch? The answer is these are not real agents; they are in fact called pseudo-agents. The **networking-odl** driver is reading configuration provided by OpenDaylight about each OpenStack node, and then entering it into the Neutron Agent DB as an agent for ML2. This information is used to select the node for ML2 port binding (binding a Neutron port to a physical host). But where does OpenDaylight get this configuration from? This is a good time to take a look at the Open vSwitch configuration on a node:

    $ ssh root@summit-compute1 ovs-vsctl list open_vswitch
	_uuid               : 9aa34a4e-efdf-4b9f-98dc-089b71506c97
	bridges             : [2180eb91-09d5-44dd-b084-ad042340b15e, ac6193d6-f3b5-4ea5-9a97-9445a2beac7e]
	cur_cfg             : 23
	datapath_types      : [netdev, system]
	db_version          : "7.15.0"
	external_ids        : {hostname="summit-compute1.localdomain", "odl_os_hostconfig_config_odl_l2"="{  \"supported_vnic_types\": [{    \"vnic_type\": \"normal\",    \"vif_type\": \"ovs\",    \"vif_details\": {}  }],  \"allowed_network_types\": [\"local\",\"vlan\",\"vxlan\",\"gre\"],  \"bridge_mappings\": {\"datacentre\":\"br-ex\"}}", odl_os_hostconfig_hostid="summit-compute1.localdomain", system-id="62467b46-8d0c-4124-803f-640e77034668"}
	iface_types         : [geneve, gre, internal, lisp, patch, stt, system, tap, vxlan]
	manager_options     : [ace09c13-2d72-4372-8394-548b2575f6ed, d1d2e151-b7f1-4900-ab10-61391cf34ed8]
	next_cfg            : 23
	other_config        : {local_ip="172.17.2.16", provider_mappings="datacentre:br-ex"}
	ovs_version         : "2.7.3"
	ssl                 : []
	statistics          : {}
	system_type         : rhel
	system_version      : "7.4"

Let's examine the above output.  The **'other_config'** section lists **'local_ip'**, this is source IP that VXLAN based network overlay will use as the source IP for its tunnel. The **'provider_mappings'** works the same way as Neutron bridge mappings and provides the mapping for logical to physical networks on this host.

Additionally look at **'external_ids'**.  The **'odl\_os\_hostconfig\_config\_odl\_l2'** section contains more information (some duplicate for legacy reasons) which indicates what kind of ports this node supports (vhostuser DPDK, or normal OVS VIF ports), the allowed network types on this node, etc. This information is read by OpenDaylight using the OVSDB protocol and eventually propagated by networking-odl into the Neutron Agent DB.
Note, in Red Hat OpenStack Platform 12 when a port binding event occurs, networking-odl automatically sets the port to Active state.  Typically in port binding a port should get created in Neutron then a ML2 port binding will execute a bind call to the driver (in this case ODL) who will verify the port is bound on the correct host. In Red Hat OpenStack Platform 13, there is support for a new web-socket based connection which runs over port 8185 between OpenDaylight and Neutron.  This allows OpenDaylight to update the port state to **Active** once it sees the port created on the compute node and bound correctly to the virtual Neutron network.

The **ODL-L2** agent configures the connections for OVSDB and OpenFlow, and are configured within Open vSwitch; each is described below:

| Connection Type  | Host/Port Number  | Description |
|---|---|---|
| ODL Southbound OVSDB |  odl-controller:6640 | OVSDB is used to manage switch configuration.  While OpenFlow (see below) allows us to configure the **datapath** table of a switch, we also need a protocol to allow configuration of ports, bridges and other settings on the switch.  OVSDB gives us that ability to do switch configuration management, and this connection is used by OpenDaylight to program the local Open vSwitch.
| ODL Southbound OpenFlow | odl-controller:6653 | OpenFlow is a protocol that was designed with SDN in mind.  In the network data path it is represented as a list of rules that determine how packets are forwarded.  The concept of a rule is broken down into **'match'** criteria (to match on a packet) and then an **'action'** (what to do with the packet). The control plane side of OpenFlow allows a centralised OpenFlow controller (such as ODL) to push these rules into remote switches and control datapath forwarding throughout a network from a centralised controller. This connection is how OpenDaylight configures the flows of the local Open vSwitch.
| Local OVSDB Connection  | localhost:6639 | This is configured by Director as the local port to run OVSDB Server on. By default OVSDB Server runs on port 6640, but since that port is taken by ODL on the control nodes, we reconfigure the local OVSDB server to use 6639. This port is what Neutron's **DHCP agent** uses to configure OVS with DHCP ports. Note that all nodes are configured with this, regardless of whether the DHCP agent is started.

If we look at the Open vSwitch configuration on our **summit-networker** machine we can see how all of this has been configured. To do this, we'll need to briefly ssh to that machine and output the entire configuration; this is annotated below:

	$ ssh root@summit-networker ovs-vsctl show
    60ccb669-1eb9-4097-b915-8f02ac6f46e3
    Manager "ptcp:6639:127.0.0.1"                      <--- Local OVSDB Server listener (DHCP)
    Manager "tcp:172.17.1.16:6640"                     <--- ODL Southbound OVSDB
        is_connected: true
    Bridge br-ex                                       <--- the br-ex OVS bridge
        fail_mode: standalone
        Port br-ex-int-patch
            Interface br-ex-int-patch
                type: patch
                options: {peer=br-ex-patch}
        Port "vlan101"                                 <--- additional VLAN interfaces
            tag: 101                                        for other OpenStack traffic
            Interface "vlan101"                             e.g. storage/internal API networks
                type: internal
        Port "eth1"                                    <--- physical eth1 nic
            Interface "eth1"
        Port "vlan201"
            tag: 201
            Interface "vlan201"
                type: internal
        Port br-ex
            Interface br-ex
                type: internal
    Bridge br-int                                      <--- integration bridge
        Controller "tcp:172.17.1.16:6653"              <--- ODL Southbound OpenFlow
            is_connected: true
        fail_mode: secure
        Port br-int
            Interface br-int
                type: internal
        Port "tape42503b6-de"                          <--- DHCP agent TAP device
            Interface "tape42503b6-de"
                type: internal
        Port "tun86ef5582407"                          <--- VXLAN tunnel endpoint
            Interface "tun86ef5582407"                      to a compute node
                type: vxlan
                options: {key=flow, local_ip="172.17.2.20", remote_ip="172.17.2.19"} <--- local_ip from OVS configuration
        Port br-ex-patch
            Interface br-ex-patch
                type: patch
                options: {peer=br-ex-int-patch}
    ovs_version: "2.7.3"

> **NOTE:** We don't need to specify a password to execute remote commands on the networker node - we've already pre-populated the overcloud nodes with a secure shell key for convenience, but you may have to accept the key.

> **NOTE**: Tunnel devices are created when required rather than establishing a full mesh network, hence why the output above only shows a single connection to one of our compute nodes, and not the other one. Recall that we only have **one** instance running, and it would have been scheduled upon one of those nodes, not both.

In addition, you'll also note that bridge "br-ex" is already attached to a real world physical network interface, **"eth1"**. The important thing to understand here is that Neutron only understands **logical** "physical network" names - these logical network names are translated into real underlying networks via a network bridge by the plugin that we're using (in our case, OpenDaylight). To understand how this works, we need to look at the defined logical networks that OVS/OpenDaylight is exposing to Neutron via the '**bridge_mappings**' extension. Select the '**id**' for the ODL-L2 pseudo agent on '**summit-networker**' in the following command, and it will show you the current bridge mappings:

	$ openstack network agent show 23e8b46a-bce0-4a94-a8ba-277aa60ed971 -f json | grep -A2 bridge_mappings
		"bridge_mappings": {
			"datacentre": "br-ex"
		}

What this shows it that we have a physical network name of "**datacentre**", which if used, would tell Neutron to route all external traffic for that network onto the Open vSwitch bridge "**br-ex**". Recall that when we created our external network, we defined it as using the **'datacentre'** logical network, hence the mapping here. To re-iterate, when we defined our external network with the Neutron logical name **"datacentre"**, the traffic utilises **"eth1"** as a physical network via the Open vSwitch bridge **"br-ex"**. 

Let's now explore how Neutron is communicating with OpenDaylight, showing that through the ML2 interface Neutron networks are represented within OpenDaylight and are then implemented within the Open vSwitch configuration on the relevant nodes. For this, we need to query the OpenDaylight REST API and ask it for a list of networks. By default, the OpenDaylight controller as deployed by TripleO is not accessible from the external or public network, but is available on both the control plane network and the internal API network within the overcloud. These endpoints, like many other OpenStack services, are maintained by the HA Proxy configuration on the overcloud controller.

Let's first get the control plane virtual IP address so we know how to contact our OpenDaylight controller, for this we can request a list from the undercloud, making sure that you've sourced your **stackrc** (undercloud) environment file, **not** the overcloud one:

	undercloud$ source ~/stackrc
	undercloud$ openstack stack output show overcloud VipMap
	+--------------+------------------------------------------------------------------------+
	| Field        | Value                                                                  |
	+--------------+------------------------------------------------------------------------+
	| description  | Mapping of each network to VIP addresses. Also includes the Redis VIP. |
	| output_key   | VipMap                                                                 |
	| output_value | {                                                                      |
	|              |   "storage": "172.17.3.13",                                            |
	|              |   "management_uri": "",                                                |
	|              |   "internal_api_subnet": "",                                           |
	|              |   "ctlplane": "172.16.0.23",                                           |
	|              |   "storage_uri": "172.17.3.13",                                        |
	|              |   "management": "",                                                    |
	|              |   "management_subnet": "",                                             |
	|              |   "redis": "172.17.1.10",                                              |
	|              |   "storage_subnet": "",                                                |
	|              |   "storage_mgmt_uri": "172.17.4.19",                                   |
	|              |   "tenant_uri": "",                                                    |
	|              |   "external": "192.168.122.100",                                       |
	|              |   "storage_mgmt": "172.17.4.19",                                       |
	|              |   "tenant": "",                                                        |
	|              |   "tenant_subnet": "",                                                 |
	|              |   "ctlplane_uri": "172.16.0.23",                                       |
	|              |   "external_subnet": "",                                               |
	|              |   "storage_mgmt_subnet": "",                                           |
	|              |   "internal_api": "172.17.1.15",                                       |
	|              |   "internal_api_uri": "172.17.1.15",                                   |
	|              |   "external_uri": "192.168.122.100",                                   |
	|              |   "ctlplane_subnet": "172.16.0.23/24"                                  |
	|              | }                                                                      |
	+--------------+------------------------------------------------------------------------+

In my environment, it's been allocated the IP address **172.16.0.23** (**ctlplane**), but your environment will very likely be different. We could have fixed this during the deployment, but we've opted to minimise the complexity of the templates. Let's export this as an environment variable, making sure that you use the IP address that was selected for your environment.

	undercloud$ export CTLPLANE=172.16.0.23

Next, let's call the REST API for OpenDaylight and ask it for a list of Neutron networks, noting that the default username and password is '**admin/admin**':

	undercloud$ curl -u admin:admin http://$CTLPLANE:8081/controller/nb/v2/neutron/networks
	{
	   "networks" : [ {
	      "id" : "642f4496-7773-4d17-8f82-52fe8efc2a62",
	      "tenant_id" : "c14b205e428e43319fe43fb0396bd092",
	      "project_id" : "c14b205e428e43319fe43fb0396bd092",
	      "revision_number" : 3,
	      "name" : "external",                                        <-- our 'external' network
	      "admin_state_up" : true,
	      "status" : "ACTIVE",
	      "shared" : false,
	      "router:external" : true,
	      "provider:network_type" : "flat",
	      "provider:physical_network" : "datacentre",
	      "segments" : [ ]
	   }, {
	      "id" : "bd8db3a8-2b30-4083-a8b3-b3fd46401142",
	      "tenant_id" : "bd8db3a82b304083a8b3b3fd46401142",
	      "project_id" : "bd8db3a8-2b30-4083-a8b3-b3fd46401142",
	      "name" : "Sync Canary Network",
	      "admin_state_up" : false,
	      "status" : "ACTIVE",
	      "shared" : false,
	      "router:external" : false,
	      "provider:network_type" : "flat",
	      "segments" : [ ]
	   }, {
	      "id" : "22f9a9cd-1da0-4c6a-a815-d25da07aa18d",
	      "tenant_id" : "c14b205e428e43319fe43fb0396bd092",
	      "project_id" : "c14b205e428e43319fe43fb0396bd092",
	      "revision_number" : 2,
	      "name" : "internal",                                       <-- our 'internal' network
	      "admin_state_up" : true,
	      "status" : "ACTIVE",
	      "shared" : false,
	      "router:external" : false,
	      "provider:network_type" : "vxlan",
	      "provider:segmentation_id" : "30",
	      "segments" : [ ]
	   } ]
	}

You should see three networks listed, the VXLAN-based internal tenant network called "**internal**" and the external network called "**external**", both of which we created earlier, that's used for floating IP access and external routing (noting that it's associated to the flat "datacentre" physical network), and you'll note that the ID's also match up (remember to source the ~/overcloudrc file again):

	$ source ~/overcloudrc
	$ openstack network list -c ID -c Name
	+--------------------------------------+----------+
	| ID                                   | Name     |
	+--------------------------------------+----------+
	| 22f9a9cd-1da0-4c6a-a815-d25da07aa18d | internal |
	| 642f4496-7773-4d17-8f82-52fe8efc2a62 | external |
	+--------------------------------------+----------+

And then a third network called the "**Sync Canary Network**". The canary network is a dummy network used as a placeholder, created by the ML2 OpenDaylight service as a mechanism to check whether we're in a consistent state between OpenDaylight and Neutron. More specifically, if this network has been removed, it's assumed that the OpenDaylight network database has been dropped, and it will trigger a full re-sync between Neutron and OpenDaylight to ensure consistency. The fact that these networks are known by OpenDaylight, aside from the fact that we've validated that basic networking is working, prove that Neutron is able to successfully communicate with the OpenDayight SDN controller.

### OpenDaylight Under the Covers

In this section we're going to take a look at the OpenDaylight controller itself; where it runs, how you can connect into it, query it for information, and what functions are available. In our model the OpenDaylight controller runs on our dedicated networker machine, so let's hop over to it:

	$ ssh root@summit-networker

> **NOTE**: If the following commands are prefixed with "**networker#**" it signifies that the commands are to be executed on the summit-networker machine and not the undercloud.

Now we can connect to the OpenDaylight management command-line console, which runs over secure shell. This listens on port **8101** on the nodes running an instance of the OpenDaylight controller:

	networker# netstat -tunpl | grep 8101
	tcp6       0      0 :::8101                 :::*           LISTEN      2190/java

But it's actually running in a Docker container (in OSP12+ the vast majority of OpenStack services are now containerised as opposed to installed as standard RPM's and managed through systemd) but we'll explore this a bit further later on:

	networker# docker ps -a | grep opendaylight
	60e5bba8875e    172.16.0.1:8787/rhosp12/openstack-opendaylight:12.0-20180319.1   "kolla_start"       3 weeks ago         Up 10 hours            opendaylight_api

Let's connect into this console from the networker node itself (password is '**karaf**'):

	networker# ssh karaf@localhost -p 8101
	Password authentication
	Password:
	
	    ________                       ________                .__  .__       .__     __
	    \_____  \ ______   ____   ____ \______ \ _____  ___.__.|  | |__| ____ |  |___/  |_
	     /   |   \\____ \_/ __ \ /    \ |    |  \\__  \<   |  ||  | |  |/ ___\|  |  \   __\
	    /    |    \  |_> >  ___/|   |  \|    `   \/ __ \\___  ||  |_|  / /_/  >   Y  \  |
	    \_______  /   __/ \___  >___|  /_______  (____  / ____||____/__\___  /|___|  /__|
	            \/|__|        \/     \/        \/     \/\/            /_____/      \/
	
	
	Hit '<tab>' for a list of available commands
	and '[cmd] --help' for help on a specific command.
	Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown OpenDaylight.
	
	opendaylight-user@root>

Here, we can run some additional commands to inspect the current networking status. Let's start with seeing whether we can match the Open vSwitch configuration setup for our VXLAN-based tunnel network endpoints to the structure that OpenDaylight knows about:

	opendaylight$ vxlan:show
	Name                                              Description
	Local IP                 Remote IP                Gateway IP         AdmState
	OpState                  Parent                   Tag
	--------------------------------------------------------------------------------
	tund35bcb4915e                                    VXLAN Trunk Interface
	172.17.2.19              172.17.2.20              0.0.0.0            ENABLED
	UP                       215658939848679/tund35bcb4915e 7
	
	tun86ef5582407                                    VXLAN Trunk Interface
	172.17.2.20              172.17.2.19              0.0.0.0            ENABLED
	UP                       97146301947967/tun86ef5582407 8


As you can see, this matches the output shown in Open vSwitch, remembering that the only nodes that have networking connectivity at the moment are the networker to the first compute node that's running our instance. If we were to launch an additional instance, it's likely that this would be extended to accommodate connectivity to the second compute node.

Next, another basic command we can use is to list the NAPT switches/routers that are used to perform SNAT functionality for instances that do not have floating IP's, noting that the router ID will match the Neutron router ID that we created earlier in the lab:

	opendaylight$ odl:display-napt-switches
	 Router Id                             Datapath Node Id      Managment Ip Address
	-------------------------------------------------------------------------------------------
	 fa88e28a-34c4-4b8e-8c33-b67e54c4c9c0  233871321246373       172.17.2.16

The IP address of the NAPT switch, **172.17.2.16**, is the IP address of a compute node where OpenDaylight has deployed a virtual router to perform the SNAT functionality, allowing us to have distributed routing and is using **conntrack** to manage it. Note that it's not currently possible to have highly-available SNAT routers with the OpenDaylight integration.

Let's quit out of our OpenDaylight controller and return to our undercloud machine for now:

	opendaylight$ (Ctrl-D, or 'logout')
	Connection to localhost closed.
	networker# exit
	Connection to 172.16.0.25 closed.

Many of the OpenStack-specific implementations don't have an equivalent Karaf console command that we can use, and therefore we sometimes have to use the REST API like we did to query the networks earlier. Let's check the floating IP mappings that are being used as an example:

	undercloud$ $ curl -s -u admin:admin \
		http://$CTLPLANE:8081/restconf/config/odl-nat:floating-ip-info | \
		python -m json.tool
		
	{
	    "floating-ip-info": {
	        "router-ports": [
	            {
	                "external-network-id": "642f4496-7773-4d17-8f82-52fe8efc2a62",
	                "ports": [
	                    {
	                        "internal-to-external-port-map": [
	                            {
	                                "external-id": "4965c561-c97e-48c0-b89e-7f9064d399f6",
	                                "external-ip": "192.168.122.200",
	                                "internal-ip": "172.16.1.7"
	                            }
	                        ],
	                        "port-name": "bcc0f8b1-9450-4e7d-b19f-0d0d0e1942f4"
	                    }
	                ],
	                "router-id": "fa88e28a-34c4-4b8e-8c33-b67e54c4c9c0"
	            }
	        ]
	    }
	}

Here we have our floating IP matching our instance IP, as demonstrated through the Neutron API too:

	$ openstack floating ip show 192.168.122.200
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-18T13:35:49Z                 |
	| description         |                                      |
	| fixed_ip_address    | 172.16.1.7                           |       <--- "internal-ip"
	| floating_ip_address | 192.168.122.200                      |       <--- "external-ip"
	| floating_network_id | 642f4496-7773-4d17-8f82-52fe8efc2a62 |
	| id                  | 4965c561-c97e-48c0-b89e-7f9064d399f6 |
	| name                | 192.168.122.200                      |
	| port_id             | bcc0f8b1-9450-4e7d-b19f-0d0d0e1942f4 |
	| project_id          | c14b205e428e43319fe43fb0396bd092     |
	| revision_number     | 1                                    |
	| router_id           | fa88e28a-34c4-4b8e-8c33-b67e54c4c9c0 |
	| status              | ACTIVE                               |
	| updated_at          | 2018-04-18T13:36:33Z                 |
	+---------------------+--------------------------------------+

As you can see, there are a number of different ways of interacting with OpenDaylight; Neutron can provide a direct route to a lot of the data, but this gets translated into OpenDaylight specific constructs by the ML2 OpenDaylight implementation. It's then the responsibility of OpenDaylight to program the local Open vSwitch implementations at each node that requires it.

# Connecting to the OpenDaylight Container

Red Hat OpenStack Platform, as per the default configuration in OSP12 and beyond, utilises containers for the vast majority of the OpenStack services. OpenDaylight is no exception here, and as such, on our dedicated networker node the OpenDaylight SDN Controller runs within a docker container. If you ever need to access the container where OpenDaylight is running, e.g. if it's not running correctly and the API access is not sufficient, then there are a number of different ways you can interact with it.

Firstly, you can query the logs that the container is producing. For this you need to make sure that you're connected to the host that's running our OpenDaylight controller:

	undercloud$ ssh root@summit-networker
	networker# # docker ps -a
	CONTAINER ID        IMAGE                                                            COMMAND             CREATED             STATUS                    PORTS               NAMES
	34e2e1a71f09        172.16.0.1:8787/rhosp12/openstack-cron:12.0-20171129.1           "kolla_start"       15 hours ago        Up 15 hours                                   logrotate_crond
	3e94f63b3068        172.16.0.1:8787/rhosp12/openstack-opendaylight:12.0-20171129.1   "kolla_start"       15 hours ago        Up 15 hours                      opendaylight_api

Here you'll see that this host is only running two containers - recall that this is actually a very simple role that just runs OpenDaylight in a container; with OSP12, all other OpenStack networking services are not yet containerised, primarily to allow third party vendors that provide networking solutions can have some time to move over to the docker packaging format. To verify, you can demonstrate that Neutron's DHCP service is still managed by systemd:

	networker# systemctl status neutron-dhcp-agent.service
	● neutron-dhcp-agent.service - OpenStack Neutron DHCP Agent
	   Loaded: loaded (/usr/lib/systemd/system/neutron-dhcp-agent.service; enabled; vendor preset: disabled)
	   Active: active (running) since Wed 2018-04-18 12:21:15 UTC; 10h ago
	 Main PID: 24284 (neutron-dhcp-ag)
	   Memory: 102.9M
	   CGroup: /system.slice/neutron-dhcp-agent.service
	           ├─24284 /usr/bin/python2 /usr/bin/neutron-dhcp-agent --config-file /usr/share/neutron/neutron-dist.conf --config-file /etc/neutron/neutron.conf --config-file /etc/neutron/dhcp_agent...
	           ├─92414 sudo neutron-rootwrap-daemon /etc/neutron/rootwrap.conf
	           ├─92415 /usr/bin/python2 /usr/bin/neutron-rootwrap-daemon /etc/neutron/rootwrap.conf
	           ├─92486 dnsmasq --no-hosts --no-resolv --strict-order --except-interface=lo --pid-file=/var/lib/neutron/dhcp/1e357bee-87e5-4074-b87a-a418778b99e8/pid --dhcp-hostsfile=/var/lib/neutr...
	           └─92488 haproxy -f /var/lib/neutron/ns-metadata-proxy/1e357bee-87e5-4074-b87a-a418778b99e8.conf

Going back to the OpenDaylight container, to view the logs you simply need to use the 'docker logs' command, with the ID of the container provided from the list we got earlier:

	networker# docker logs 3e94f63b3068
	(...)

> **NOTE**: The above output is removed simply due to its verbosity. But this can be used to track down startup issues, or any issues that may be logged, helping you troubleshoot any problems.

Let's say that you get this far and you want to go a little deeper; it's possible to attach into a container to execute commands from within the container, interacting with all namespaces associated with that container:

	networker# docker exec -it 3e94f63b3068 /bin/bash
	tput: No value for $TERM and no -T specified
	tput: No value for $TERM and no -T specified
	container# export TERM=xterm

> **NOTE**: We export $TERM to help the console output properly, as without this you may find that viewing configuration files or log files is very difficult.

Now you can view the processes, for example, for that namespace, and have direct access to log files, configuration files, and anything that the container requires, as if it was running on the baremetal host:

	container# ps -ax
	  PID TTY      STAT   TIME COMMAND
      1 ?        	Ssl	   77:33 /usr/bin/java -Djava.security.properties=/opt/opendaylight/etc/odl.java.security -server -Xms128M -Xmx2048m -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass -XX:+H
	  45476 ?        Ss     0:00 /bin/bash
	  45530 ?        R+     0:00 ps -ax

It's just as easy to disconnect from the container when required:

	container# exit
	networker# exit
	Connection to 172.16.0.25 closed.
	undercloud$
