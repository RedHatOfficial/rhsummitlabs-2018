#**Lab 8: Deployment of Load Balancers using Neutron's LBaaS feature**

##**Introduction**

Today, LBaaS (Load Balancing-as-a-Service) is part of Neutron as an advanced extra component, providing both proprietary and open source load balancing technologies to end users via self-service. A user can simply request a load balancer via Neutron's RESTful API and Neutron is responsible for managing the lifecycle of such a load balancer to distribute requests amongst a group of instances.

In this lab we'll deploy a load balancer with the default **HAproxy** based backend, which is the default in Red Hat OpenStack Platform. We'll deploy multiple instances that we'll load balance across to demonstrate functionality, and to show how LBaaS can recover from situations in which a node may cease to exist.

##**Deploying Multiple Machines**

In the previous lab we deployed a single node with an httpd server (and a welcome message) via OpenStack Heat. For our LBaaS lab we'll need more than one machine to demonstrate its capabilities. Heat supports the concept of "**resource groups**" which allows us to deploy multiple OpenStack resources of the same type by simply specifying a quantity.

Via a simple Heat template we can point to our original stack template and call it multiple times using this concept, making it easy for us to deploy multiple machines. We've provided this file for you, and it looks like the following:

	$ cat ~/labs/osp/multiple-instances.yaml
	heat_template_version: 2013-05-23
	
	resources:
	  resource_group:
	    type: OS::Heat::ResourceGroup
	    properties:
	      count: 2
	      resource_def: {type: heat-demo.yaml}

This stack requests **two** (see **count**) of the stacks detailed in **heat-demo.yaml** (our original stack template) to form the constuct of **nested stacks**. We simply need to feed in the **multiple-instances.yaml** file to Heat, and it'll take care of pulling in the nested stack templates and build out the resources.

Let's deploy our nested Heat stack. As before, we need to provide our **key_name** parameter, but this time we'll use an environment file. An environment file allows us to provide parameters within a file, allowing configuration to become repeatable, portable, and versionable, just like the templates. You may remember that the message said "Hello, Anonymous", we can override this parameter at the same time as the name of the keypair to us.

But first, we need to make sure that we include **your** name (replace with your name):

	$ export MY_NAME=<your name>

	$ cat > ~/summit.yaml << EOF
	parameter_defaults:
	  key_name: my_keypair
	  my_name: $MY_NAME
	EOF

Now we can launch our multiple instances Heat stack:

	$ openstack stack create --template ~/labs/osp/multiple-instances.yaml \
		--environment ~/summit.yaml multiple
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| id                  | 5c8072ec-4ea4-4bea-a1e1-e9094723bbf9 |
	| stack_name          | multiple                             |
	| description         | No description                       |
	| creation_time       | 2018-04-09T22:20:40Z                 |
	| updated_time        | None                                 |
	| stack_status        | CREATE_IN_PROGRESS                   |
	| stack_status_reason | Stack CREATE started                 |
	+---------------------+--------------------------------------+

Once this has turned to a **CREATE_COMPLETE** state, we need to validate that multiple instances have been started (if you're reading this with the printed version, I apologise for the formatting):

	$ openstack stack list
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	| ID                                   | Stack Name | Project                          | Stack Status    | Creation Time        | Updated Time |
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	| 5c8072ec-4ea4-4bea-a1e1-e9094723bbf9 | multiple   | f991d44fac91419c8e6016184381871a | CREATE_COMPLETE | 2018-04-09T22:20:40Z | None         |
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	
	$ openstack server list
	+--------------------------------------+-------------------------------------------------------+--------+---------------------------------------+-------+---------+
	| ID                                   | Name                                                  | Status | Networks                              | Image | Flavor  |
	+--------------------------------------+-------------------------------------------------------+--------+---------------------------------------+-------+---------+
	| ebe48c72-ceec-4473-812c-c7171aac01bd | mu-cgmawqm6yiy-0-eoiv432ojknr-summit_srv-rxqhqfrgiq2h | ACTIVE | internal=172.16.1.9, 192.168.122.203  | rhel7 | m1.labs |
	| e1362b6d-b6d8-475b-92f6-001ecc0ed598 | mu-cgmawqm6yiy-1-2i2kn4aj3v5y-summit_srv-wjqclkn66vi7 | ACTIVE | internal=172.16.1.12, 192.168.122.205 | rhel7 | m1.labs |
	+--------------------------------------+-------------------------------------------------------+--------+---------------------------------------+-------+---------+

As before, we can test these instances with the **server-check.sh** script to ensure that firstly the software deployment and configuration was completed, and secondly that it took our name as configured. For this we'll need to take the floating IP's as per the output from '**openstack server list**', noting that your IP's will differ from the ones shown in the example below:

	$ sh ~/labs/osp/server-check.sh 192.168.122.203

	INFO: Using http://192.168.122.203:80... (Ctrl-c to stop)
	
	
	Mon  9 Apr 18:25:10 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-0-eoiv432ojknr-summit-srv-rxqhqfrgiq2h @ 172.16.1.9
	
	
	Mon  9 Apr 18:25:13 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-0-eoiv432ojknr-summit-srv-rxqhqfrgiq2h @ 172.16.1.9
	
	
	^C
	
Next try the other server:
	
	$ sh ~/labs/osp/server-check.sh 192.168.122.205

	INFO: Using http://192.168.122.205:80... (Ctrl-c to stop)
	
	
	Mon  9 Apr 18:25:52 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-1-2i2kn4aj3v5y-summit-srv-wjqclkn66vi7 @ 172.16.1.12
	
	
	Mon  9 Apr 18:25:55 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-1-2i2kn4aj3v5y-summit-srv-wjqclkn66vi7 @ 172.16.1.12
	
	
	^C

> **NOTE**: Your IP addresses will very likely be different to the ones in the lab guide, and you can use **Ctrl-C** to stop the server-check script. You may get some errors where it says that it cannot communicate with the server. It may be the case that the machines are still installing the required packages.
	
If both of these attempts succeeded, we know that both of the machines are up, and responding individually.

##**Creating a Loadbalancer**

Now we're actually going to create a load balancer that will balance traffic between our two instances that we launched via our Heat template. OpenStack Neutron has a built-in mechanism for automating the creation of load balancers and relies on a set of backend plugins for implementation. Out of the box, we ship with a HAproxy implementation and is what we're going to be using in this lab, although there are other plugins that organisations may want to use, e.g. to leverage existing investments from the likes of F5, etc.

The high level architecture looks like this:

<br>
<img src="images/lbaasv2-diagram.png" style="width: 1000px;"/>
<br><br>

> **NOTE**: The following instructions utilise the legacy Neutron command line tooling rather than the OpenStack command line tooling, this is only because certain syntax is not yet supported in the OpenStack command line replacement, hence we're falling back to the Neutron CLI for these steps only.

The first step is to create the top-level **load balancer** construct. This will allow us to select the type of loadbalancer that we want to create based on the backend and will allow us to choose the network in which we want the load balancer to live in. For our use-case we want to use HAproxy, and therefore given it's the default we don't need to specify it on the commandline, and for the network we want to select our **internal_subnet**:

	$ neutron lbaas-loadbalancer-create --name demo_lb internal_subnet
	
	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new loadbalancer:
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| admin_state_up      | True                                 |
	| description         |                                      |
	| id                  | 18868c2f-3e5d-4a75-a568-f8bae2d7518c |
	| listeners           |                                      |
	| name                | demo_lb                              |
	| operating_status    | OFFLINE                              |
	| pools               |                                      |
	| provider            | haproxy                              |
	| provisioning_status | PENDING_CREATE                       |
	| tenant_id           | f991d44fac91419c8e6016184381871a     |
	| vip_address         | 172.16.1.13                          |
	| vip_port_id         | 726869e5-c519-41cc-abc0-868f885cb3f0 |
	| vip_subnet_id       | 4d38f43b-402e-48a5-b048-3092d6f7da02 |
	+---------------------+--------------------------------------+

> **NOTE**: Ignore the Neutron CLI warnings; the most important item above is to see the VIP that was created on the internal network, 172.16.1.0/24.

Next we'll need to create a **listener** for our load balancer, a listener is used to define what it is that we're load balancing, for example, on which port, over which protocol, and also some additional configurables such as connection limits. As we're going to be balancing requests between our two machines that are utilising port 80 for their web content (over HTTP) we need to build a listener as appropriate:

	$ neutron lbaas-listener-create --name demo_lb_http \
		--loadbalancer demo_lb --protocol HTTP --protocol-port 80
		
	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new listener:
	+---------------------------+------------------------------------------------+
	| Field                     | Value                                          |
	+---------------------------+------------------------------------------------+
	| admin_state_up            | True                                           |
	| connection_limit          | -1                                             |
	| default_pool_id           |                                                |
	| default_tls_container_ref |                                                |
	| description               |                                                |
	| id                        | ece3a993-bdfc-4ca1-96d3-23cfb0c9346d           |
	| loadbalancers             | {"id": "18868c2f-3e5d-4a75-a568-f8bae2d7518c"} |
	| name                      | demo_lb_http                                   |
	| protocol                  | HTTP                                           |
	| protocol_port             | 80                                             |
	| sni_container_refs        |                                                |
	| tenant_id                 | f991d44fac91419c8e6016184381871a               |
	+---------------------------+------------------------------------------------+

Now we need to create a **pool** for our listener. A pool allows us to associate a set of **members**, i.e. the actual systems we want to load balance across and the method in which we balance, e.g. round-robin, or perhaps least number of connections. By default it selects round-robin based balancing, i.e. send each request to each node in sequence, ignoring the load-level on each node. Let's create our pool:

	$ neutron lbaas-pool-create --name demo_lb_pool_http \
		--lb-algorithm ROUND_ROBIN --listener demo_lb_http --protocol HTTP
		
	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new pool:
	+---------------------+------------------------------------------------+
	| Field               | Value                                          |
	+---------------------+------------------------------------------------+
	| admin_state_up      | True                                           |
	| description         |                                                |
	| healthmonitor_id    |                                                |
	| id                  | 19ca4761-ab73-4577-8a0a-6a8fc3d8e421           |
	| lb_algorithm        | ROUND_ROBIN                                    |
	| listeners           | {"id": "ece3a993-bdfc-4ca1-96d3-23cfb0c9346d"} |
	| loadbalancers       | {"id": "18868c2f-3e5d-4a75-a568-f8bae2d7518c"} |
	| members             |                                                |
	| name                | demo_lb_pool_http                              |
	| protocol            | HTTP                                           |
	| session_persistence |                                                |
	| tenant_id           | f991d44fac91419c8e6016184381871a               |
	+---------------------+------------------------------------------------+

We've created our pool, but before we add our **members** we need to define how the pool should identify whether a node has failed or not, and how to avoid sending requests to any failed nodes. For this we need to create a **health monitor**. There are a number of ways of configuring the monitor depending on the type of application you're wanting to load balance. For example, you could perform a simple **ping** check, this pings each node to see if it's up, the problem with this test is that a ping doesn't tell you whether the application itself is running, just that the underlying node is responding; therefore, if the application dies, the load balancer will still point clients to it, which is not ideal. Therefore we should focus on application-specific checks, and utilise some of the other available checks such as a simple HTTP (or others) connection which should validate whether the application itself is running.

As our application is HTTP based, we're going to use the **http** test, as it's the most simple to demonstrate functionality. We're going to be using the following characteristics:

* A **delay** of 5 seconds
* A **timeout** of 5 seconds
* A max **retries** of 2
* Using the HTTP protocol to check node availability

This ensures that we check each node every 5 seconds, with a maximum timeout of 5000ms, and to attempt this two times before we assume that the node has be lost, and to no longer send any requests to it. Let's create the monitor based on these characteristics:

	$ neutron lbaas-healthmonitor-create --name demo_lb_http_monitor \
		--delay 5 --max-retries 2 --timeout 5 --type HTTP --pool demo_lb_pool_http
		
	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new healthmonitor:
	+------------------+------------------------------------------------+
	| Field            | Value                                          |
	+------------------+------------------------------------------------+
	| admin_state_up   | True                                           |
	| delay            | 5                                              |
	| expected_codes   | 200                                            |
	| http_method      | GET                                            |
	| id               | ff371340-0f1c-45b4-bc26-9f62244828a1           |
	| max_retries      | 2                                              |
	| max_retries_down | 3                                              |
	| name             | demo_lb_http_monitor                           |
	| pools            | {"id": "19ca4761-ab73-4577-8a0a-6a8fc3d8e421"} |
	| tenant_id        | f991d44fac91419c8e6016184381871a               |
	| timeout          | 5                                              |
	| type             | HTTP                                           |
	| url_path         | /                                              |
	+------------------+------------------------------------------------+

Now we need to add our members to our pool, recall that the members are the instances/nodes themselves that we want to load balance across. We need to get the IP addresses of our two nodes on the **internal** network, lets quickly get these from Nova:


	$ openstack server list -c ID -c Networks
	+--------------------------------------+---------------------------------------+
	| ID                                   | Networks                              |
	+--------------------------------------+---------------------------------------+
	| ebe48c72-ceec-4473-812c-c7171aac01bd | internal=172.16.1.9, 192.168.122.203  |
	| e1362b6d-b6d8-475b-92f6-001ecc0ed598 | internal=172.16.1.12, 192.168.122.205 |
	+--------------------------------------+---------------------------------------+

In my environment you can see that we have **172.16.1.9**, and **172.16.1.12**. These are the two IP addresses that I need to add to the pool, and will become the two nodes that the health monitor will continually poll to ensure that only alive nodes are sent traffic. To save some time in creating the members, you can export these environment variables:

	$ export NODE1_INTERNAL=172.16.1.9
	$ export NODE2_INTERNAL=172.16.1.12

> **NOTE**: You'll need to specify **your** node IP's in the above command.

Next, create the members, making sure that we specify the subnet that they reside in, the internal port that they're listening on (for us the port doesn't deviate from the default port 80) and the pool that they're to be attached to (**demo_lb_pool_http**):

	$ neutron lbaas-member-create --name demo_lb_member1 --subnet internal_subnet \
		--address $NODE1_INTERNAL --protocol-port 80 demo_lb_pool_http

	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new member:
	+----------------+--------------------------------------+
	| Field          | Value                                |
	+----------------+--------------------------------------+
	| address        | 172.16.1.9                           |
	| admin_state_up | True                                 |
	| id             | f8ad7143-67c9-42a2-8bc9-b8da0afa3d1a |
	| name           | demo_lb_member1                      |
	| protocol_port  | 80                                   |
	| subnet_id      | 4d38f43b-402e-48a5-b048-3092d6f7da02 |
	| tenant_id      | f991d44fac91419c8e6016184381871a     |
	| weight         | 1                                    |
	+----------------+--------------------------------------+
	
	$ neutron lbaas-member-create --name demo_lb_member2 --subnet internal_subnet \
		--address $NODE2_INTERNAL --protocol-port 80 demo_lb_pool_http

	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	Created a new member:
	+----------------+--------------------------------------+
	| Field          | Value                                |
	+----------------+--------------------------------------+
	| address        | 172.16.1.12                          |
	| admin_state_up | True                                 |
	| id             | 7d11c195-a409-4cb7-bf3f-6cd6ff719049 |
	| name           | demo_lb_member2                      |
	| protocol_port  | 80                                   |
	| subnet_id      | 4d38f43b-402e-48a5-b048-3092d6f7da02 |
	| tenant_id      | f991d44fac91419c8e6016184381871a     |
	| weight         | 1                                    |
	+----------------+--------------------------------------+

Now we need to add a virtual IP for our loadbalancer that listens on the external network. This will provide a **frontend** virtual IP address to load balance across the nodes on the **internal** network (we won't be able to access the load balancer from the outside external network yet). The first step is to get the internal network Neutron port ID in which our load balancer is running on. The easiest way to get this port-id is to query the load-balancer object itself:

	$ neutron lbaas-loadbalancer-show demo_lb
	neutron CLI is deprecated and will be removed in the future. Use openstack CLI instead.
	+---------------------+------------------------------------------------+
	| Field               | Value                                          |
	+---------------------+------------------------------------------------+
	| admin_state_up      | True                                           |
	| description         |                                                |
	| id                  | 18868c2f-3e5d-4a75-a568-f8bae2d7518c           |
	| listeners           | {"id": "ece3a993-bdfc-4ca1-96d3-23cfb0c9346d"} |
	| name                | demo_lb                                        |
	| operating_status    | ONLINE                                         |
	| pools               | {"id": "19ca4761-ab73-4577-8a0a-6a8fc3d8e421"} |
	| provider            | haproxy                                        |
	| provisioning_status | ACTIVE                                         |
	| tenant_id           | f991d44fac91419c8e6016184381871a               |
	| vip_address         | 172.16.1.13                                    |
	| vip_port_id         | 726869e5-c519-41cc-abc0-868f885cb3f0           |
	| vip_subnet_id       | 4d38f43b-402e-48a5-b048-3092d6f7da02           |
	+---------------------+------------------------------------------------+

The ID we need here is **vip_port_id**, in my case this is **726869e5-c519-41cc-abc0-868f885cb3f0**. So, let's create a floating IP and associate it directly to this port:

	$ openstack floating ip create external --port 726869e5-c519-41cc-abc0-868f885cb3f0
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| created_at          | 2018-04-09T22:33:51Z                 |
	| description         |                                      |
	| fixed_ip_address    | 172.16.1.13                          |
	| floating_ip_address | 192.168.122.201                      |
	| floating_network_id | d5bfe8ac-d26c-4db3-b9ca-6459426f6362 |
	| id                  | e11c06cc-4b2d-4b3a-a912-13fd55cd589a |
	| name                | 192.168.122.201                      |
	| port_id             | 726869e5-c519-41cc-abc0-868f885cb3f0 |
	| project_id          | f991d44fac91419c8e6016184381871a     |
	| revision_number     | 0                                    |
	| router_id           | f8d34761-f57b-4697-8e37-741f274c4ff4 |
	| status              | DOWN                                 |
	| updated_at          | 2018-04-09T22:33:51Z                 |
	+---------------------+--------------------------------------+

In my case, this means that any inbound requests on my floating IP (**192.168.122.201**) should be directed to the load-balancers VIP (**172.16.1.13**).

##**Testing our Load Balancer**

Now we can test to see whether our load balancer is working as expected. Let's fire up our **server-check** script like before but provide it with the new floating IP we just associated:

	$ sh ~/labs/osp/server-check.sh 192.168.122.201
	INFO: Using http://192.168.122.201:80... (Ctrl-c to stop)


	Mon  9 Apr 18:35:18 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-1-2i2kn4aj3v5y-summit-srv-wjqclkn66vi7 @ 172.16.1.12
	
	
	Mon  9 Apr 18:35:21 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-0-eoiv432ojknr-summit-srv-rxqhqfrgiq2h @ 172.16.1.9
	
	
	Mon  9 Apr 18:35:24 EDT 2018
	Hello, Rhys, I am mu-cgmawqm6yiy-1-2i2kn4aj3v5y-summit-srv-wjqclkn66vi7 @ 172.16.1.12
	
	
	^C

> **NOTE**: Replace this IP address with your own as per the floating IP it created for you earlier.

The output of which shows that it's load balancing between both of the nodes at every connection attempt (it's doing this because we selected the **ROUND_ROBIN** allocation mechanism; other options would have likely confused us when it kept redirecting us to the **same** node.

Let's make sure that from a client perspective we're not interrupted by the loss of a node. Let's shut down on of our nodes. Firstly, bring up your Horizon dashboard again, recalling that the URL for which can be found [here](https://www.opentlc.com/guidgrabber/guidgrabber.cgi). You may have to login again using your **user** credentials, or it may let you proceed based on the existing login if your session hasn't already timed out:

<img src="images/horizon.png" style="width: 1000px;"/>

The first thing you should see when you navigate to the **project** tab, and then navigate to the **Compute** tab, and **Instances** sub-tab, is the two virtual machines that were created by Heat in the previous step:

<img src="images/two-instances.png" style="width: 1000px;"/>

Let's shut the first node off - select the **Actions** menu for the first node in the list and select **"Shut Off Instance"** (this won't delete the instance, it will merely turn it off):

<br>
<img src="images/shut-off.png" style="width: 1000px;" border=0/>

You'll be asked to confirm that you definitely want to do this, select "Shut Off Instance":

<br>
<img src="images/confirm-shutdown.png" style="width: 1000px;" border=0/>

Now, if you immediately re-run your server-check script, you'll notice that the first few connections will likely swap between the two, but once the other node has been safely shut down, the load balancer starts pointing all requests to the only surviving node (in our case **172.16.1.12**), showing that our monitor is working properly and that our load balancer is able to recover efficiently with minimal client problems or downtime:

<br>
<img src="images/server-check-fail.png" style="width: 1200px;" border=0/>

##**Cleaning up**

That's it! We're done! We need to clean up the Heat stack, and we'll take care of the rest before some of the other labs can proceed later in the day.

	$ openstack stack delete multiple
	Are you sure you want to delete this stack(s) [y/N]? y

Thank you very much for attending this lab, I hope that it gave you a bit of insight into how to use OpenStack, how its components fit together and how it all works with practical examples. If you have any feedback please share it with us, and if there's anything we can do to assist you in your OpenStack journey, please don't hesitate to ask!
