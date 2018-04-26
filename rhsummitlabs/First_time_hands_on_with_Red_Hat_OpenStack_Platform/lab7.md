#**Lab 7: Deployment of Application Stacks using OpenStack Orchestration (Heat)**

##**Introduction**

OpenStack Heat is a feature that was originally designed as an orchestration engine, allowing organisations to pre-define which OpenStack resources to deploy for a given application. One would simply define a stack template, typically a set of yaml formatted text documents, outlining the requirement for a number of infrastructure resources (for example, instances, networks, floating IP's, storage volumes) along with a set of parameters for configuration. Heat would deploy the resources based on a given dependency chain, in other words, which resources need to be built before the others. Heat can then monitor such resources for availability, and scale them out where necessary. These templates enable application stacks to become portable and to achieve repeatability and predictability.

In this lab we'll deploy a simple resource-based stack, comprising of a single server, a port on our existing internal network, a floating IP address on the external network, and to install and configure a web server for us, based on packages exported from our workstation.

##**Getting the Heat Stack Definition**

For this lab, the instructor has provided a pre-defined template. It can be deployed either using the dashboard or via the command line tools. This lab will guide you through deploying via the command line. The file has already been placed within the home directory (**~/heat-demo.yaml**). If you don't have this file in your home directory, please let us know.

For your reference, the file contents look like the following:

	$ cat ~/labs/osp/heat-demo.yaml
	heat_template_version: 2015-04-30

	description: Red Hat Summit 2018 - Lab1009
	
	parameters:
	  key_name:
	    type: string
	    description:
	      Name of an existing key pair to enable SSH access to the instance.
	    default: default
	  image:
	    type: string
	    description: ID of the image to use for the instance to be created.
	    default: rhel7
	  instance_type:
	    type: string
	    description: Type of the instance to be created.
	    default: m1.labs
	  public_net:
	    type: string
	    description: >
	      Public network for which floating IP addresses will be allocated
	    default: external
	  private_net:
	    type: string
	    description: Private network into which servers get deployed
	    default: internal
	  private_subnet:
	    type: string
	    description: Private subnet into which servers get deployed
	    default: internal_subnet
	  my_name:
	    type: string
	    description: Enter your name here
	    default: Anonymous
	
	resources:
	  summit_srv:
	    type: OS::Nova::Server
	    properties:
	      key_name: { get_param: key_name }
	      image: { get_param: image }
	      flavor: { get_param: instance_type }
	      networks:
	        - port: { get_resource: summit_port }
	      user_data_format: RAW
	      user_data:
	        get_resource: summit_config
	
	  summit_port:
	    type: OS::Neutron::Port
	    properties:
	      network: { get_param: private_net }
	      fixed_ips:
	        - subnet: { get_param: private_subnet }
	      security_groups: [{ get_resource: summit_secgroup }]
	
	  summit_floating_ip:
	    type: OS::Neutron::FloatingIP
	    properties:
	      floating_network: { get_param: public_net }
	      port_id: { get_resource: summit_port }
	
	  summit_secgroup:
	    type: OS::Neutron::SecurityGroup
	    properties:
	      description: Add security group rules for server
	      name: heat-stack-secgroup
	      rules:
	        - remote_ip_prefix: 0.0.0.0/0
	          protocol: tcp
	          port_range_min: 22
	          port_range_max: 22
	        - remote_ip_prefix: 0.0.0.0/0
	          protocol: tcp
	          port_range_min: 80
	          port_range_max: 80
	        - remote_ip_prefix: 0.0.0.0/0
	          protocol: icmp
	
	  summit_config:
	    type: OS::Heat::SoftwareConfig
	    properties:
	      group: ungrouped
	      config:
	        str_replace:
	          template: |
	            #!/usr/bin/env bash
	            curl -o /etc/yum.repos.d/packages.repo http://192.168.122.253/packages.repo
	            yum install httpd -y
	            systemctl enable httpd
	            systemctl start httpd
	            MY_IP=$(/sbin/ifconfig eth0 | grep 'inet ' | awk '{ print $2}')
	            echo "Hello, $my_name, I am $(hostname -s) @ $MY_IP" > /var/www/html/index.html
	          params:
	            $my_name: {get_param: my_name}
	
	outputs:
	  instance_ip:
	    description: Public IP address of the newly created Nova instance.
	    value: { get_attr: [ summit_floating_ip, floating_ip_address ] }

##**What the Stack Definition does**

For those not familiar with the template language (HOT) used within OpenStack Heat, I suggest you take a look at the file, specifically where it lists the images used, the resources it requires, and any script data that the Heat tools execute upon boot. You will notice that the Heat definition deploys the following elements-

* A new security group to allow ICMP, TCP on port 22 (SSH), and TCP on port 80 (HTTP)
* An instance with a configuration script (that gets uploaded into the user-data)
* Finally, a floating IP address for external access

##**Deploying our Stack**

As you may have seen, each parameter in the Heat stack has a **default** value, which has been set according to the environment that you've been using, e.g. the name of the flavor to use, the name of the external network (for floating IP's), and the name of the image. Therefore, deploying this stack is very straight forward. We have, however, made a deliberate mistake in the template to demonstrate how parameters can be overridden via the CLI; we've got the keyname "default" listed as the default keypair to inject, but this doesn't exist. If you recall, we created one called "my_keypair", let's deploy our stack via Heat and specify the new keypair:

	$ source ~/demorc

	$ openstack stack create --template ~/labs/osp/heat-demo.yaml --parameter key_name=my_keypair demo
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| id                  | 5db87e43-6ed8-43c6-af89-a1adfebbe285 |
	| stack_name          | demo                                 |
	| description         | Red Hat Summit 2018 - Lab1009        |
	| creation_time       | 2018-04-09T22:15:31Z                 |
	| updated_time        | None                                 |
	| stack_status        | CREATE_IN_PROGRESS                   |
	| stack_status_reason | Stack CREATE started                 |
	+---------------------+--------------------------------------+

> **NOTE:** If you omit the **key_name** parameter, the stack will fail to create.
	
After a few minutes, it should have completed, you can view the progress using the following-

	$ openstack stack list
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	| ID                                   | Stack Name | Project                          | Stack Status    | Creation Time        | Updated Time |
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	| 5db87e43-6ed8-43c6-af89-a1adfebbe285 | demo       | f991d44fac91419c8e6016184381871a | CREATE_COMPLETE | 2018-04-09T22:15:31Z | None         |
	+--------------------------------------+------------+----------------------------------+-----------------+----------------------+--------------+
	
Once your stack has deployed successfully (with the stack status at **"CREATE_COMPLETE"**), simply use the following to find the output IP address for your instance. Within a Heat template you can specify a number of **outputs** that you can query Heat for, saving you from using other tooling to get the data you requested:

	$ openstack stack show demo | grep output_value
	|                       |   output_value: 192.168.122.205                                                                                                                  

In my example, the floating IP address that was allocated for my machine is **192.168.122.205**, you may receive a different one. Let's curl this machine on the http port and see what it displays:

	$ curl http://192.168.122.205
	Hello, Anonymous, I am demo-summit-srv-2d4cplvtrw5k @ 172.16.1.12

This has shown us that our script has worked properly (to install httpd and present a welcome message saying which server and IP address this machine is) and that our stack deployment was successful. An alternative way of checking that the machine is running successfully is to use the script **"~/labs/osp/server-check.sh"** in your home directory (**Ctrl-C** to stop it):

	$ sh ~/labs/osp/server-check.sh 192.168.122.205
	INFO: Using http://192.168.122.205:80... (Ctrl-c to stop)

	Tue  4 Apr 11:53:49 EDT 2017
	Hello, Anonymous, I am demo-summit-srv-2d4cplvtrw5k @ 172.16.1.12

The reason why this is a repeating script will become obvious in the next lab when we look at utilising load balancers. Finally, clean-up your deployment:

	$ openstack stack delete demo
	Are you sure you want to delete this stack(s) [y/N]? y

> **NOTE**: Make sure you enter 'y' above, otherwise it won't clean up the existing resources.