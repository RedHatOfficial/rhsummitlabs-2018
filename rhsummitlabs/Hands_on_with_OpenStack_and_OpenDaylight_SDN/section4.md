## **Exploration of TripleO Requirements for OpenDaylight**

In this section we're going to be looking at how we can utilise OSP director (through TripleO) to deploy an OpenStack overcloud with integrated OpenDaylight, and we'll look at the customisations available with the TripleO heat templates. We already have a pre-deployed environment available to us, but we'll remove this and go through all of the steps to configure and kick-off a new deployment. Unfortunately it's not likely that you'll see the deployment successfully complete given the time constraints of this lab, but at a very minimum you should be able to understand the flows and how the deployment is started.

Let's first remove/clean-up the existing deployment and reset our environment, remembering that we now have to use the undercloud and as such must source the **~/stackrc** file:

	$ source ~/stackrc

Now we can ask the undercloud to remove the existing overcloud stack:

	$ openstack stack delete overcloud
	Are you sure you want to delete this stack(s) [y/N]? yes

> **NOTE**: This process will take a few minutes so we'll let it process the removal in the background and move on.

<br />
Support for the deployment and integration of OpenDaylight has been available within TripleO for a number of releases, and is fully composable like all other OpenStack services, i.e. it can be deployed within any existing role, or a brand new role (e.g. a dedicated networker). Since OSP12, OpenStack services (with the exception of a few core components) are containerised, and are deployed as immutable containers across a set of hosts; OpenDaylight is no exception here, although TripleO currently supports the option of deploying it as a legacy service ("**puppet**" based) or as a container ("**docker**" based).

We're going to be using the containerised OpenDaylight service in our new deployment, which means that we do not need to install any software in the default RHEL image that OpenStack Ironic rolls out during the node deployment phase. The next few steps are going to walk you through the following tasks:

* Creating a custom (composable) role for the networking service, one that will take responsibility for running the OpenDaylight services, just like the dedicated networker node we had in the pre-deployed environment.<br /><br />
* Configuring the bare metal node configuration to enable role tagging - so each node gets deployed with the correct role as per the hardware specification it requires, including the dedicated networker.<br /><br />
* Investigating the "physical" networking topology and configuration for each role, and how OSP director configures the networking on our nodes accordingly.<br /><br />
* Understanding the options for OpenDaylight configuration through the TripleO Heat templates, and how this maps to the physical networking topology that we've applied.

Much of the OpenDaylight requirements are very similar to a standard OpenStack networking experience, and therefore the implementations and configuration options do not differ too much between the two.

### Creating a Dedicated Networker Role

Since OSP10 (based on OpenStack Newton), OSP director supports **composable roles** - this allows organisations to break down the legacy OpenStack roles, such as a controller, into individual services and build up custom roles to suit their requirements. This could be as simple as combining a compute and a storage role to create a hyperconverged configuration, or perhaps fragmenting the monolithic controller role to split out some of the services to enable a greater degree of scalability. Historically, all networking services were provided by the controllers; now we have a choice about where the networking services should operate. As part of this section we're going to demonstrate how we can utilise composable roles to choose how, and where, to deploy the OpenDaylight services.

We're going to be creating a dedicated networker role, in which all OpenStack networking related services, including the OpenDaylight controller, will reside. For us to understand how this works we must first explore the TripleO heat templates that dictate and control the role structure, and then we'll look to make some minor modifications to suit our desired configuration.

The templates that are fed into Heat are dynamically generated at deployment time through Jinja2 templates based on the roles that are defined by the administrator. If no custom role information is supplied it will use the default configuration residing at ***/usr/share/openstack-tripleo-heat-templates/roles_data.yaml***, which lists all of the individual OpenStack services (and supporting functions) that it needs to deploy for each of the listed roles. By either modifying this file (not recommended) or supplying a customised role information file at deployment time, one can dynamically define new roles and specify the services in which it will be associated with.

Inside of this file it's possible to both create new roles dynamically, or simply move services between the existing role structures that are already well understood. In our environment we're going to be pulling the networking functions (including OpenDaylight) out of the controller and placing them into a new role called "**Networker**". For this, we can use TripleO to generate some of the role definitions for us. Let's first make a directory that we'll use to hold all of our customised templates:

	$ mkdir -p ~/templates/
	$ cd ~/templates/

Next, let's generate a new custom roles file, specifying the three main roles that we want to cover, i.e. a "Controller", a "Compute", and a "Networker"; noting that TripleO already has the construct of a dedicated networker node available, so it knows which services to allocated into this role:

	$ openstack overcloud roles generate \
		Controller Compute Networker -o ~/templates/lab_roles.yaml

So how does this differ from the standard configuration? Well, firstly, the TripleO concept of a networker node is very slightly different from what we're trying to achieve with our deployment. Namely, the default TripleO networker role still assumes that we want the OpenDaylight SDN controller itself to reside within the **controller** role, but for the purposes of this exercise we're going to move this to our dedicated **networker** role, along with all Neutron based services except the Neutron API server, which is best left on the controller.

For this, we need to move the service called **"OS::TripleO::Services::OpenDaylightApi"** from the controller, along with all Neutron agents to the networker role, so OSP director knows that it needs to put it them on the networker and not the controller nodes. Let's make this change, first by removing the instance of **'OpenDaylightApi'** from the controller role, noting that as there's only one we can use a simple sed command:

	$ sed -i /OpenDaylightApi/d ~/templates/lab_roles.yaml

Now we can add that service to the end of the file as the Networker role is at the bottom of the custom roles file and it's a list of services, noting that whitespace is very important here to ensure the hierarchy of the yaml formatted file:

	undercloud$ echo "    - OS::TripleO::Services::OpenDaylightApi" \
		>> ~/templates/lab_roles.yaml
	undercloud$ echo "    - OS::TripleO::Services::NeutronCorePlugin" \
		>> ~/templates/lab_roles.yaml

Now, we need to remove all Neutron services/agents/daemons (with the exception of Neutron API and the Neutron Core Plugin service, something used to configure Neutron's API server to talk to the chosen plugin) from the controller. TripleO, via the roles file generation tool, has already ensured that the networker node has these roles already satisfied.

Next, edit the file, and remove all lines containing "**OS::TripleO::Services:Neutron**" from the **Controller** block.

You can use your favourite text editor for this...

	$ vi ~/templates/lab_roles.yaml

You'll need to **remove** these services (by simply deleting the line in the file):

* **OS::TripleO::Services::NeutronBgpVpnApi**
* **OS::TripleO::Services::NeutronDhcpAgent**
* **OS::TripleO::Services::NeutronL2gwAgent**
* **OS::TripleO::Services::NeutronL2gwApi**
* **OS::TripleO::Services::NeutronL3Agent**
* **OS::TripleO::Services::NeutronLbaasv2Agent**
* **OS::TripleO::Services::NeutronLinuxbridgeAgent**
* **OS::TripleO::Services::NeutronMetadataAgent**
* **OS::TripleO::Services::NeutronML2FujitsuCfab**
* **OS::TripleO::Services::NeutronML2FujitsuFossw**
* **OS::TripleO::Services::NeutronOvsAgent**
* **OS::TripleO::Services::NeutronVppAgent**

But **don't** remove the following services:

* **OS::TripleO::Services::NeutronApi**
* **OS::TripleO::Services::NeutronCorePlugin**

If you're not comfortable modifying these files yourself, and just want to proceed past this, you can copy the pre-built file that we've made available for you, note that you should **only** execute the following command if you just want the easy ride here:

	$ cp ~/labs/odl/templates/custom_roles.yaml ~/templates/lab_roles.yaml

What we're saying here is that the following services have been moved from the controller role to our new networker role:

* All of Neutron's standard agents (DHCP, L3, Metadata, LBaaS, OVS etc.)
* OpenDaylight's API Service

To integrate OpenDaylight, the TripleO engineers had to add two TripleO services to the list of available :

* The **OpenDaylightApi** service, which is the TripleO service that is responsible for deploying and operating the OpenDaylight SDN controller itself, this is what we've pulled from the default controller role and placed into the custom **networker** role. OpenDaylight offers High Availability (HA) by scaling the number of **OpenDaylightApi** service instances by at least three.  This means that by default, scaling the number of networker nodes to three or more will automatically enable HA.<br /><br />
* The **OpenDaylightOvs** service, which is responsible for configuring Open vSwitch on each of the compute nodes and the networker node itself to properly communicate with OpenDaylight, i.e. take its instructions from OpenDaylight. Note that as networker node will be providing additional Neutron capabilities such as DHCP, the local Open vSwitch configuration also needs to be configured by OpenDaylight.

For the setup that we're going to deploy in upcoming lab sections, we've got a partially pre-built set of templates that are going to be used. We'll be adding to these skeleton files with some additional configuration over the next few lab sections, but we should grab these templates now, making sure we execute this in our new ~/templates directory:
	
	$ cd ~/templates/
	$ cp -rf ~/labs/odl/templates/* ~/templates/

There's one main environment file that has been partially filled out to help us define the lab environment, specifically setting things like the physical networking configuration (to conincide with the physical setup - more on this later), and various other main parameters that we can look into; this is set at **~/templates/config.yaml**.

<br />
### Node to Role Tagging

OpenStack Nova uses two main metrics to determine whether an Ironic node is suitable for a given role (e.g. controller, compute node, networker), both of which are encapsulated into a flavor. The flavor defines the required size of the machine (e.g. number of CPU's, memory capacity, and available disk space) and also has a set of properties that a node must also satisfy. For example, a node may have the required physical capacity, but it may not satisfy additional properties, such as having SR/IOV capable network interfaces. In OSP director we use the additional properties to define what we call a **profile**, it's the profile that we assign to a given node so that Nova doesn't just have to rely on the minimum hardware specification, we can give it a hint to say these machines are definitely controllers, for example.

Whilst it's possible to not use profiles at all and have Nova just rely on hardware specifications, or just choose nodes at random, profiles allow us more predictability when it comes to knowing which nodes will become certain roles, and for many organisations it's exactly what they need. In this lab section we're going to assign some profiles to our nodes, as our nodes have differing hardware specification and we want to make sure the right nodes are chosen for a given role.

As we just alluded to, Nova relies on a flavor to determine which role to assign to a given node. These flavors are defined by a required hardware specification and a set of properties that align to a profile that each node is assigned. For example, if we look at the available flavors, and the **controller** flavor example:

	$ openstack flavor list
	+--------------------------------------+---------------+------+------+-----------+-------+-----------+
	| ID                                   | Name          |  RAM | Disk | Ephemeral | VCPUs | Is Public |
	+--------------------------------------+---------------+------+------+-----------+-------+-----------+
	| 2190e8a9-e7c8-4829-a45c-66d67b214786 | block-storage | 4096 |   40 |         0 |     1 | True      |
	| 54a81574-48d7-485b-8918-67e729469e08 | baremetal     | 4096 |   40 |         0 |     1 | True      |
	| 75fe95eb-86a2-4c1a-ac8b-e6352b53ee4b | networker     | 4096 |   40 |         0 |     1 | True      |
	| 81199b17-dcd4-4ca5-92b0-2287c85af020 | control       | 4096 |   40 |         0 |     1 | True      |
	| 8759f6de-7d24-4f99-86dc-5e93a22306dc | ceph-storage  | 4096 |   40 |         0 |     1 | True      |
	| 95df47f2-10f0-41a0-b577-02ab48f51400 | compute       | 4096 |   40 |         0 |     1 | True      |
	| becf2ea9-af96-405b-aa14-d4c858715181 | swift-storage | 4096 |   40 |         0 |     1 | True      |
	+--------------------------------------+---------------+------+------+-----------+-------+-----------+
	
	$ openstack flavor show control
	+----------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field                      | Value                                                                                                                                                                |
	+----------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| OS-FLV-DISABLED:disabled   | False                                                                                                                                                                |
	| OS-FLV-EXT-DATA:ephemeral  | 0                                                                                                                                                                    |
	| access_project_ids         | None                                                                                                                                                                 |
	| disk                       | 40                                                                                                                                                                   |
	| id                         | 81199b17-dcd4-4ca5-92b0-2287c85af020                                                                                                                                 |
	| name                       | control                                                                                                                                                              |
	| os-flavor-access:is_public | True                                                                                                                                                                 |
	| properties                 | capabilities:boot_option='local', capabilities:profile='control', resources:CUSTOM_BAREMETAL='1', resources:DISK_GB='0', resources:MEMORY_MB='0', resources:VCPU='0' |
	| ram                        | 4096                                                                                                                                                                 |
	| rxtx_factor                | 1.0                                                                                                                                                                  |
	| swap                       |                                                                                                                                                                      |
	| vcpus                      | 1                                                                                                                                                                    |
	+----------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------+

You can see from the above output that the controller flavor (**control**) requires 4GB memory, 1 CPU, and 40GB disk, but relies on the **profile** also called "**control**" (see properties field). Hence when Nova is looking for available baremetal nodes stored by Ironic, it will want to find a node that has been associated with the correct profile. If we look at the currently assigned profiles for our nodes, you'll see that the profile has already been assigned (as it was done for the pre-deployed environment):

	$ openstack overcloud profiles list -c "Node Name" -c "Current Profile"
	+--------------------+-----------------+
	| Node Name          | Current Profile |
	+--------------------+-----------------+
	| summit-controller1 | control         |
	| summit-compute1    | compute         |
	| summit-compute2    | compute         |
	| summit-networker1  | networker       |
	+--------------------+-----------------+

> **NOTE**: If you don't see all four nodes listed here it may be an API problem with the public cloud environment that we're using. You can reset the node state with the following, before re-running the profiles list command:

	$ for i in controller1 compute1 compute2 networker1; \
		do ironic node-set-provision-state summit-$i deleted; done

As these are already set, we don't have to make any changes at this point, but to demonstrate how easy it is to set a profile to a node, you can re-run this command:

	$ openstack baremetal node set --property \
		capabilities='profile:control,boot_option:local' summit-controller1

> **NOTE**: This command should produce no output unless there was an error.

## Physical Networking Topology

Each of our overcloud nodes has been configured with **two** "physical" (only quoted because they're virtualised baremetal machines) network interfaces:

* The first network interface (**eth0**) is attached to a dedicated, isolated network that is used for bare metal provisioning, known as the **control plane** (ctlplane), i.e. this is the network that is used by Ironic to deploy the nodes via DHCP/PXE, and is used by the nodes to communicate back to the undercloud during initial deployment and any ongoing maintenance tasks. OSP director is assumed to have full control over this network.

* The second network interface (**eth1**) is attached to a network provided by the public cloud platform in which we can both gain routable access to our overcloud nodes when they're provisioned and also run a number of different VLAN's on-top of for other OpenStack network traffic types, e.g. internal API communication, or storage access.

Or visually represented:

<img src="images/network-topology.png" style="width: 1000px;"/>

> **NOTE**: In the above diagram, the green network (where our jump host is connected to, just incase you're viewing this without colour) represents what would likely be a corporate network in a real-world environment, one that's routable. But within our virtual lab, this is a flat network with subnet 192.168.122.0/24, and we'll be able to easily access the nodes and the OpenStack API's once the overcloud is deployed.

Prior to this lab (and out of scope for the lab instructions) we defined the virtualised nodes within Ironic, and when they were registered we provided the MAC address of **eth0** for each node as the unique identifier, this ensures that when it does DHCP/PXE boot for deployment, Ironic knows exactly which machine it's dealing with. During the initial bootstrap of the image onto the booting nodes, this is the only interface we care about. However, once the machine reboots into the image for the next stage of deployment OSP director needs to configure all of the networking interfaces. To do this, OSP director relies on a set of templates known as '**nic-configs**' to set the configuration for each interface, including any additional VLANs, bonds, or bridges. The primary requirement in our lab environment is going to be defining what happens with **eth1** - how the necessary VLANs are defined on-top of this interface, and how OpenStack can be deployed to use them.

### Out of the Box Options

TripleO and OSP director ship with a number of **nic-config** templates that can be used as examples to build specific templates for the given environment that they're being deployed into. Examples include (and can be found in this directory):

	undercloud$ ll /usr/share/openstack-tripleo-heat-templates/network/config/
	total 0
	drwxr-xr-x. 2 root root 230 Aug 18 08:06 bond-with-vlans
	drwxr-xr-x. 2 root root 194 Aug 18 08:06 multiple-nics
	drwxr-xr-x. 2 root root 170 Aug 18 08:06 single-nic-linux-bridge-vlans
	drwxr-xr-x. 2 root root 205 Aug 18 08:06 single-nic-vlans

In each directory there's a template for each role (at least out of the box roles, **not** taking into consideration composable roles) and slight variations of the roles, e.g. compute with DPDK enabled, or controller with IPv6 support:

	undercloud$ ll /usr/share/openstack-tripleo-heat-templates/network/config/bond-with-vlans/
	total 68
	-rw-r--r--. 1 root root 5813 Apr 27 21:06 ceph-storage.yaml
	-rw-r--r--. 1 root root 6073 Apr 27 21:06 cinder-storage.yaml
	-rw-r--r--. 1 root root 6387 Apr 27 21:06 compute-dpdk.yaml
	-rw-r--r--. 1 root root 6060 Apr 27 21:06 compute.yaml
	-rw-r--r--. 1 root root 6240 Apr 27 21:06 controller-no-external.yaml
	-rw-r--r--. 1 root root 6933 Apr 27 21:06 controller-v6.yaml
	-rw-r--r--. 1 root root 6676 Apr 27 21:06 controller.yaml
	-rw-r--r--. 1 root root 2128 Apr 27 21:06 README.md
	-rw-r--r--. 1 root root 6072 Apr 27 21:06 swift-storage.yaml

Unfortunately, none of these out of the box templates suit our specific environment, i.e. multiple network interfaces but with a dedicated provisioning interface and a dedicated interface for everything else. These templates either assume that you have just a single network interface, or each network interface is for a different traffic type (common in Cisco UCS environments) or if you want all interfaces bonded together for resilience. Within our virtualised baremetal environment we've gone for something slightly different - certainly not recommended for production, but makes it easier to explain how the templates work. As none of these out of the box templates work for us, we're going to have to use custom nic-config templates to suit our requirements.

### Pre-prepared nic-config Templates

In the **~/templates** directory you'll find a subdirectory called **'nic-configs'**, in which we've pre-prepared a set of templates for us to use, ones that match our expectations perfectly. Two network interfaces, first interface being for provisioning (via the OSP director control plane network) and a second interface for running all OpenStack traffic on, including for providing external networking access, i.e. floating IP access, and also OpenStack API access via a routable network from our workstation. Despite these being custom templates they're heavily built from the "**single-nic-vlans**" example.

Let's verify that we have these in place:

	$ ll ~/templates/nic-configs/
	total 20
	-rw-r--r--. 1 stack stack 3951 Dec 1  11:01 compute.yaml
	-rw-r--r--. 1 stack stack 4277 Dec 1  11:01 controller.yaml
	-rw-r--r--. 1 stack stack 4277 Dec 1  11:01 networker.yaml

You'll notice that this directory is slightly cut down from the examples above. This is primarily because we only have three different roles within our environment - we don't have dedicated storage nodes, nor are we using IPv6, etc. We have a single controller, a dedicated networker, and two compute nodes, hence why we only have templates that represent the roles that we want to deploy.

Let's take a look at what these templates actually look like. Just like all other templates, these are **yaml** formatted, and therefore whitespace is incredibly important in describing the hierarchy. Let's cut the main bit out of the template so we can explain the most important section (but please feel free to look into the entire file with the favourite text editor, just don't make any modifications before we start our deployment). The first command below will print the entire of the main section, but we've split the output to describe each section below.

The top level sector is the "**network_config**" type, where all of the interfaces and sub-interfaces are described programmatically. Underneath this we describe our first interface, **eth0**, and associate it with both an IP address (and netmask) from our **ControlPlane**, the network we use for provisioning, as well as a static route to the metadata service:


	$ grep -A54 network_config ~/templates/nic-configs/controller.yaml
            $network_config:
              network_config:
              - type: interface
                name: eth0
                use_dhcp: false
                addresses:
                - ip_netmask:
                    list_join:
                    - /
                    - - get_param: ControlPlaneIp
                      - get_param: ControlPlaneSubnetCidr
                routes:
                - ip_netmask: 169.254.169.254/32
                  next_hop:
                    get_param: EC2MetadataIp
             (...)

Next, we create an Open vSwitch bridge called **br-ex**, and associate it with an IP address and a default route from the **ExternalNetwork**. We also add **eth1** as the physical interface that backs this bridge. OpenDaylight utilises Open vSwitch on the nodes and therefore the configuration here is identical to a non-OpenDaylight environment, it's just that the Open vSwitch will be programmed directly by the OpenDaylight controller instead of Neutron's ML2/OVS implementation. In a later step we'll tell OSP director to configure OpenDaylight to utilise this bridge.

Any interfaces or sub-interfaces associated with this bridge will be able to egress and ingress via **eth1**.

	            - type: ovs_bridge
                name: br-ex
                use_dhcp: false
                dns_servers:
                  get_param: DnsServers
                addresses:
                - ip_netmask:
                    get_param: ExternalIpSubnet
                routes:
                - default: true
                  next_hop:
                    get_param: ExternalInterfaceDefaultRoute
                members:
                - type: interface
                  name: eth1
                  # force the MAC address of the bridge to this interface
                  primary: true
                (...)
	                  
Then, we add multiple **VLAN** sub-interfaces to this bridge, one for each OpenStack network traffic type, noting that via a parameter it looks up the VLAN ID we want to assign to each traffic type:
	                  
	          - type: vlan
                  vlan_id:
                    get_param: InternalApiNetworkVlanID
                  addresses:
                  - ip_netmask:
                      get_param: InternalApiIpSubnet
                - type: vlan
                  vlan_id:
                    get_param: StorageNetworkVlanID
                  addresses:
                  - ip_netmask:
                      get_param: StorageIpSubnet
                - type: vlan
                  vlan_id:
                    get_param: StorageMgmtNetworkVlanID
                  addresses:
                  - ip_netmask:
                      get_param: StorageMgmtIpSubnet
                - type: vlan
                  vlan_id:
                    get_param: TenantNetworkVlanID
                  addresses:
                  - ip_netmask:
                      get_param: TenantIpSubnet

Remember, whitespace is incredibly important for the network interface hierarchy, and the parameters that should be associated to each interface. When the machine boots up for the first time, this network template is provided to a tool called **os-net-config** which applies this template to the local machine. The above example is for the controller model, in which all VLANs are present, have a look at the compute one to see that there are a limited number of VLANs present.

> **NOTE**: OSP director has a lot of granularity when it comes to the physical networks that it utilises for network traffic types, mapped by the **ServiceNetMap**, in which you choose which network is used for each type. For example, you could combine certain traffic types onto one network if the number of VLANs are limited, or you want to minimise the number of network interfaces used. The templates used here satisfy the default network traffic types.

By default, the **ServiceNetMap** maps the OpenDaylightApi network to the Internal API network, in other words, Open vSwitch on all of the compute nodes and the networker nodes is configured to talk to the OpenDaylight controller on the **Internal** network.

OpenDaylight uses a distributed routing architecture, much like the distributed virtual routing (DVR) capabilities on OVS/ML2+Neutron, therefore each compute node needs to have direct physical access to any network that permits floating IP attachment. Therefore, any bridges that are required to allow this functionality must be configured within the nic-config templates also.

So, we've got these nic-config templates, but how do we tell OSP director that we want to actually use them? We can specify this in our environment file, by overriding specific TripleO Heat resource types - unless these are overriden, OSP director assumes that your nodes only have one network interface and carries all traffic over this interface. Inside of our environment file you'll notice that there's a section called **"resource_registry"** in which we can do this very thing - override resource types by specifying the location of a Heat template file that describes such a resource:

	undercloud$ grep -A3 resource_registry ~/templates/config.yaml
	resource_registry:
  	  OS::TripleO::Compute::Net::SoftwareConfig: /home/stack/templates/nic-configs/compute.yaml
  	  OS::TripleO::Controller::Net::SoftwareConfig: /home/stack/templates/nic-configs/controller.yaml
  	  OS::TripleO::Networker::Net::SoftwareConfig: /home/stack/templates/nic-configs/networker.yaml


> **NOTE**: To reiterate, if you omit to advise OSP director of your nic-configs, the default model is to assume that your systems only have a single network interface and that it should be used for provisioning, control plane functionality, and all OpenStack service traffic. This is not a likely deployment in production, but it can suffice for testing.

## Specifying the Network Details

We've got the nic-config templates ready, but they don't actually specify what IP addresses, VLAN's, DNS servers, default routes, etc, to use for each of the interface, all it shows is a link to a parameter which we haven't yet specified. Where do we specify these? Well, just like some of the other parameters, we need to specify these in our environment file. We need to make sure that OSP director knows how to satisfy all of the different network traffic types being requested and therefore need to provide information for each subnet, i.e. which network address range to use, what the default route is (if applicable) and if you're using VLAN isolation, what VLAN ID it should use for each network. There are also a few additional parameters that are advisable to set during this process.

In our environment we've opted to use the following network configuration, where we can specify the subnet size, the VLAN ID, and a range of IP's to use in that pool. In addition, we've specified the network size and default route of the control plane network, as well as the DNS servers to use and the default name for the external network bridge:

	$ grep -A27 "Internal API" ~/templates/config.yaml
	  # Internal API used for private OpenStack Traffic
	  InternalApiNetCidr: 172.17.1.0/24
	  InternalApiAllocationPools: [{'start': '172.17.1.10', 'end': '172.17.1.200'}]
	  InternalApiNetworkVlanID: 101
	  InternalApiNetworkVip: 172.17.1.150
	
	  # Tenant Network Traffic - will be used for VXLAN over VLAN
	  TenantNetCidr: 172.17.2.0/24
	  TenantAllocationPools: [{'start': '172.17.2.10', 'end': '172.17.2.200'}]
	  TenantNetworkVlanID: 201
	
	  # Public Storage Access - e.g. Nova/Glance <--> Ceph
	  StorageNetCidr: 172.17.3.0/24
	  StorageAllocationPools: [{'start': '172.17.3.10', 'end': '172.17.3.200'}]
	  StorageNetworkVlanID: 301
	  StorageNetworkVip: 172.17.3.150
	
	  # Private Storage Access - i.e. Ceph background cluster/replication
	  StorageMgmtNetCidr: 172.17.4.0/24
	  StorageMgmtAllocationPools: [{'start': '172.17.4.10', 'end': '172.17.4.200'}]
	  StorageMgmtNetworkVlanID: 401
	  StorageMgmtNetworkVip: 172.17.4.150
	
	  # External Networking Access - Public API Access
	  ExternalNetCidr: 192.168.122.0/24
	  ExternalAllocationPools: [{'start': '192.168.122.102', 'end': '192.168.122.129'}]
	  ExternalInterfaceDefaultRoute: 192.168.122.1
	  ExternalNetworkVip: 192.168.122.100

As an example, you can see that the external network has been configured such that the subnet specification is **192.168.122.0/24**, with IP's available between 192.168.122.100 and 192.168.122.129, and that the **default route** is 192.168.122.1. Note also that this doesn't have a VLAN associated with it as it's a flat network on the public cloud platform.

## OpenDaylight Specifics

So far, pretty much all of the TripleO configuration that we've used is identical to that of a non-OpenDaylight environment, and would suffice for configuring a standard Neutron with the OVS/ML2 networking implementation. But the whole purpose of this lab is to get OpenDaylight integrated with our overcloud, so surely we have to configure that too, right?

Well, actually the current configuration options are relatively limited; there are many configuration options that make no sense for us to deviate from the defaults, such as the default ports and communication protocols, and some options we cannot enable, such as DPDK, simply because we don't have easy access to such hardware. A list of the current configuration options exposed by TripleO are shown below:

| Parameter  | Details  |
|---|---|
| **OpenDaylightPort** |  This dictates the port that OpenDaylight uses on the Northbound traffic, i.e. the port that Open vSwitch communicates with OpenDaylight on. This defaults to **8081**. |
| **OpenDaylightUsername** |  This sets the username that OpenDaylight uses for management of the SDN controller. Defaults to "**admin**" if left unset.|
| **OpenDaylightPassword** |  This sets the password that goes in combination with the username set in **OpenDaylightUsername**, but defaults to "**admin**" if left unset. |
| **OpenDaylightEnableDHCP** |  This enables DHCP functionality provided by OpenDaylight itself. Unfortunately this is not currently supported, so the parameter is more of a placeholder at this time. OpenDaylight still relies on Neutron's default DHCP agent for implementation. Hence it defaults to **false**, but is ignored. |
| **OpenDaylightFeatures** |  This allows the administrator to specify the features that OpenDaylight enables, recalling that OpenDaylight has a very modular and flexible architecture. This defaults to **[odl-netvirt-openstack, odl-netvirt-ui, odl-jolokia]**, the minimum set of features to meet parity with OVS/ML2 in terms of OpenStack networking integration. Currently, Red Hat only package the list that's provided above, so it's not a good idea to modify this list at this time. |
| **OpenDaylightConnectionProtocol** |  This sets the connection protocol that OpenDaylight can be contacted on for API access, i.e. Layer7. This defaults to '**http**'. |
| **OpenDaylightManageRepositories** |  This either enables or disables the upstream OpenDaylight repositories for plugins and modules, this defaults to **false**. |
| **OpenDaylightSNATMechanism** |  This sets the mechanism in which OpenDaylight provides SNAT support for Neutron networks. This defaults to using **'conntrack'**, which distributes SNAT to compute nodes within the cluster using OVS-based firewalling, but this option also supports **'controller'** mode which punts this functionality to the controllers. Useful for situations in which non-kernel based OVS needs to be used, e.g. DPDK environments. |
| **OpenDaylightCheckURL** |  This sets the URL that OpenDaylight exposes for clients to check that OpenDaylight has come up properly and is ready to function as the controller. This defaults to '**restconf/operational/network-topology:network-topology/topology/netvirt:1**'. |
| **OpenDaylightProviderMappings** | This tells OpenDaylight how to map Neutron logical network labels, e.g. **physnet1** to a configured bridge on the hosts. This parameter is very important to ensure proper networking functionality. By default, OSP director creates a logical Neutron network label called **datacentre**, and this needs to be mapped to an OVS bridge set in the nic-configs. |
| **OvsEnableDpdk** | This parameter, whilst not strictly an OpenDaylight parameter, can be used to enable the DPDK functionality with Open vSwitch, for collaboration with OpenDaylight. This defaults to **false** if unset. |
| **HostAllowedNetworkTypes** |  Again, not strictly an OpenDaylight parameter, but is important as it defines the network types that are permitted to be created by Neutron. This defaults to '**['local', 'vlan', 'vxlan', 'gre']**' if left unset.|

Based on these options, let's make some slighy changes to the default configuration that's provided by the TripleO templates. By default, OSP director creates a logical Neutron network label called **datacentre**, so we will need to map this logical name to an Open vSwitch bridge that OpenDaylight can control:

	$ echo "  OpenDaylightProviderMappings: 'datacentre:br-ex'" >> ~/templates/config.yaml

Next, let's override the default username/password combination for our OpenDaylight controller:
  
	$ echo "  OpenDaylightUsername: admin" >> ~/templates/config.yaml
	$ echo "  OpenDaylightPassword: redhat" >> ~/templates/config.yaml
  
That's it! Our nodes are ready, our templates have been explored and customised to suit, and we understand the options that we could have chosen. Let's move onto the deployment itself.

## OpenStack Deployment with OpenDaylight

In this section we're going to be deploying our Red Hat OpenStack Platform overcloud with an integrated OpenDaylight SDN controller. In the previous step we explored the main physical characteristics of the environment that we're working with and then configured the vast majority of the templates that are required for deployment in such an environment. Before we go ahead with our deployment we need to wrap up a couple of last minute customisations.

### OpenStack Service Containerisation

One of the biggest changes in OSP12 is the containerisation of OpenStack services, i.e. OpenStack services residing and operating within docker-based containers. This is a huge feature of OSP12, and a huge architectural change. Red Hat are embracing it for many reasons...

Firstly, we get dependency isolation for each service - we can embed everything that we need for a given service into a single container image, without having to worry that we’ll break other service functionality, including required library versions, or anything that may be required. It makes updates and upgrades a lot easier to manage - a container can simply be replaced with a newer copy, containing the newer or patched code, and if that operation fails, it’s very simple to roll back to the previous version, without a panic about how to restore the environment. We get a higher degree of deployment flexibility, building on-top of the composable roles functionality we can distribute and rebalance services at will. Scalability is also much easier, we can throw more containers into the mix to accommodate demand when required, and scale back when not, efficiently using hardware. Because we’re using immutable infrastructure, i.e. when it’s running it doesn’t change you need to rip and replace to make a change, it means that the code and configuration is well understood, and it means that the complexity around configuration file management and day to day operations is minimised. Finally, we can also leverage a lot of the new container runtime management technology for better resource utilisation, and control over allocation of system resources. To sum things up, containers are bringing a huge benefit to our customers and Red Hat's ability to support and maintain OpenStack.

### What's required for Containerised Services?

By default in OSP12+, each OpenStack service has it's own self-contained Docker image, containing all required dependencies. Docker images are stored in repositories known as registries and need to be **pulled** before using them. All of the required images have been pulled for you, and reside in a local registry on the undercloud machine. Whilst it's possible to use a remote Docker registry during deployment, it's not very efficient given that the images can be quite large, and also the nodes may not have a network route to the registry. By using the undercloud server as a registry we gain performance and a guaranteed network route.

The typical flow of an administrator wanting to deploy a containerised overcloud is as follows-

1. Generate a list of required images that will need to be used based on the chosen TripleO configuration, e.g. if using OpenDaylight, make sure you include this in your required images list.<br /><br />
2. Take this list and upload the images to the **local** docker registry (on the undercloud) from the **remote** registry (e.g. the Red Hat Content Delivery Network).<br /><br />
3. Generate an environment file used by TripleO; typically stored as "**docker_registry.yaml**", that tells OSP director exactly which docker images to use for each TripleO service and where to find them, i.e. the image location, the name, and the tag on the **local** registry. It's used to tell the booting overcloud nodes which images to pull and where to pull them from.<br /><br />
4. Deploy the overcloud, specifying the generated environment file (i.e. docker_registry.yaml).

Steps (1), (2), and (3) can be taken using TripleO command line tooling, but for convenience we've already taken these steps, and you'll find the correct docker_registry.yaml file in your **~/templates/** directory. As an example, let's ensure that it has the OpenDaylight container configured correctly:

	$ grep -i OpenDaylight ~/templates/docker_registry.yaml | grep -v "^#"
	  DockerNeutronApiImage: 172.16.0.1:8787/rhosp12/openstack-neutron-server-opendaylight:12.0-20180319.1
	  DockerNeutronConfigImage: 172.16.0.1:8787/rhosp12/openstack-neutron-server-opendaylight:12.0-20180319.1
	  DockerOpendaylightApiImage: 172.16.0.1:8787/rhosp12/openstack-opendaylight:12.0-20180319.1
	  DockerOpendaylightConfigImage: 172.16.0.1:8787/rhosp12/openstack-opendaylight:12.0-20180319.1
	
Now we have four images listed, why four? Well, each service upon TripleO instantiation needs to do two things - firstly, it needs to configure itself, e.g. setting up configuration files via puppet and running bootstrap commands (e.g. Galera bootstrap, RabbitMQ configuration, etc), and secondly there has to be an image that is used to run the OpenStack service itself - one that contains the binaries. In the vast majority of cases, this image is the same across both the first phase configuration step, and the second phase actually running of the binaries/services.

### Let's do the deployment...

That's it, we've identified all of the docker images that are required, we've uploaded the images to the local docker registry, and we've generated a TripleO environment file to point OSP director at those images for utilisation by the overcloud nodes. We can now start the deployment. Ensure that you're in the home directory of the stack user (to keep the ~/templates directory clean) and execute the deploy command:

	$ cd ~
	$ openstack overcloud deploy --templates \
		-r ~/templates/lab_roles.yaml \
		-e ~/templates/config.yaml \
		-e ~/templates/docker_registry.yaml \
		-e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml \
		-e /usr/share/openstack-tripleo-heat-templates/environments/services-docker/neutron-opendaylight.yaml
	(...)

The deployment takes approximately 60 minutes (on the public cloud platform that we're using), so depending on how long is left in the lab session you may not get to see this finish, but by getting to this stage you should have a good understanding of what the integration looks like, and how to deploy it. For reference (and for reading whilst the deployment is taking place), the command line options for the deploy command are explained below in more detail:

| Parameter  | Details  |
|---|---|
| --templates |  This tells OSP director that you want to use the TripleO Heat Templates for deployment, from the default location **/usr/share/openstack-tripleo-heat/templates** (unless overridden with a different directory path) |
| -r ~/templates/custom_roles.yaml  | The '**-r**' flag tells the command line tooling that you want to define the roles manually, rather than utilising the out of the box roles. This points to our **~/templates/custom_roles.yaml** file which we are using to create the dedicated networker node. |
| -e ~/templates/config.yaml  | The '**-e**' flag tells OSP director that you're specifying an environment file that will modify the out of the box configuration, it can be used to specify (and override) standard parameters and TripleO resources with custom values and custom templates (e.g. nic-configs). In this section we're providing our custom environment file containing links to our custom nic-configs and all associated parameters that we've described over the past few lab sections. |
| -e ~/templates/docker_registry.yaml  | The environment file that specifies the list of docker images that will be used within the overcloud based on the desired TripleO configuration, including the location of the images, i.e. the local docker registry. |
| -e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml  | An environment file that we've not seen before, and is an out of the box example environment file - this allows us to use the default network isolation mechanism, i.e. a dedicated network (and associated ports) for each network traffic type. |
| -e /usr/share/openstack-tripleo-heat-templates/environments/services-docker/neutron-opendaylight.yaml  | This is another environment file that specifies that we want to use OpenDaylight as part of this deployment. This environment file explicitly disables Neutron's out of the box Open vSwitch configuration, along with non-used supporting agents such as Neutron's L3 agent. It also sets specific parameters so that Neutron can use OpenDaylight.  |
 
<br />
Once the deployment has succeeded, you should receive the following output:

	(...)
	2017-11-27 12:36:56Z [overcloud.AllNodesDeploySteps]: CREATE_COMPLETE  Stack CREATE completed successfully
	2017-11-27 12:36:56Z [overcloud.AllNodesDeploySteps]: CREATE_COMPLETE  state changed
	2017-11-27 12:36:56Z [overcloud]: CREATE_COMPLETE  Stack CREATE completed successfully
	
	 Stack overcloud CREATE_COMPLETE
	
	Host 192.168.122.100 not found in /home/stack/.ssh/known_hosts
	Overcloud Endpoint: http://192.168.122.100:5000/v2.0
	Overcloud Deployed

> **NOTE**: If you do not receive a "**CREATE_COMPLETE**" status output then unfortunately your deployment has failed. This may be because of a syntax error in your templates. Run the following command to output the failures that were caught.

> 		undercloud$ openstack stack failures list overcloud --long

<br />
If you've got this far, we're assuming that you've successfully built your OpenDaylight integrated Red Hat OpenStack Platform overcloud. We have provided a test script that will quickly build some resources to test that everything worked correctly; you can execute it as follows:

	$ sh ~/labs/odl/test-overcloud.sh
	(...)
	
	+--------------------------------------+---------+--------+---------------------------------------+-------+---------+
	| ID                                   | Name    | Status | Networks                              | Image | Flavor  |
	+--------------------------------------+---------+--------+---------------------------------------+-------+---------+
	| 638b3a19-f768-4d31-87e3-cca6e99f0197 | test_vm | ACTIVE | internal=172.16.1.11, 192.168.122.203 | rhel7 | m1.labs |
	+--------------------------------------+---------+--------+---------------------------------------+-------+---------+

The final thing that it should do if things were successful is print out a list of instances, as demonstrated above. A simple ping check should verify if your OpenDaylight deployment was successful, noting that your IP address may be different as it's not consistently assigned:

	$ ping -c4 192.168.122.203
	PING 192.168.122.203 (192.168.122.203) 56(84) bytes of data.
	64 bytes from 192.168.122.203: icmp_seq=1 ttl=64 time=0.521 ms
	64 bytes from 192.168.122.203: icmp_seq=2 ttl=64 time=0.834 ms
	(...)

## Wrapping Up

That's it - we're done! We need to clean up the Heat stack, and we'll take care of the rest before some of the other labs can proceed later in the day...

	$ openstack stack delete multiple
	Are you sure you want to delete this stack(s) [y/N]? yes

Thank you very much for attending this lab, I hope that it gave you a bit of insight into how to use OpenStack and OpenDaylight, how its components fit together and how it all works with practical examples. If you have any feedback please share it with us, and if there's anything we can do to assist you in your OpenStack journey, please don't hesitate to ask!