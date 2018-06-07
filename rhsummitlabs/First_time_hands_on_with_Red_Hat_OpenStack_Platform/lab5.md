#**Lab 5: Creation of Instances in Nova (Compute Service)**

##**Introduction**

Nova is OpenStack's compute service, it's responsible for scheduling and lifecycle management of compute resource, e.g. instances (note it's called an instance as it's not explicit that OpenStack provisions a virtual machine, it could be a baremetal node, or indeed a container). Nova is undoubtably one of the most important parts of the OpenStack project and is indeed where the vast majority of the original OpenStack code was implemented.

Nova schedules virtual machines to run on a set of nodes by defining drivers that interact with underlying virtualisation mechanisms, and by exposing the functionality to the other OpenStack components. It's responsible for ensuring that an instance continues to run, can have networking interfaces attached, access to storage devices, and provides full control over the lifecycle to the project that owns the instance.

Nova supports a wide variety of drivers, but we're going to be focussing on the libvirt driver that uses KVM as the hypervisor. This lab enables us to start instances based on the previous two labs; each instance requires an image to boot from and a network to attach to at the very least.

##**Starting instances via the console**

Let's launch our first instance in OpenStack using the command line. The first task that we'll do is create our own custom flavor; flavors define the specification of the instance in terms of vCPUs, memory, disk space, etc. Given that we have to run all of this on a workstation, we have to create our own specific flavor to match the needs of our image, but also to fit within the constraints of the environment.

First, make sure we've sourced our environment file (with administrative access):

	$ source ~/demorc
	
Then, create a new flavor for us to use:

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

What we've done there is to create a new flavor, with the name "m1.labs" with 2048MB RAM, 2 vCPU's, and 10GB of disk space; this matches the requirements of our booting RHEL7 image, where all other flavors wouldn't have sufficed. Any instance that we create with this flavor profile will be assigned this specification.

Next, boot a new instance on OpenStack using our new flavor and the image we uploaded earlier ("**rhel7**"), ensure you specify a name for the instance, below we use "**my_vm**":

	$ openstack server create --flavor m1.labs --image rhel7 my_vm
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
	| adminPass                           | Qk7M9caVyjQT                                 |
	| config_drive                        |                                              |
	| created                             | 2018-04-09T14:38:25Z                         |
	| flavor                              | m1.labs (6)                                  |
	| hostId                              |                                              |
	| id                                  | e11dba06-cccf-47ce-b6e2-8c2d425d382a         |
	| image                               | rhel7 (4568dd3f-b6f6-4a8e-b551-473e885cf7c5) |
	| key_name                            | None                                         |
	| name                                | my_vm                                        |
	| progress                            | 0                                            |
	| project_id                          | f991d44fac91419c8e6016184381871a             |
	| properties                          |                                              |
	| security_groups                     | name='default'                               |
	| status                              | BUILD                                        |
	| updated                             | 2018-04-09T14:38:25Z                         |
	| user_id                             | 2c580c9e773143f5b4d82b9a6131b47a             |
	| volumes_attached                    |                                              |
	+-------------------------------------+----------------------------------------------+
	
Note that because we only have one private network ("**internal**" as created earlier), it assumes we want to join this one. Otherwise we would have had to specify a network to use. We can verify that our system has been started successfully with the following command, noting that it may take a few minutes to become active:

	$ openstack server list
	+--------------------------------------+-------+--------+---------------------+-------+---------+
	| ID                                   | Name  | Status | Networks            | Image | Flavor  |
	+--------------------------------------+-------+--------+---------------------+-------+---------+
	| e11dba06-cccf-47ce-b6e2-8c2d425d382a | my_vm | ACTIVE | internal=172.16.1.9 | rhel7 | m1.labs |
	+--------------------------------------+-------+--------+---------------------+-------+---------+
	
> **NOTE**: You may see that the system is in a **"SPAWNING"** state for a few minutes, this is to be expected as the machine provisions itself. Please be patient and allow it to start. We want to make sure that it goes into an **"ACTIVE"** state.

##**Viewing Console Output (VNC)**

It's critical to ensure that our virtual machine has started successfully. Nova provides us with two main ways of checking this without relying on networking being available - the console output, and the VNC console output. During this section, we'll demonstrate how to access the VNC console of the booting VM. OpenStack provides a component called novncproxy; this brokers connections from clients to the compute nodes running the instances - connections come into the novncproxy service only, it then creates a tunnel through to the VNC server, meaning VNC servers need not be open to everyone, only to the proxy service.

Using a web-browser, we can view the VNC console via an HTML5 viewer. We can get the URL via the following command:

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

Close down the web browser window before continuing.


##**Attaching Floating IP's to Instances**

According to Nova, our machine has been given a network address of **172.16.1.5** and has been started; the IP address that you receive may be slightly different. You'll note that the IP address of this system is on the private **internal** network, and you won't be able to access it - remember that despite us attaching a router to get to the outside, this machine doesn't have a floating IP yet, and as such we cannot get to it from the outside.

OpenStack allows us to assign 'floating IPs' to instances to allow network traffic from any **external** network to be routed to a specific instance, running on a private tenant network, or in our case the **internal** network. Behind the scenes Neutron's L3-agent listens on an additional IP address based on the network bridge and physical interface we configured in an earlier lab and uses NAT to tunnel the traffic to the correct instance on the private/internal network.

##**Creating and Assigning Floating IP's**

For this task you'll need an instance running first, if you don't have one running revisit the previous section and start an instance. We'll request floating IP's from a pool that we've already created, this is known as the allocation pool on the **external** network.

OpenStack makes you 'claim' an IP from the available list of IP addresses for the tenant (project) you're currently running in before you can assign it to an instance, we specify the 'ext' network to claim from:

	$ openstack floating ip create external
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-09T14:44:38Z                 |
	| description         |                                      |
	| fixed_ip_address    | None                                 |
	| floating_ip_address | 192.168.122.210                      |
	| floating_network_id | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| id                  | 88d118f7-daad-4fa0-b94c-37158e98ab59 |
	| name                | 192.168.122.210                      |
	| port_id             | None                                 |
	| project_id          | f991d44fac91419c8e6016184381871a     |
	| revision_number     | 0                                    |
	| router_id           | None                                 |
	| status              | DOWN                                 |
	| updated_at          | 2018-04-09T14:44:38Z                 |
	+---------------------+--------------------------------------+
	
You can see that it's reserved **192.168.122.210** for our project, although it's not attached to an instance *yet* and therefore is not much use. Next, we can assign our claimed IP address to an instance:

	$ openstack server add floating ip my_vm 192.168.122.210	
> **NOTE:** If the command is successful it has no output, and your IP address may vary from the one displayed above - use the IP address that the create command allocated to you.

You can now verify that the IP address was assigned to your node with the following:

	$ openstack server list
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| ID                                   | Name  | Status | Networks                             | Image | Flavor  |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| e11dba06-cccf-47ce-b6e2-8c2d425d382a | my_vm | ACTIVE | internal=172.16.1.9, 192.168.122.210 | rhel7 | m1.labs |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+

> **NOTE**: It may take a while for the floating IP to show up here, so keep trying the above command. 

For those of you that may have already tried to ping or SSH into the node using this IP address may be confused as to why this is not working. By default, OpenStack's security group rules will prevent **all** inbound access, so we'll need to open these up before we confirm that our instance is working properly.

##**OpenStack Security Groups**

By default, OpenStack Security Groups prevent any inbound access to instances, including ICMP/ping! Therefore, we have to edit the security group policy to ensure that the firewall is opened up for us. If you don't believe me, try ping'ing your instance:

	$ ping 192.168.122.210
	PING 192.168.122.210 (192.168.122.210) 56(84) bytes of data.
	^C
	--- 192.168.122.210 ping statistics ---
	14 packets transmitted, 0 received, 100% packet loss, time 13005ms

> **NOTE**: You'll likely need to Ctrl-C the above ping command to get back out.

Let's add two rules, firstly for all instances to have ICMP and SSH access. By default, Neutron ships with a 'default' security group, it's possible to create new groups and assign custom rules to these groups and then assign these groups to individual servers. For this lab, we'll just configure the default group. The problem is that there are multiple default groups, and as an administrator you can see them all, noting that they're project-specific, and the ID for your project will be different than the ones in the example below as we created it in an earlier step:

	$ openstack security group list -c ID -c Project
	+--------------------------------------+----------------------------------+
	| ID                                   | Project                          |
	+--------------------------------------+----------------------------------+
	| 13f8b461-0a66-4c1d-ba81-9bd53b16c31a | 9eb95e04cff34482b44b8672b65caac9 |
	| 69f49751-975f-412b-a21f-765b6e7715a4 | f991d44fac91419c8e6016184381871a |
	| 6f2a4119-fc40-434e-8b71-f9796cd4d9cc |                                  |
	| c9fcfba4-5788-4f50-b4f9-2bffd398feb3 | ae16125945e84ecbb09f81e6ec102649 |
	+--------------------------------------+----------------------------------+

So, let's look at enabling ICMP for *every* node within the default group, using the security group ID as the unique identifier for our '**default**' group within the demo project that we're using (and that our instance is booted onto):

	$ openstack project list
	+----------------------------------+---------+
	| ID                               | Name    |
	+----------------------------------+---------+
	| 9eb95e04cff34482b44b8672b65caac9 | admin   |
	| ae16125945e84ecbb09f81e6ec102649 | service |
	| f991d44fac91419c8e6016184381871a | demo    |          <--- this is the project we're concerned with
	+----------------------------------+---------+

If we take the the project ID of our demo project (**f991d44fac91419c8e6016184381871a**), noting that your UUID will be different here, we can see that it maps to security group ID **69f49751-975f-412b-a21f-765b6e7715a4** from the output of 'openstack security group list' as shown above. We can now create the rules in the correct group, for convenience we'll ensure we grab the correct one:

	$ export MY_PROJECT=$(openstack project list | awk '$4 == "demo" {print $2};')
	$ export SEC_GROUP_ID=$(openstack security group list | grep $MY_PROJECT | awk '{print $2;}')
	$ echo $SEC_GROUP_ID
	69f49751-975f-412b-a21f-765b6e7715a4
	
Now we can use this ID to create our security group rules, noting that your ID *will* be different from the one above, hence why we used the export commands above to ensure that we're using the correct ID's for your environment:

	$ openstack security group rule create --proto icmp $SEC_GROUP_ID
	+-------------------+--------------------------------------+
	| Field             | Value                                |
	+-------------------+--------------------------------------+
	| created_at        | 2018-04-09T15:14:29Z                 |
	| description       |                                      |
	| direction         | ingress                              |
	| ether_type        | IPv4                                 |
	| id                | 2546ea92-3417-44e3-a4ed-e7d4e4849637 |
	| name              | None                                 |
	| port_range_max    | None                                 |
	| port_range_min    | None                                 |
	| project_id        | f991d44fac91419c8e6016184381871a     |
	| protocol          | icmp                                 |
	| remote_group_id   | None                                 |
	| remote_ip_prefix  | 0.0.0.0/0                            |
	| revision_number   | 0                                    |
	| security_group_id | 69f49751-975f-412b-a21f-765b6e7715a4 |
	| updated_at        | 2018-04-09T15:14:29Z                 |
	+-------------------+--------------------------------------+
	
> **NOTE:** These security group rules can be configured with a lot more granularity, e.g. specifying from which subnets we allow from, etc, but here we allow from all as we didn't specify any limitations.

Within a few seconds (for the hypervisor to pick up the changes) you should be able to ping your floating IP:

	$ ping -c4 192.168.122.210
	PING 192.168.122.210 (192.168.122.210) 56(84) bytes of data.
	64 bytes from 192.168.122.210: icmp_seq=1 ttl=63 time=3.82 ms
	64 bytes from 192.168.122.210: icmp_seq=2 ttl=63 time=1.34 ms
	64 bytes from 192.168.122.210: icmp_seq=3 ttl=63 time=1.16 ms
	(...)

We can ping, but we can't SSH yet, as that's still not allowed (**Ctrl-C** to stop this command if you don't want to wait for it to timeout):

	$ ssh -v root@192.168.122.210
	OpenSSH_7.4p1, OpenSSL 1.0.2k-fips  26 Jan 2017
	debug1: Reading configuration data /etc/ssh/ssh_config
	debug1: /etc/ssh/ssh_config line 58: Applying options for *
	debug1: Connecting to 192.168.122.210 [192.168.122.210] port 22.
	debug1: connect to address 192.168.122.210 port 22: Connection timed out
	ssh: connect to host 192.168.122.210 port 22: Connection timed out

Next, let's try adding another rule, to allow SSH access for all instances in the 'default' group, but restrict access to ssh from the **192.168.122.0/24** source network only:

	$ openstack security group rule create --proto tcp --dst-port 22:22 $SEC_GROUP_ID
	+-------------------+--------------------------------------+
	| Field             | Value                                |
	+-------------------+--------------------------------------+
	| created_at        | 2018-04-09T15:16:03Z                 |
	| description       |                                      |
	| direction         | ingress                              |
	| ether_type        | IPv4                                 |
	| id                | 425f092a-901f-4e9a-b241-b01f325bbd85 |
	| name              | None                                 |
	| port_range_max    | 22                                   |
	| port_range_min    | 22                                   |
	| project_id        | f991d44fac91419c8e6016184381871a     |
	| protocol          | tcp                                  |
	| remote_group_id   | None                                 |
	| remote_ip_prefix  | 0.0.0.0/0                            |
	| revision_number   | 0                                    |
	| security_group_id | 69f49751-975f-412b-a21f-765b6e7715a4 |
	| updated_at        | 2018-04-09T15:16:03Z                 |
	+-------------------+--------------------------------------+

Let's now retry the SSH connection.. (The root password is **'redhat'**)

	$ ssh root@192.168.122.210
	The authenticity of host '192.168.122.210 (192.168.122.210)' can't be established.
	ECDSA key fingerprint is SHA256:+uZc8kjyYfBW5RRV67kbpbaESKrard1XuosJDkpy0s0.
	ECDSA key fingerprint is MD5:01:d6:c8:87:8a:5e:34:cf:35:95:1c:df:1f:b5:15:1c.
	Are you sure you want to continue connecting (yes/no)? yes
	Warning: Permanently added '192.168.122.210' (ECDSA) to the list of known hosts.
	Password:
	Last login: Mon Apr  9 10:42:28 2018
	[root@my-vm ~]#
	
When you see the "**[root@my-vm ~]#**" prompt, you're connected into your instance successfully. Check the network configuration within the instance, note that it is *not* aware of the "192.168.122.210" address - this is being NAT'd by the L3 agent from the outside **external** network:

	[root@my-vm ~]# ip address show eth0
	2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc pfifo_fast state UP qlen 1000
	    link/ether fa:16:3e:8e:25:34 brd ff:ff:ff:ff:ff:ff
	    inet 172.16.1.9/24 brd 172.16.1.255 scope global dynamic eth0
	       valid_lft 84123sec preferred_lft 84123sec
	    inet6 fe80::f816:3eff:fe8e:2534/64 scope link
	       valid_lft forever preferred_lft forever

Press "Ctrl+d", or simply type "**exit**" to return to your OpenStack environment:

	[root@my-vm ~]# exit
	Connection to 192.168.122.210 closed.
	$

Before we move on, let's open up one more security group rule for HTTP traffic. It will become clear as to why we're doing this during a later lab:

	$ openstack security group rule create --proto tcp --dst-port 80:80 $SEC_GROUP_ID
	+-------------------+--------------------------------------+
	| Field             | Value                                |
	+-------------------+--------------------------------------+
	| created_at        | 2018-04-09T15:19:08Z                 |
	| description       |                                      |
	| direction         | ingress                              |
	| ether_type        | IPv4                                 |
	| id                | 5190f741-c731-4a23-abf1-adeb911d3719 |
	| name              | None                                 |
	| port_range_max    | 80                                   |
	| port_range_min    | 80                                   |
	| project_id        | f991d44fac91419c8e6016184381871a     |
	| protocol          | tcp                                  |
	| remote_group_id   | None                                 |
	| remote_ip_prefix  | 0.0.0.0/0                            |
	| revision_number   | 0                                    |
	| security_group_id | 69f49751-975f-412b-a21f-765b6e7715a4 |
	| updated_at        | 2018-04-09T15:19:08Z                 |
	+-------------------+--------------------------------------+

##**Networking under the Covers**

So we know that we can connect from our workstation to our instances, demonstrating that security groups (ip filtering/firewalling) is working, and that routing (NAT based L3) is working. But how does it actually work under the covers? Let's briefly explore this - security groups and routing are both performed by iptables, but at different places.

Security groups are implemented by the hypervisor, and assuming the instance has a single network interface, the virtual NIC (a tap device) is attached into a Linux bridge, and filtering is imposed at the bridge level where specific iptables chains are configured for each virtual interface on a per-port basis. Ideally, we wouldn't want to go through a Linux bridge to do the filtering as it's additional overhead and has a slight performance impact; longer term we're looking to implement Open vSwitch based filtering (and OVS based routing) instead.

As mentioned, the chains are constructed based on the security group applied to each port (when we say port we mean Neutron port, i.e. a virtual interface), as such all iptables chains are named after the respective Neutron port-id. As security groups are implemented at the hypervisor, the port we're interested in is the internal network port, i.e. the **internal** network that the instances vNIC is associated to. Remember that the floating IP's are handled by the dedicated network node(s) where the L3 agent resides by default, and the hypervisors or the instances themselves have no visibility into the external routing.

So, let's first remind ourselves of the internal IP address assigned to our instance:

	$ openstack server list -c Name -c Networks
	+-------+--------------------------------------+
	| Name  | Networks                             |
	+-------+--------------------------------------+
	| my_vm | internal=172.16.1.9, 192.168.122.210 |
	+-------+--------------------------------------+

Take the IP address that we've been assigned (yours will be slightly different) and get the Neutron port-id 

	$ export IP_ADDR=172.16.1.5
	$ openstack port list | awk -v ip_addr="$IP_ADDR" '$7 ~ ip_addr {print $2;}'
	6b9ccfcf-91ac-43f5-aa4b-51d455030d40

> **NOTE**: Your IP address may be different, and the port-number will almost certainly be different. Do not copy the 'export' command above, this is just an example based on the test environment used to build this lab.
	
Now that we've got the port-id, we can take a look on the hypervisor itself and dump the iptables chains for that port, noting that the chain names take the first ten characters of the port-id, so in our case "**6b9ccfcf-9**", but as highlighted earlier yours will be different as they're unique identifiers, so make sure to edit the following lines accordingly. We need to establish which host our instance is running on to ensure we execute the commands on the correct node:

	$ openstack server show my_vm -c name -c OS-EXT-SRV-ATTR:hypervisor_hostname
	+-------------------------------------+-----------------------------+
	| Field                               | Value                       |
	+-------------------------------------+-----------------------------+
	| OS-EXT-SRV-ATTR:hypervisor_hostname | summit-compute2.localdomain |         <--- it's on this node.
	| name                                | my_vm                       |
	+-------------------------------------+-----------------------------+

In the above example it shows that our instance is running on 'summit-compute2', but please adjust to your environment, as the scheduler may have chosen 'summit-compute1' instead.

We need to view the individual rules inside of two important chains-

* neutron-openvswi-**i**6b9ccfcf-9 (note highlighted 'i' for input/inbound)
* neutron-openvswi-**o**6b9ccfcf-9 (note highlighted 'o' for output/outbound)

Let's take a look at the rules that exist for the input chain, as we've only created entries in our security groups for inbound traffic, noting that we're remotely executing these commands on the compute node that our instance is running on, adjust this to suit your environment:

	$ ssh root@summit-compute2 iptables -S | grep 6b9ccfcf-9
	-N neutron-openvswi-i6b9ccfcf-9
	-N neutron-openvswi-o6b9ccfcf-9
	-N neutron-openvswi-s6b9ccfcf-9
	(snip)
	-A neutron-openvswi-i6b9ccfcf-9 -p icmp -j RETURN                        <-- here's our icmp rule
	-A neutron-openvswi-i6b9ccfcf-9 -p tcp -m tcp --dport 22 -j RETURN       <-- here's our ssh rule
	-A neutron-openvswi-i6b9ccfcf-9 -p tcp -m tcp --dport 80 -j RETURN       <-- here's our http rule
	(snip)

> **NOTE**: I've slightly modified the output above to show the most important rules as dictated by the security group choices that we made before, primarily allowing port 22/tcp and 80/tcp, and ICMP (ping) from everywhere. Your output will show a lot more rules, primarily for things like allowing DHCP, preventing spoofing, and allowing existing connections. I've annotated the above too.

As you can see, it's relatively easy to view the iptables rules that are governed by Neutron's security group rules. These are dynamically updated upon security group modification so that any security changes are almost instantaneously reflected in the configuration.

Next, let's check out how the NAT-based routing is implemented by the L3 agent on the dedicated network node (**summit-networker**). It's possible to have multple L3 agents for high availability and load sharing, and it's also possible to have the L3 functionality being taken care of by the compute nodes themselves, which we call Distributed Virtual Routing (DVR), but we're using the legacy approach here with a single L3 agent for simplicity.

Earlier in this lab we configured a virtual router, this is what the L3 agent is responsible for creating and maintaining. The virtual routers are scheduled across any nodes within the cluster that are both running the L3 agent daemon, and have connectivity to the external network being requested. Within our environment we only have one L3 agent daemon running on a single system, so we know where the virtual router will be scheduled.

Each virtual router runs within its own network namespace on the host in which it has been schedule to run on, with the namespace being uniquely identified by the ID of the virtual router itself:

	$ openstack router list
	+--------------------------------------+-------------+--------+-------+-------------+-------+----------------------------------+
	| ID                                   | Name        | Status | State | Distributed | HA    | Project                          |
	+--------------------------------------+-------------+--------+-------+-------------+-------+----------------------------------+
	| f8d34761-f57b-4697-8e37-741f274c4ff4 | demo_router | ACTIVE | UP    | False       | False | f991d44fac91419c8e6016184381871a |
	+--------------------------------------+-------------+--------+-------+-------------+-------+----------------------------------+
	
	$ ssh root@summit-networker ip netns list | grep router
	qrouter-808f8855-0e10-4749-ba2f-bff9ebacb102

> **NOTE:** In the above output, note that the qrouter namespace matches to the router ID

Inside of this namespace is where iptables is used to perform both DNAT (destination NAT, i.e. inbound traffic) and SNAT (source NAT, i.e. outbound traffic) based on floating IP's that have been allocated to instances. Note that if a floating IP hasn't been allocated to an instance, SNAT still works but it goes through another,  dedicated IP address that would be shared by multiple instances that don't have their own IP.

If we view the iptables NAT rules within the dedicated network namespace on the networker node, we can see the NAT rules present. First, let's verify the IP addresses that we're working with, both our internal IP and the floating IP that we allocated:

	$ openstack server list -c Name -c Networks
	+-------+--------------------------------------+
	| Name  | Networks                             |
	+-------+--------------------------------------+
	| my_vm | internal=172.16.1.9, 192.168.122.210 |
	+-------+--------------------------------------+

Now, let's look within our namespace, and specifically ask for iptables NAT rules associated with our floating IP (192.168.122.210 in the example environment), noting that we're executing this command remotely on the **summit-networker** machine:

	$ ssh root@summit-networker ip netns exec \
		qrouter-808f8855-0e10-4749-ba2f-bff9ebacb102 \
		iptables -L -n -t nat | grep 192.168.122.210
	
	DNAT       all  --  0.0.0.0/0            192.168.122.210      to:172.16.1.9
	DNAT       all  --  0.0.0.0/0            192.168.122.210      to:172.16.1.9
	SNAT       all  --  172.16.1.9           0.0.0.0/0            to:192.168.122.210

As you can see, there are specific entries for both **DNAT** from floating IP 192.168.122.210 to 172.16.1.9, our internal port, and SNAT from the internal port out to the same external IP.

Before moving onto the next section, clean-up the environment by removing this instance:

	$ openstack server delete my_vm

**NOTE**: There is no output from the delete command, you can run the 'openstack server list' command again to verify that it has been removed.

##**Starting instances via the Dashboard**

The vast majority of this lab guide focusses on the command line, but many tasks can also be completed via the OpenStack Web-based user interface (Horizon). Let's log in and take a look, we'll demonstate the launching of an instance via the dashboard also. Follow these options to complete this section of the lab:

1. Firstly, open up a web-browser and navigate to the Horizon dashboard. 

	The URL for this can be found in the email that you received from RHPDS, see the hyperlink in the middle that looks like this - [http://horizon-REPL.rhpds.opentlc.com/dashboard](http://horizon-REPL.rhpds.opentlc.com/dashboard) (where REPL is your GUID that was allocated to you when we started), once opened you should see the following:

	<img src="images/horizon.png" style="width: 1000px;"/>

2. Login to the dashboard with your own credentials (remembering that you created this username and password, if you need to confirm them, look at the **~/demorc** file we created earlier.
3. Our user has administrative rights, and therefore it automatically positions you to the 'admin' tab of Horizon, but as we're wanting to launch instances, we need to move to the **"Project"** tab by selecting the appropriate tab underneath the URL and next to the "Red Hat OpenStack Platform" image in the top-left hand corner:

	<img src="images/admin.png" style="width: 1000px;"/>

4. Next, select '**Compute**' and then '**Instances**' and you should be provided with an **empty** list of machines (if it lists a machine already running, perhaps you forgot to delete the one that we created earlier?)...

	<img src="images/instances.png" style="width: 1000px;"/>


4. Select 'Launch Instance' in the top-right corner of this page, and you should be presented with a pop-up window:

	<img src="images/launch.png" style="width: 1000px;"/>

5. Next, in the **Details** pane you'll need to provide your instance with a name, I've chosen "**new_vm**", you can ignore the availability zone and instance count here, as the defaults are just fine - we only want one instance, and we only have one availability zone in our environment.

6. In the **Source** pane you'll need to ensure that the '**Boot Source**' is set to '**Image**' as we want to boot from our RHEL7 image that we uploaded previously, and we want to make sure that we have selected **NO** to '**Create New Volume**'. This means that we're asking the OpenStack platform to utilise ephemeral storage, and not persistent storage backed by Cinder (we'll use this in a later lab). You'll also need to ensure that you select the '**rhel7**' image by clicking the "**+**" symbol to the right hand side of the image at the bottom of the pane.

	The completed dialog should look like this:

	<img src="images/source.png" style="width: 1000px;"/>
	
7. Next, move to the '**Flavor**' pane and ensure that our "**m1.labs**" flavor is selected, as shown below:

	<img src="images/flavor.png" style="width: 1000px;"/>
	
8. Now move onto the '**Networks**' pane and you'll see that our one **'internal'** network has already been selected for us by default:

	<img src="images/networks.png" style="width: 1000px;"/>

8. Finally, select '**Launch Instance**' in the bottom right-hand corner of the pop-up window.

9. The machine should now be scheduled and started as a nested virtual machine; the dashboard will monitor progress for you.

	* Spawning/building the machine:

	<img src="images/spawning.png" style="width: 1000px;"/>
	
	* Once successful, the machine should go into an **ACTIVE** state, like we saw before via the CLI:

	<img src="images/active.png" style="width: 1000px;"/>
	
10. In the above image you'll see that there's a drop-down box underneath **Actions** on the right hand side of each instance running. Have a look in here - you can view the startup log via "**View Log**" or view the VNC console via "**Console**":

	<img src="images/actions.png" style="width: 1000px;"/>
	
	Here's an example of the console log being shown directly in the Horizon dashboard:
	
	<img src="images/console-log.png" style="width: 1000px;"/>

11. Finally, once you're happy with how this works, reteurn back to the instances tab and shut down the instance via the same **Actions** drop-down menu, then select **Delete Instance**. You'll be asked to confirm, as this is a permenant deletion, **NOT a shutdown**:

	<img src="images/confirm.png" style="width: 1000px;"/>

> **NOTE**: Make sure that you delete all instances that may be available before proceeding with the next section of this lab.

##**Uploading public keys**

Public keys are used in OpenStack (and other cloud platforms) to uniquely identify a user, avoiding any password requirements. It's also useful when you have passwords installed by users in their VM images but they aren't shared; with a key a user can log-in and change the password to something they're happy with. When an instance is first initialised, one of the options is to select a public key that should be injected into the image at boot time. With the image that we have been using we already know the root password, but it's good practice to demonstrate how we can use them with images that do not permit root login, or don't allow password-based authentication.

Note that for public key injection to work, the guest must have **cloud-init** installed; an in-guest agent that contacts OpenStack's metadata service to check whether there are any tasks for the guest to perform, e.g. public key injection, software installation, arbitrary code execution, etc. For the rest of this lab we're going to be returning to our command line, and the next steps **assume** that you're logged into your OpenStack environment.

We should already have a public key created for us that we can utilise:

	$ cat ~/.ssh/id_rsa.pub
	ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJ3FaJmZqjBD94oRh8SIxqjpWs[...]
	
Next, upload the public keypair into OpenStack - this ensures that Nova has a record of the keypair so it can be injected when required. We'll call our keypair "**my_keypair**":

	$ source ~/demorc
	$ openstack keypair create my_keypair --public-key ~/.ssh/id_rsa.pub
	+-------------+-------------------------------------------------+
	| Field       | Value                                           |
	+-------------+-------------------------------------------------+
	| fingerprint | 9f:f4:27:53:8e:2b:1c:d8:cf:79:1f:ee:79:00:8b:67 |
	| name        | my_keypair                                      |
	| user_id     | 19af400bc0e4445eb96879b1e718072b                |
	+-------------+-------------------------------------------------+


##**Launching an Instance with a Keypair**

Next, launch an instance and ensure that you specify your keypair to use (note that if you do this via the dashboard it will automatically select it if it's the **only** one available):

	$ openstack keypair list
	+------------+-------------------------------------------------+
	| Name       | Fingerprint                                     |
	+------------+-------------------------------------------------+
	| my_keypair | 9f:f4:27:53:8e:2b:1c:d8:cf:79:1f:ee:79:00:8b:67 |
	+------------+-------------------------------------------------+

Let's now boot up another RHEL 7 instance with the previously used flavor, but this time we'll specify our keypair:

	$ openstack server create --flavor m1.labs --image rhel7 --key-name my_keypair my_vm
	(... output omitted...)

Next, request an additional floating IP address for our instance and assign it:

	$ openstack floating ip create external
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-09T21:55:38Z                 |
	| description         |                                      |
	| fixed_ip_address    | None                                 |
	| floating_ip_address | 192.168.122.206                      |
	| floating_network_id | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| id                  | d82569c0-631a-48d3-a39c-83e208498479 |
	| name                | 192.168.122.206                      |
	| port_id             | None                                 |
	| project_id          | f991d44fac91419c8e6016184381871a     |
	| revision_number     | 0                                    |
	| router_id           | None                                 |
	| status              | DOWN                                 |
	| updated_at          | 2018-04-09T21:55:38Z                 |
	+---------------------+--------------------------------------+

	$ openstack server add floating ip my_vm 192.168.122.206
	
> **NOTE**: Your IP address may vary from the output shown above, use the IP as listed when requesting the IP address

If the metadata server worked correctly you should be able to **ssh** into the machine without requiring the password. If you cannot login, ensure that you associated your instance with the keypair as above.

	$ ssh root@192.168.122.206
	The authenticity of host '192.168.122.206 (192.168.122.206)' can't be established.
	ECDSA key fingerprint is SHA256:ALlJOwHLgobeacRyYpkkq+NLQHBGmASeaF2qdoKf+yI.
	ECDSA key fingerprint is MD5:c1:26:eb:d2:d1:dd:64:87:35:b5:9c:2d:6c:48:12:ea.
	Are you sure you want to continue connecting (yes/no)? yes
	Warning: Permanently added '192.168.122.206' (ECDSA) to the list of known hosts.
	[root@my-vm ~]#

	[root@my-vm ~]# uname -a
	Linux my-vm 3.10.0-693.el7.x86_64 #1 SMP Thu Jul 6 19:56:57 EDT 2017 x86_64 x86_64 x86_64 GNU/Linux
	
Let's now delete this machine now we've confirmed the metadata service and keypair injection is working. You'll need to disconnect from this machine first:

	# exit
	logout
	Connection to 192.168.122.206 closed.
	$
	
Then, delete the machine, remembering that unless there's an error, this call provides no output:

	$ openstack server delete my_vm

##**Executing boot-time scripts**

In addition to injecting public keys, the **cloud-init** guest agent can request executable code (and other instructions) from OpenStack's metadata service. In this section of the lab we're going to provide a demonstration of an instance retrieving a script and executing it at boot time, and we'll demonstrate exactly how **cloud-init** and the metadata service works.

First, we need a script to execute, let's create one that makes some basic system calls and writes the output to a text file on the instance's filesystem:

	$ cat > ~/user-script.sh << EOF
	#!/usr/bin/env bash
	uname -a > /tmp/uname
	date > /tmp/date
	EOF

Next, create another instance, but pass the script as **user-data** to the instance with the parameter **"--user-data"**:

	$ openstack server create --flavor m1.labs --image rhel7 \
		--key-name my_keypair --user-data user-script.sh my_vm
	(...)
	
> **NOTE**: It's also possible to inject user-data via the OpenStack Dashboard

Now, create and assign a floating IP so that we can check that our script worked...

	$ openstack floating ip create external
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-09T21:58:55Z                 |
	| description         |                                      |
	| fixed_ip_address    | None                                 |
	| floating_ip_address | 192.168.122.202                      |
	| floating_network_id | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| id                  | 05138ed3-632b-49b3-8070-d6d8cfb87cfa |
	| name                | 192.168.122.202                      |
	| port_id             | None                                 |
	| project_id          | f991d44fac91419c8e6016184381871a     |
	| revision_number     | 0                                    |
	| router_id           | None                                 |
	| status              | DOWN                                 |
	| updated_at          | 2018-04-09T21:58:55Z                 |
	+---------------------+--------------------------------------+

	$ openstack server add floating ip my_vm 192.168.122.202
	
> **NOTE**: Your IP address may vary from the output shown above, use the IP as listed when requesting the IP address

Once your machine goes active, you may have to wait a little while for it to actually boot up. You can attempt to SSH into it (noting that we also asked it to inject our keypair) with the following command, if it gives you the following error, the machine is still booting up:

	$ ssh root@192.168.122.202
	ssh: connect to host 192.168.122.202 port 22: Connection refused

Once you're able to successfully SSH into your machine, we can check for success:

	$ ssh root@192.168.122.202
	The authenticity of host '192.168.122.202 (192.168.122.202)' can't be established.
	ECDSA key fingerprint is SHA256:yHv+iwMxE+g9BlEQX3aiOM+FJNfJ0jPdu+eQ/OMx3nI.
	ECDSA key fingerprint is MD5:be:32:63:36:c7:50:18:ca:14:bd:bf:eb:99:92:06:07.
	Are you sure you want to continue connecting (yes/no)? yes
	Warning: Permanently added '192.168.122.202' (ECDSA) to the list of known hosts.
	[root@my-vm ~]#

	[root@my-vm ~]# cat /tmp/date; cat /tmp/uname
	Mon Apr  9 17:59:21 EDT 2018
	Linux my-vm 3.10.0-693.el7.x86_64 #1 SMP Thu Jul 6 19:56:57 EDT 2017 x86_64 x86_64 x86_64 GNU/Linux

> **NOTE**: If you receive any empty files, or not files not found, it's likely that the user data was not retrieved successfully.

So, our script ran successfully, but how does the user-data get delivered to the instance? Well, cloud-init is designed to work across multiple cloud platforms, OpenStack, EC2, etc, all of which support the metadata service. This metadata service "listens" on a specific IP address **169.254.169.254**, which we can interrogate from our instance to see what data it provides. **cloud-init** simply provides a mechanism and hierarchy for executing and applying data that is found within the metadata service. The metadata service provides instance-specific data by uniquely identifying each machine.

Assuming that you're still connected to your instance, run the following commands to demonstrate the metadata functionality (**within our instance**):

	[root@my-vm ~]# curl http://169.254.169.254
	1.0
	2007-01-19
	2007-03-01
	2007-08-29
	2007-10-10
	2007-12-15
	2008-02-01
	2008-09-01
	2009-04-04
	latest
	
	[root@my-vm ~]# curl http://169.254.169.254/latest
	meta-data/
	user-data
	
	[root@my-vm ~]# curl http://169.254.169.254/latest/user-data
	#!/bin/sh
	uname -a > /tmp/uname
	date > /tmp/date
	
Now we can see where our script came from. **cloud-init** simply downloads the user-data and executes it as part of a module. You can check out **/etc/cloud/cloud.cfg** to see what modules this instance is configured to use.

For now, disconnect from the machine, but **don't delete it**, we'll need this machine for a later lab:

	[root@my-vm ~]# exit
	logout
	Connection to 192.168.122.202 closed.
	$
