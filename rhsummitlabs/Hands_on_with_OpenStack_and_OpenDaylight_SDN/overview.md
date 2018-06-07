<img src="images/redhat.png" style="width: 200px;" border=0/>

<font color="red">
**Lab Update - 4th June 2018**

**This lab has now been updated to run on the Red Hat Product Demo System (RHPDS) and so Summit instructions have been removed in favour of specific instructions for RHPDS. You can skip to the first lab section if you're following this post-Summit. If you have any questions or any problems accessing this content, please let us know.**
</font>

<h2>Red Hat Summit, San Francisco 2018</h2>
**Title**: Hands on with OpenStack and OpenDaylight SDN (**L1059**)<br>
**Date**: 8th May 2018<br>

**Authors/Lab Owners**:
<ul class="tab">
<li>Rhys Oxenham <<roxenham@redhat.com>></li>
<li>Nir Yechiel <<nyechiel@redhat.com>></li>
<li>Andre Fredette <<afredette@redhat.com>></li>
<li>Tim Rozet <<trozet@redhat.com>></li>
</ul>


#**Lab Contents**#

1. **What is OpenDaylight and what can it do?**
2. **How does OpenDaylight integrate with OpenStack?**
3. **Getting Started with the Labs**
4. **Prebuilt Lab Investigation**
5. **Testing, investigating, and using OpenDaylight**
6. **Exploration of TripleO requirements for OpenDaylight**

<!--BREAK-->

#**Lab Overview**

First of all, it's my pleasure to welcome you to the Red Hat Summit 2018, here at the San Francisco Moscone Centre! The past few years have been an exciting time for Red Hat, and both the OpenStack and OpenDaylight communities; we've seen unprecedented interest and development in these new revolutionary technologies and we're proud to be at the heart of it all. Red Hat is firmly committed to the future of OpenStack and OpenDaylight; our goal is to continue to enhance the technology, make it more readily consumable and to enable our customers to be successful when using it.

This hands-on lab aims to get you, the attendees, a bit closer to both Red Hat OpenStack Platform and OpenDaylight, with a specific focus on both the usage and deployment integration, and will run through an investigation of a pre-built environment, how to use it including some troubleshooting steps, and then an investigation of how it's possible to specify and configure OpenDaylight as part of your Red Hat OpenStack Platform deployment. We will use a combination of command-line tools and interaction via the OpenStack Dashboard (Horizon).

Whilst you'll be asked to use and explore some fundamental components within OpenStack, you won't need to install OpenStack or OpenDaylight from scratch within this lab, we've provided a pre-installed OpenStack environment with OpenDaylight already integrated. The environment is comprised of one controller, one networker (where OpenDaylight runs), and two compute nodes. These machines will be virtual machines themselves, running on-top of a shared public cloud environment. You will have been provided with the necessary connection details on-screen and the first lab will demonstrate how to connect into the environment and how to get started with the lab sections.

> **NOTE**: If you've **not** been provided with connection details or you do not see your unique session information on-screen, please ask and we'll ensure that access is provided.

If you have any problems at all or have any questions about Red Hat, our OpenStack distribution, or OpenDaylight, please put your hand-up and a lab moderator will be with you shortly to assist - we've asked many of our OpenStack networking experts to be here today, so please make use of their time. If you have printed materials, they're yours to take away with you, otherwise the online copy will be available for the foreseeable future; I hope that they'll be useful assets in your OpenStack endeavours.


This lab is comprised of two major components, split into individual sections:

* **Overviews** - Here we'll be mainly focusing on background information; a detailed and comprehensive look at the architecture/topology, components, features, and futures of OpenDaylight and its integration with OpenStack. For those that are already familiar with OpenDaylight concepts, you may want to skip over this and head to the hands-on labs to put things into practice.<br /><br />
* **Hands-on Labs** - Here we'll be providing some guided, hands-on labs that will walk you through the usage of an OpenDaylight-enabled OpenStack environment so that you can build your skills, experience, and expertise. This will be combined with an investigation into how OpenDaylight can be deployed by OSP director, Red Hat's deployment and lifecycle management tool for Red Hat OpenStack Platform.

> **NOTE**: This lab has been written specifically to coincide with the launch of Red Hat OpenStack Platform 12 (based on upstream Pike), where OpenDaylight integration will be at technology preview status.

<br /><br />

##**What is OpenDaylight?**
<center><img src="images/opendaylight.png" style="width: 600px;" border=0/></center>

[OpenDaylight](https://www.opendaylight.org/), hosted by the Linux Foundation, is an open-source, open-standards based Software Defined Networking (SDN) implementation; one that is flexible, modular, and scalable. Being modular, it's comprised of many different sub-components or projects that combine together to create a comprehensive solution that addresses many different use-cases and requirements. The OpenDaylight community is vibrant and ever expanding with a wide variety of ecosystem partners, enabling innovation and integration at all levels.

Customers are choosing to implement SDN's for a number of different reasons; the majority of which have nothing to do with OpenStack at all. Many organisations are desiring a much more comprehensive, robust, flexible, and centralised networking management platform at the core of their business, enabling them to have a greater degree of control over their networking implementation, reducing silos of networking infrastructure whilst enabling scalability. But on-top of this, a true SDN provides programmability of the networking fabric, and should not be restricted to a single area of the business; it should span all (often heterogenous) infrastructure providers, including physical hardware, virtual infrastructure, virtual switches and the physical networking layers. There should be a holistic view of the entire networking infrastructure, with management and intelligence provided through open API's and optional graphical representations.

There are a large number of vendors working in this space to provide their own SDN implementations, some of which are open-source, but many are proprietary based; examples include Juniper Contrail, Nuage Networks, VMware NSX, and Cisco ACI. OpenDaylight is a community project with the backing of many different vendors, Red Hat included, to create a comprehensive open-source alternative without vendor lock-in. Its goal is to further the adoption and innovation of software-defined networking through the creation of a common, industry supported platform; one that can adapt to the requirements of applications deployed on-top of it, rather than having applications constrained by classical networking constructs. OpenDaylight itself is quite closely aligned with NFV (Network Function Virtualisation) adoption due the similarities that NFV has on compute virtualisation with SDN and network virtualisation; SDN drives on-demand deployment of virtual network services when, and where, they are needed.

OpenDaylight has been adopted as the core SDN implementation of the [OPNFV community](https://www.opnfv.org/community/upstream-projects/opendaylight), as well as being positioned as a core-enabling technology for Telco/NFV customers for Red Hat. We have no current plans to productise OpenDaylight as a stand-alone product, but we'll be continuing to integrate it alongside Red Hat OpenStack Platform in the coming releases, where we'll be adding significant value to customers in these industry sectors with the features that they require for their implementations, such as comprehensive policy-driven control over networking, service-function chaining, and open-API's/standards for integration with existing datacentre investments. Red Hat will ensure that the components and modules that ship with OpenDaylight have been thoroughly tested for functionality, stability, and compatibility, ensuring that customers have an easy transition to an SDN environment should they want to.

The OpenDaylight architecture is modular and pluggable.  It consists of a Northbound layer (usually where REST API based applications exist), the Model-Driven Service Abstraction Layer (MD-SAL) which is used to service the core database as well as RPCs and communication between Services and Applications, as well as the Southbound layer, which contains many plug-ins used to program and interact with the network forwarding fabric. The following diagram picture shows a simplified view of the typical OpenDaylight architecture:
<br /><br />

<center><img src="images/odl-architecture.png" style="width: 900px;" border=0/></center>

The Red Hat OpenDaylight solution (part of the Red Hat OpenStack Platform) includes several Northbound plugins that provide things like authorisation (AAA), REST interface with Neutron (Neutron NB application), translating Neutron models to OpenFlow (NetVirt). NetVirt handles most of the complex logic of taking abstract Neutron network resources and translating it into OpenFlow/OVSDB based data models. In the future releases, more applications will be added to provide additional SDN/NFV functionality.

Most applications will only use a small subset of the available southbound plug-ins to control the data plane. The NetVirt application of the Red Hat OpenDaylight solution uses OpenFlow and Open vSwitch Database Management Protocol (OVSDB). The overview of the Red Hat OpenDaylight architecture is shown in the following diagram:
<br /><br />

<center><img src="images/odl-parts.png" style="width: 600px;" border=0/></center>
<br /><br />

##**How does OpenDaylight integrate with OpenStack?**

OpenStack Networking (Neutron) supports a plugin model that allows it to integrate with multiple different systems in order to implement networking capabilities for OpenStack. For the purpose of OpenStack integration, OpenDaylight exposes a single common northbound service, which is implemented by the Neutron Northbound component. The exposed API matches exactly the REST API of Neutron. This common service allows multiple Neutron providers to exist in OpenDaylight. As mentioned before, the Red Hat OpenDaylight solution is based on NetVirt as a Neutron provider for OpenStack. It is important to highlight that NetVirt consumes the Neutron API, rather than replacing or changing it.

The OpenDaylight plug-in for OpenStack Neutron is called **networking-odl**, and is responsible for passing the OpenStack network configuration into the OpenDaylight controller. The communication between OpenStack and OpenDaylight is done using the public REST APIs. This model simplifies the implementation on the OpenStack side, because it offloads most of the networking tasks onto OpenDaylight, which diminishes the processing burden for OpenStack.  OpenDaylight directly programs the Open vSwitch (OVS) switches in the data center.  This eliminates the need for most agents on every OpenStack node.  For example, when using OpenDaylight there is no OVS agent or L3 agent on compute. This means less processes running on compute nodes, which frees up more resources on a per node basis and allows greater scalability within OSP.  However, the Neutron DHCP agent is still used and exists on each OpenStack controller.
<br /><br />

<center><img src="images/odl-neutron.png" style="width: 700px;" border=0/></center>

<br /><br />
The OpenDaylight controller uses NetVirt, then configures Open vSwitch instances (which use the OpenFlow and OVSDB protocols), and provides the necessary networking environment. This includes Layer 2 networking, L3 distributed virtual routing (DVR), security groups, and so on. The OpenDaylight controller can maintain the necessary isolation among different tenants. In addition, NetVirt is also able to control hardware gateways using the OVSDB protocol. A hardware gateway is typically a top of rack (ToR) Ethernet switch, that supports the OVSDB hardware_vtep schema, and can be used to connect virtual machines with physical devices. These physical machines may span over multiple L3 domains but still be able to function as part of the same broadcast L2 domain as the virtual machines thanks to VXLAN tunneling overlay, which terminates at the HW VTEP switch. The traffic is then mapped to VLANs and sent to the appropriate physical machine; making it seem as if the VMs and physical machines were in the same L2 domain.
<br /><br />

## **Overview of OpenDaylight Features with OpenStack (OSP12)**

This section lists the key features available with OpenDaylight and Red Hat OpenStack Platform 12 release. Please skip ahead if you're already aware of these, or if you just want to have them as a reference for a later date.

* **Integration with Red Hat OpenStack Platform Director** - The Red Hat OpenStack Platform director is a toolset for installing and managing a complete OpenStack environment. With Red Hat OpenStack Platform 12, director can deploy and configure OpenStack to work with OpenDaylight. OpenDaylight can run together with the OpenStack overcloud controller role, or as a separate custom role on a different node in several possible scenarios. In Red Hat Openstack Platform 12, OpenDaylight is installed and run in a **container** which provides more flexibility to its maintenance and use.

* **L2 Connectivity between OpenStack instances** - OpenDaylight provides the required Layer 2 (L2) connectivity among VM instances belonging to the same Neutron virtual network. Each time a Neutron network is created by a user, OpenDaylight automatically sets the required Open vSwitch (OVS) parameters on the relevant compute nodes to ensure that instances, belonging to the same network, can communicate with each other over a shared broadcast domain.

	While VXLAN is the recommended encapsulation format for tenant networks traffic, 802.1q VLANs are also supported. In the case of VXLAN, OpenDaylight creates and manage the virtual tunnel endpoints (VTEPs) between the OVS nodes automatically to ensure efficient communication between the nodes, and without relying on any special features on the underlying fabric (the only requirement from the underlying network is support for unicast IP routing between the nodes).

* **IP Address Management (IPAM)** - VM instances get automatically assigned with an IPv4 address using the DHCP protocol, according to the tenant subnet configuration. This is currently done by leveraging the Neutron DHCP agent. Each tenant is completely isolated from other tenants, so that IP addresses can overlap.

	> **NOTE**: OpenDaylight can operate as a DHCP server. However, using the Neutron DHCP agent provides High Availability (HA) and support for VM instance metadata (cloud-init). Therefore Red Hat recommends to deploy the DHCP agent, rather than relying on OpenDaylight for such functionality at this time.

* **Routing between OpenStack networks** - OpenDaylight provides support for Layer 3 (L3) routing between OpenStack networks, whenever a virtual router device is defined by the user. Routing is supported between different networks of the same project (tenant), which is also commonly referred to as East-West routing. OpenDaylight uses a distributed virtual routing paradigm, so that the packet forwarding and routing is done locally on each compute node and programmed directly into the switch dataplane.

* **Floating IPs** - A floating IP is a 1-to-1 IPv4 address mapping between a floating address (on an external network) and the fixed IP address, assigned to the instance in the tenant network. Once a VM instance is assigned with a floating IP by the user, the IP is used for any incoming or outgoing external communication. Red Hat OpenStack Platform director includes a default template, where each compute role has external connectivity for floating IPs communication. These external connections support both flat (untagged) and VLAN based networks, and is a source of **DNAT** (Destination Network Address Translation).

* **Security Groups** - OpenDaylight provides support for tenant configurable Security Groups that allow a tenant to control what traffic can flow in and out VM instances. Security Groups can be assigned per VM port or per Neutron network, and filter traffic based on TCP/IP characteristics such as IP address, IP protocol numbers, TCP/UDP port numbers and ICMP codes.

	By default, each instance is assigned a default Security Group, where egress traffic is allowed, but all ingress traffic to the VM is blocked. The only exception is the trusted control-plane traffic such as ARP and DHCP. In addition, anti-spoofing rules are present, so a VM cannot send or receive packets with MAC or IP addresses that are unknown to Neutron. OpenDaylight also provides support for the Neutron port-security extension, that allows tenants to turn on or off security filtering on a per port basis. OpenDaylight implements the Security Groups rules within OVS in a stateful manner, by leveraging OpenFlow and conntrack.

* **IPv6** - IPv6 is an Internet Layer protocol for packet-switched networking and provides end-to-end datagram transmission across multiple IP networks, similarly to the previous implementation known as IPv4. The IPv6 networking not only offers far more IP addresses to connect various devices into the network, but it also allows to use other features that were previously not possible, such as stateless address autoconfiguration, network renumbering, and router announcements.

	OpenDaylight in Red Hat OpenStack Platform 12 brings some feature parity in IPv6 use-cases with OpenStack Neutron. Some of the features that are supported in OpenDaylight include: IPv6 addressing support including stateless address autoconfiguration (SLAAC), DHCPv4 and DHCPv6 modes, IPv6 Security Groups along with allowed address pairs, IPv6 VM-to-VM communication in same network, IPv6 East-West routing, and Dual Stack (IPv4/IPv6) networks.

* **VLAN aware VMs** - VLAN aware VMs (or VMs with trunking support) allows an instance to be connected to one or more networks over one virtual NIC (vNIC). Multiple networks can be presented to an instance by connecting it to a single port. Network trunking lets users create a port, associate it with a trunk, and launch an instance on that port. Later, additional networks can be attached to or detached from the instance dynamically without interrupting the instance’s operations.

	The trunk typically provides a parent port, which the trunk is associated with, and can have any number of child ports (subports). When users want to create instances, they need to specify the parent port of the trunk to attach the instance to it. The network presented by the subport is the network of the associated port. The VMs see the parent port as an untagged VLANs and the child ports are tagged VLANs.

* **SNAT** - SNAT (Source Network Address Translation) enables that virtual machines in a tenant network have access to the external network without using floating IPs. It uses NAPT (Network Address Port Translation) to allow multiple virtual machines communicating over the same router gateway to use the same external IP address.

	Red Hat OpenStack Platform 12 introduces the conntrack based SNAT where it uses OVS netfilter integration where netfilter maintains the translations. One switch is designated as a NAPT switch, and performs the centralised translation role. All the other switches send the packet to centralised switch for SNAT. If a NAPT switch goes down an alternate switch is selected for the translations, but the existing translations will be lost on a failover.

* **OVS-DPDK** - Open vSwitch is a multilayer virtual switch that uses the OpenFlow protocol and its OVSDB interface to control the switch. The native Open vSwitch uses the kernel space to deliver data to the applications. The kernel creates the so called flow table which holds rules to forward the passing packets. Packets that do not match any rule, usually the first packets are sent to an application in the user space for further processing. When the application (a daemon) handles the packet, it makes a record in the flow table, so that next packets could use a faster path. Thus, OVS can save a reasonable amount of time by by-passing the time consuming switching between the kernel and the applications. Such approach can still have limitations in the bandwidth of the Linux network stack, which is unsuitable for use cases that require to process a high rate of packets, such as telecommunications.

	DPDK is a set of user space libraries that enable a user to build applications that can process the data faster. It offers several Poll Mode Drivers (PMDs), that enable the packets to pass the kernel stack and go directly to the user space. Such behaviour speeds up the communication remarkably, because it handles the traffic outside of the kernel space completely. OpenDaylight in Red Hat Openstack Platform 12 may be deployed with Open vSwitch Data Plane Development Kit (DPDK) acceleration with director. This deployment offers higher data plane performance as packets are processed in user space rather than in the kernel.

* **SR-IOV integration** - The Single Root I/O Virtualisation (SR-IOV) specification is a standard for a type of PCI device assignment that can project a single networking device to multiple virtual machines and improve their performance. For example, SR-IOV enables a single Ethernet port to appear as multiple, separate, physical devices. A physical device with SR-IOV capabilities can be configured to appear in the PCI configuration space as multiple functions. Basically, SR-IOV distinguishes between Physical Functions (PFs) and Virtual Functions (VFs). PFs are full PCIe devices with SR-IOV capabilities. They provide the same functionality as usual PCI devices and can be assigned the VFs.

	VFs are simple PCIe functions that derive from PFs. The number of Virtual Functions a device may have is limited by the device hardware. A single Ethernet port, the Physical Device, may map to many Virtual Functions that can be shared to virtual machines through the hypervisor. It maps one or more Virtual Functions to a virtual machine. Each VF can be mapped to a single guest at a time only, because it requires real hardware resources. A virtual machine can have more VFs. To the virtual machine, the VF appears as a usual networking card.

	The main advantage is that the SR-IOV devices can share a single physical port with multiple virtual machines. Furthermore, the VFs have near-native performance and provide better performance than paravirtualised drivers and emulated access, and they provide data protection between virtual machines on the same physical server. OpenDaylight in Red Hat OpenStack Platform 12 can be deployed with compute nodes that support SR-IOV. It is also possible to create mixed environments with both OVS and SR-IOV nodes in a single OpenDaylight installation. The SR-IOV deployment requires the Neutron SR-IOV agent in order to configure the virtual functions (VFs), which are directly passed to the compute instance when it is deployed as a network port.

* **Controller clustering** - High availability is the continued availability of a service even when individual systems providing it fail. There are a number of different ways of implementing high availability; one desirable feature shared by most is that whatever operations are involved in ensuring continuity of service are handled automatically by the system, without administrator involvement. Typically system administrators will be notified when systems fail, but won’t need to take action to keep the overall service operational; they will only need to take manual action to restore the entire system to its nominal configuration.

	The OpenDaylight Controller in Red Hat OpenStack Platform supports a cluster based High Availability model. Several instances of the OpenDaylight Controller form a Controller Cluster and together, they work as one logical controller. The service provided by the controller, viewed as a logical unit, continues to operate as long as a majority of the controller instances are functional and able to communicate with each other. The Red Hat OpenDaylight Clustering model provides both High Availability and horizontal scaling: more nodes can be added to absorb more load, if necessary.

* **Hardware VXLAN VTEP (L2GW)** - Layer 2 gateway services allow a tenant’s virtual network to be bridged to a physical network. This integration provides users with the capability to access resources on a physical server through a layer 2 network connection rather than via a routed layer 3 connection, that means extending the layer 2 broadcast domain instead of going through L3 or Floating IPs. To implement this, there is a need to create a bridge between the virtual workloads running inside an overlay (VXLAN) and workloads running in physical networks (normally using VLAN). This requires some sort of control over the physical top-of-rack (ToR) switch the physical workload is connected to. Hardware VXLAN Gateway (aka HW VTEP) can help with that.

	HW VTEP (VXLAN Tunnel End Point) usually resides on the ToR switch itself and performs VXLAN encapsulation and de-encapsulation. Each VTEP device has two interfaces – one is a VLAN interface (facing the physical server) and the other is an IP interface to other VTEPs. The idea behind hardware VTEPs is to create an overlay network that connects VMs and physical servers and make them think that they’re in the same L2 network. Red Hat OpenStack customers can benefit from an L2GW to integrate traditional bare-metal services into a Neutron overlay. This is especially useful for bridging external physical workloads into a Neutron tenant network, BMaaS/Ironic for bringing a bare metal server (managed by OpenStack) into a tenant network, and bridging SR-IOV traffic into a VXLAN overlay; taking advantage of the line-rate speed of SR-IOV and the benefits of an overlay network to interconnect SR-IOV VMs.
