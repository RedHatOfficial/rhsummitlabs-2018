#**Lab 6: Customised Setup (Roles and Environmental)**

One of the most important components within OSP director is OpenStack Heat, a feature that was originally designed as an orchestration engine, allowing organisations to pre-define which OpenStack resources to deploy for a given application. One would simply define a stack template, typically a set of **yaml** formatted text documents, outlining the requirement for a number of infrastructure resources (for example, instances, networks, storage volumes) along with a set of parameters for configuration. Heat would then deploy the resources based on a given dependency chain, in other words, which resources need to be built before the others. Heat can then monitor such resources for availability, and scale them out where necessary. These templates enable application stacks to become portable and to achieve repeatability and predictability.

Heat is used extensively within OSP director as the core orchestration engine for overcloud deployment.  Heat takes care of the provisioning and management of any required resources, such as physical servers, OpenStack software deployment and configuration, and physical networking setup. By providing a set of stack templates to Heat, we can describe the overcloud environment in intimate detail, including quantities and any necessary configuration parameters. It also makes the templates versionable and programmatically understood - the instructions for deploying everything necessary are listed within the templates, you need only provide it with the parameters to suit the deployment environment.

In this lab section we're going to start thinking about what our overcloud deployment is going to look like. Up until this point we've been preparing our nodes, ensuring that they're ready to become their eventual roles, but we've not thought about how to customise the default or vanilla configuration one would get right out of the box. It's here that we're going to start looking at modifying the Heat templates that will be used for overcloud deployment. Rather than modifying the core templates themselves, a common way to provide modifications to an OSP director deployment is to create an **environment** file, one that will override the default options and parameters for that specific environment.

# Creating a Dedicated Networker Role

Since OSP10 (based on OpenStack Newton), OSP director supports **composable roles** - this allows organisations to break down the legacy OpenStack roles, such as a controller, into individual services and build up custom roles to suit their requirements. This could be as simple as combining a compute and a storage role to create a hyperconverged configuration, or perhaps fragmenting the monolithic controller role to split out some of the services to enable a greater degree of scalability. Historically, all networking services were provided by the controllers; now we have a choice about where the networking services should operate. As part of this section we're going to demonstrate how we can utilise composable roles to choose how, and where, to deploy the networking services.

We're going to be creating a dedicated networker role, in which all OpenStack networking related services (except for the Neutron API) will reside. For us to understand how this works we must first explore the TripleO heat templates that dictate and control the role structure, and then we'll look to make some minor modifications to suit our desired configuration.

The templates that are fed into Heat are dynamically generated at deployment time through Jinja2 templates based on the roles that are defined by the administrator. If no custom role information is supplied it will use the default configuration residing at ***/usr/share/openstack-tripleo-heat-templates/roles_data.yaml***, which lists all of the individual OpenStack services (and supporting functions) that it needs to deploy for each of the listed roles. By either modifying this file (not recommended) or supplying a customised role information file at deployment time, one can dynamically define new roles and specify the services in which it will be associated with.

Inside of this file it's possible to both create new roles dynamically, or simply move services between the existing role structures that are already well understood. In our environment we're going to be pulling the networking functions out of the controller role and placing them into a new role called "**Networker**". For this, we can use TripleO to generate some of the role definitions for us. Let's first make a directory that we'll use to hold all of our customised templates:

	$ mkdir -p ~/templates/

Next, let's generate a new custom roles file, specifying the three main roles that we want to cover, i.e. a "Controller", a "Compute", and a "Networker"; noting that TripleO already has the construct of a dedicated networker node available, so it knows which services to allocated into this role:

	$ openstack overcloud roles generate \
		Controller Compute Networker -o ~/templates/custom_roles.yaml

So how does our view of a dedicated networker node differ from the standard configuration? Well, firstly, the TripleO concept of a networker node is very slightly different from what we're trying to achieve with our deployment. Namely, the default TripleO networker role still assumes that we want the Controller nodes to still perform networking functions, whereas we want the networker nodes to do all of this for us, again aside from the Neutron API which will remain on the controller.

So, let's remove all Neutron services/agents/daemons (with the exception of Neutron API and the Neutron Core Plugin service, something used to configure Neutron's API server to talk to the chosen plugin) from the controller. TripleO, via the roles file generation tool, has already ensured that the networker node has these roles already satisfied.

Next, edit the **~/templates/custom_roles.yaml** file with your favourite text editor:

	$ vi ~/templates/custom_roles.yaml

...and remove the following services from the Controller role:

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

	$ cp ~/labs/director/complete-config/custom_roles.yaml ~/templates/

> **NOTE**: If you're still unsure about how this file works, please ask for further assistance.

We'll be providing this custom roles template file to OSP director during the deployment phase so it knows what services it should deploy on each node.

# Environment Files

As discussed previously, environment files are used to customise an overcloud, provide additional parameters, or override default functionality, such as integrating third party software or authentication systems, complex networking setups, etc - far too much for us to explore in such a short lab session. However, we can demonstrate some of the configuration options that can be set through the environment file mechanism. Environment files are in the yaml format, and therefore whitespace is incredibly important for defining the hierarchy of options defined. We use some cat/EOF commands below to ensure whitespace conformity and minimise disruptions during this lab.

Firstly, we'll create a base-configuration file known as '**config.yaml**', and we'll override the default hostnames for each of our machines. Noting that the default role definition defines role names as "overcloud-\<**role_name**>-\<**index**>.localdomain" by default, for example, the first controller node that it deploys would have the hostname "**overcloud-controller-0.localdomain**". Here let's make things a little cleaner for our environment and update/override the **HostnameMap** parameters. You'll notice that we're using the block '**parameter_defaults**' to override certain paramters:

	$ cat > ~/templates/config.yaml <<EOF
	parameter_defaults:
	  HostnameMap:
	    overcloud-controller-0: summit-controller
	    overcloud-novacompute-0: summit-compute1
	    overcloud-novacompute-1: summit-compute2
	    overcloud-networker-0: summit-networker
	EOF

Now, we need to tell OSP director how many of each role to deploy (**count**), and also which **flavor** to assign each role. Remember that in the previous lab section we assigned roles to nodes through the profile extension, well Nova needs to know which flavor to schedule so it can map that back to a profile name. Here we define that we want one compute, one controller, and one networker node. Note that the compute count of one is not a typo here, we'll demonstrate scaling the environment in a later lab section:

	$ cat >> ~/templates/config.yaml <<EOF
	  NetworkerCount: 1
	  ControllerCount: 1
	  ComputeCount: 1
	  OvercloudNetworkerFlavor: networker
	  OvercloudComputeFlavor: compute
	  OvercloudControllerFlavor: control
	EOF

As we're using virtualised environments with only a small amount of "physical" memory, often we can run into scheduling issues where the host advertises a small amount of memory as it's reserving a higher amount for standard host functions. This value defaults to 4GB, whereas that's almost all of what we've assigned to each compute node. If we tell it to only reserve 1GB then we have 3GB of virtual machine storage space on each compute node to run VM's on:

	$ cat >> ~/templates/config.yaml << EOF
	  NovaReservedHostMemory: 1024
	EOF

Finally, we can ask OSP director to run arbitrary code/scripts during deployment; this can be useful if you want it to configure certain items that it doesn't know about, or if you want to override passwords or anything like that. There are many different entry points into OSP director to call code blocks, dependent on the exact time of the deployment that you want it to run. For example, you could have some code execute right at the start, some during, and some at the end. We use the **NodeExtraConfig** entrypoint to have it call a block of code right before OpenStack deployment; all we're doing is setting the root password to something we know, as opposed to having it randomly generated. There's also a **NodeExtraConfigPost** which can be used for running code at the end of the deployment after all other steps have been exhausted.

We first have to pull in another yaml file that contains the actual code, and then create an additional environment file known as '**extra-config.yaml**' that has a **'resource_registry'** construct that points to the our code definition for the **NodeExtraConfig** resource.

	$ cp ~/labs/director/templates/pre-deployment.yaml ~/templates/pre-deployment.yaml
	$ cat > ~/templates/extra-config.yaml << EOF
	resource_registry:
	  OS::TripleO::NodeExtraConfig: /home/stack/templates/pre-deployment.yaml
	EOF

> **NOTE**: By default, **NodeExtraConfig**(Post) typically point to a no-op.yaml file. When we set this to something else, we're explicitly overriding this functionality by providing our own code through this entrypoint.


	