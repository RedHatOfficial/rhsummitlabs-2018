#**Lab 8: Deployment of Overcloud**

Recall that OSP director utilises a number of different OpenStack components to be able to deploy OpenStack itself, but the most critical component is Heat and the templates that describe how to deploy it. Whilst OSP director utilises these OpenStack components, the bulk of the value is in the TripleO 'glue' that sticks all of these components together to form a solid basis for OpenStack deployment. The majority of the work has gone into ensuring that the deployment can be carried out with a single command via the OpenStack command line tooling, via the TripleO extension for '**overcloud**', e.g. 'openstack overcloud \<command>'. The command line tooling takes in a wealth of arguments and can support the inclusion of a number of different files to help define the overclouds configuration.

The command line tool will, in turn, call the necessary OpenStack tools to carry out the deployment and will ensure that the OpenStack overcloud has been deployed successfully, advising the operator of any problems along the way, and providing real-time feedback as to what it's doing. Finally, it's responsible for finalising the deployment - building local files for interaction and building the overcloud's service catalogue ready for use.

This is the lab section that we've been building up to - the actual **deployment** of OpenStack. The previous lab sections were all about preparation and making sure that our "baremetal" nodes were ready and that our Heat templates were ready to describe to OSP director how our overcloud should look once deployed.

# Setting up for Docker Containers

One of the biggest changes in OSP12 is the containerisation of OpenStack services, i.e. OpenStack services residing and operating within docker-based containers. This is a huge feature of OSP12, and a huge architectural change. Red Hat are embracing it for many reasons.

Firstly, we get dependency isolation for each service - we can embed everything that we need for a given service into a single container image, without having to worry that we’ll break other service functionality, including required library versions, or anything that may be required. It makes updates and upgrades a lot easier to manage - a container can simply be replaced with a newer copy, containing the newer or patched code, and if that operation fails, it’s very simple to roll back to the previous version, without a panic about how to restore the environment. We get a higher degree of deployment flexibility, building on-top of the composable roles functionality we can distribute and rebalance services at will. Scalability is also much easier, we can throw more containers into the mix to accommodate demand when required, and scale back when not, efficiently using hardware. Because we’re using immutable infrastructure, i.e. when it’s running it doesn’t change you need to rip and replace to make a change, it means that the code and configuration is well understood, and it means that the complexity around configuration file management and day to day operations is minimised. Finally, we can also leverage a lot of the new container runtime management technology for better resource utilisation, and control over allocation of system resources. To sum things up, containers are bringing a huge benefit to our customers and Red Hat's ability to support and maintain OpenStack.

### What's required for Containerised Services?

By default in OSP12+, each OpenStack service has it's own self-contained Docker image, containing all required dependencies. Docker images are stored in repositories known as registries and need to be **pulled** before using them. All of the required images have been pulled for you, and reside in a local registry on the undercloud machine. Whilst it's possible to use a remote Docker registry during deployment, it's not very efficient given that the images can be quite large, and also the nodes may not have a network route to the registry. By using the undercloud server as a registry we gain performance and a guaranteed network route.

The typical flow of an administrator wanting to deploy a containerised overcloud is as follows-

1. Generate a list of required images that will need to be used based on the chosen TripleO configuration, e.g. if using a particular networking plugin such as **OpenDaylight**, make sure you include this in your required images list.<br /><br />
2. Take this list and upload the images to the **local** docker registry (on the undercloud) from the **remote** registry (e.g. the Red Hat Content Delivery Network).<br /><br />
3. Generate an environment file used by TripleO; typically stored as "**docker_registry.yaml**", that tells OSP director exactly which docker images to use for each TripleO service and where to find them, i.e. the image location, the name, and the tag on the **local** registry. It's used to tell the booting overcloud nodes which images to pull and where to pull them from.<br /><br />
4. Deploy the overcloud, specifying the generated environment file (i.e. docker_registry.yaml).

Steps (1), (2), and (3) can be executed using TripleO command line tooling, but for convenience we've already executed these steps for you, and you'll find the correct docker_registry.yaml file residing in the home directory. Let's copy the example file to our templates directory:

	$ cp ~/labs/director/templates/docker_registry.yaml ~/templates/

As an example, let's ensure that it has the OpenStack Dashboard (Horizon) container configured correctly:

	$ grep -i horizon ~/templates/docker_registry.yaml
	  DockerHorizonConfigImage: 172.16.0.1:8787/rhosp12/openstack-horizon:12.0-20180309.1
	  DockerHorizonImage: 172.16.0.1:8787/rhosp12/openstack-horizon:12.0-20180309.1	
We have two images listed, why two? Well, each service upon TripleO instantiation needs to do two things - firstly, it needs to configure itself, e.g. setting up configuration files via puppet and running bootstrap commands (e.g. Galera bootstrap, RabbitMQ configuration, etc), and secondly there has to be an image that is used to run the OpenStack service itself - one that contains the binaries. In the vast majority of cases, this image is the same across both the first phase configuration step, and the second phase actually running of the binaries/services.


# Starting the Deployment

We're ready to go! Let's ensure that we're in our home directory, and that we source the 'stackrc' file, just to be sure we're ready to start the deployment:

	$ cd && source ~/stackrc
	
Next, issue the **'openstack overcloud deploy'** command, making sure that we specify our composable role information, and our environment files that contain all of the configuration we wanted to set (all of the parameters and the options are explained in more detail a little further down the page):

	$ openstack overcloud deploy --templates \
		-r ~/templates/custom_roles.yaml \
		-e ~/templates/config.yaml \
		-e ~/templates/network-config.yaml \
		-e ~/templates/extra-config.yaml \
		-e ~/templates/docker_registry.yaml \
		-e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml

	Started Mistral Workflow tripleo.validations.v1.check_pre_deployment_validations. Execution ID: 1dd2b1f9-dad2-4d5b-afb0-551541fc5e23
	Waiting for messages on queue '6ab0545b-7d33-4ab7-8761-52990fce4374' with no timeout.
	Removing the current plan files
	Uploading new plan files
	Started Mistral Workflow tripleo.plan_management.v1.update_deployment_plan. Execution ID: 10629b68-a14d-41f9-b115-ca4bce3009f1
	Plan updated.
	Processing templates in the directory /tmp/tripleoclient-HCdhs6/tripleo-heat-templates
	Started Mistral Workflow tripleo.plan_management.v1.get_deprecated_parameters. Execution ID: f6f3e2b6-8b6c-41d5-938a-d02c75faaefd
	Deploying templates in the directory /tmp/tripleoclient-HCdhs6/tripleo-heat-templates
	Started Mistral Workflow tripleo.deployment.v1.deploy_plan. Execution ID: 50d7bb21-aae2-4edd-915f-705a6da81c38
	2018-04-26 09:07:33Z [overcloud]: CREATE_IN_PROGRESS  Stack CREATE started
	(...)

> **NOTE**: The above command will continue to output the resources as they're being created, and will show any errors should any occur. We can leave this run for now.

Given that our templates are relatively basic and that we provided a number of these for you already you shouldn't have any problems with the deployment. The unfortunate part is that this takes approximately **50 minutes** within the public cloud environment - typically it's less than half that time with such a configuration, so it may be worth grabbing a cup of coffee until it's completed. For reference (and for reading whilst the deployment is taking place), the command line options for the deploy command are explained below in more detail:

| Parameter  | Details  |
|---|---|
| --templates |  This tells OSP director that you want to use the TripleO Heat Templates for deployment, from the default location /usr/share/openstack-tripleo-heat/templates (unless overridden with a different directory path) |
| -r ~/templates/custom_roles.yaml  | The '**-r**' flag tells the command line tooling that you want to define the roles manually, rather than utilising the out of the box roles. This points to our **~/templates/custom_roles.yaml** file which we are using to create the dedicated networker node. |
| -e ~/templates/config.yaml  | The '**-e**' flag tells OSP director that you're specifying an environment file that will modify the out of the box configuration, it can be used to specify (and override) standard parameters and TripleO resources with custom values and custom templates. In this section we're providing our custom environment file containing all parameters (with the exception of the networking config) that we've described over the past few lab sections, including node counts, flavors to use, hostname overrides, etc. |
| -e ~/templates/network-config.yaml  | The environment file that specifies our overall networking configuration for the environment, including links to the nic-configs via the **resource_registry** as well as the subnet details, VLAN configurations, and any other additional Neutron configuration via **parameter_defaults**. |
| -e ~/templates/extra-config.yaml  | The environment file that specifies additional configuration that we want to apply to the overcloud through the execution of arbitrary code through the **NodeExtraConfig** extension/entrypoint. |
| -e ~/templates/docker_registry.yaml  | The environment file that specifies the list of **docker images** that will be used within the overcloud based on the desired TripleO configuration, including the location of the images, i.e. the local docker registry. |
| -e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml  | Another environment file that we've not seen before, and should be included when organisations want to use different network traffic types within their environment rather than a single shared network for all. This is an out of the box example environment file (hence its location on the filesystem) and creates a dedicated network and associated ports for each network traffic type for each node. |

If you're watching the deployment, you'll see that it runs through a number of different tasks, such as the configuration of networks, "physical" node deployment, and software installation/configuration. The vast majority of the time is spent on the software installation and configuration phases, and progress for this is indicated by the **Step** that it's currently on. There are **five** steps in total, so you'll see output such as the following that will indicate the current step:

	2018-04-26 09:30:40Z [overcloud.AllNodesDeploySteps.NetworkerDeployment_Step2]: CREATE_IN_PROGRESS  Stack CREATE started
	2018-04-26 09:30:40Z [overcloud.AllNodesDeploySteps.NetworkerDeployment_Step2.0]: CREATE_IN_PROGRESS  state changed
	2018-04-26 09:31:20Z [overcloud.AllNodesDeploySteps.NetworkerDeployment_Step2.0]: SIGNAL_IN_PROGRESS  Signal: deployment 56455a0e-6df3-4d46-86a6-7f13c0ded29a succeeded
	2018-04-26 09:31:20Z [overcloud.AllNodesDeploySteps.NetworkerDeployment_Step2.0]: CREATE_COMPLETE  state changed
 
Here it's indicating that it's currently on Step**2**. Each node that you're deploying goes through each step, hence why it's Step2.0 (0 being the index here, and in this case the first and only networker node).

Once the deployment has succeeded through all of the steps, you should receive the following output:

	2018-04-20 09:08:49Z [overcloud.AllNodesDeploySteps]: CREATE_COMPLETE  Stack CREATE completed successfully
	2018-04-20 09:08:50Z [overcloud.AllNodesDeploySteps]: CREATE_COMPLETE  state changed
	2018-04-20 09:08:50Z [overcloud]: CREATE_COMPLETE  Stack CREATE completed successfully
	
	 Stack overcloud CREATE_COMPLETE
	
	Started Mistral Workflow. Execution ID: 6081024d-d1d4-42f6-81f5-67287f8ff766
	Overcloud Endpoint: http://192.168.122.100:5000/v2.0
	Overcloud Deployed

If you're unable to successfully deploy your overcloud, please run the following command to list the failures, and ask for some assistance from one of the lab supervisors:

	$ openstack stack failures list overcloud --long
	(...)

# Post-deployment Validation

If you've got this far, your overcloud was deployed successfully and OpenStack should be running - nice work! :-)

But we should definitely make sure it's working properly. In your home directory you'll find a new file to source environment variables from, just like the '**stackrc**' file for the undercloud, you'll now find an '**overcloudrc**' file that can be used to interact with the overcloud using the same system and the same command line tools as before. Just *remember* to source the correct file depending on the cloud that you want to communicate with and the commands that you want to execute.

Let's source this new file, and see if we can get an authentication token, and a list of OpenStack services running in the overcloud:

	$ source ~/overcloudrc
	$ openstack token issue
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field      | Value                                                                                                                                                                                   |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| expires    | 2018-04-24T21:47:58+0000                                                                                                                                                                |
	| id         | gAAAAABa35f-ssm4Tq1Eo90l-zKAudKMbM8L9bM-qEZGZg3XBxGSGhX6Gx4-zEF8q_urbjsk7eGfBvtDjMwlirag7hDfPR8m9ojiu-nUfnj3FEcpH4TykWudVDm0cT1y5GmepxVXHrUFtYRkaOredVdKwzqxQIS8LnJYzyaKnCESsbZ2FRFS9vg |
	| project_id | 3812f3c0fae44138af238f6e1a0a7d1b                                                                                                                                                        |
	| user_id    | 66800ea91f6f42ea80c8050b5f6557d5                                                                                                                                                        |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

	$ openstack service list
	+----------------------------------+------------+----------------+
	| ID                               | Name       | Type           |
	+----------------------------------+------------+----------------+
	| 22ce20931f9c48d6b3ca833082b802cb | glance     | image          |
	| 31aeeb51c42f4bad9d5f34405ecf5120 | cinder     | volume         |
	| 337db4b9abcd4ef1a9e47b06cc791e00 | keystone   | identity       |
	| 3429977065c648d3877a645f5d5c9a98 | panko      | event          |
	| 3bcb1de1892d4b15b2ea1c9f590d7162 | heat       | orchestration  |
	| 60b740cdc7cf4254a00aacba41ff9c99 | ceilometer | metering       |
	| 72c83e3a51504a7d85de0eb96d20f75c | heat-cfn   | cloudformation |
	| 7dc26965b4194b4f909a2fb3207cba34 | placement  | placement      |
	| 9f2a5754a504403cb170b6871aaafbc4 | gnocchi    | metric         |
	| aaad235660ef421b90720890759ffb1f | cinderv2   | volumev2       |
	| beb892e687ae44a1adb3615cc33e88dd | swift      | object-store   |
	| ceac952515d94e28aa52a05c3adb74e7 | aodh       | alarming       |
	| d27f6d1d0aa448ea85e256ca6fb7a6ef | neutron    | network        |
	| e293763f34094edcadfdf2ff24207ea7 | nova       | compute        |
	| f970b0a8b6524fd2968f2d6dc1ac45e0 | cinderv3   | volumev3       |
	+----------------------------------+------------+----------------+

To validate that the services have been configured properly, i.e. with a single compute node and with a dedicated networker node, we can lookup the hypervisors and the network agents that have been deployed:

	$ openstack hypervisor list
	+----+---------------------------------+-----------------+-------------+-------+
	| ID | Hypervisor Hostname             | Hypervisor Type | Host IP     | State |
	+----+---------------------------------+-----------------+-------------+-------+
	|  1 | summit-compute1.localdomain     | QEMU            | 172.17.1.17 | up    |
	+----+---------------------------------+-----------------+-------------+-------+
	
	$ openstack network agent list -c "Agent Type" -c "Host" -c "State"
	+--------------------+---------------------------------+-------+
	| Agent Type         | Host                            | State |
	+--------------------+---------------------------------+-------+
	| Metadata agent     | summit-networker.localdomain    | UP    |
	| L3 agent           | summit-networker.localdomain    | UP    |
	| Open vSwitch agent | summit-networker.localdomain    | UP    |
	| Open vSwitch agent | summit-compute1.localdomain     | UP    |
	| DHCP agent         | summit-networker.localdomain    | UP    |
	+--------------------+---------------------------------+-------+
	
Great! We've got one compute node, and the dedicated networker is running all of our main supporting networking agents - i.e. DHCP, L3 (routing), and metadata, with the only additional networking service being provided by the comput node itself, the dedicated Open vSwitch agent that's responsible for configuring local networking for running virtual machines.

So it's been able to deploy OpenStack, but how do we know it it's actually working? Well, you've got a choice, you can either start to build some resources yourself, or you can run a script that we've pre-prepared for you which will get us started. You can check it out at **~/labs/director/test-overcloud.sh**

Let's run this script to make sure that everything is working...

	$ sh ~/labs/director/test-overcloud.sh
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field      | Value                                                                                                                                                                                   |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| expires    | 2018-04-24T21:50:58+0000                                                                                                                                                                |
	| id         | gAAAAABa35iy2_tszdgZ_-h5A5WyGmDEiF47o2c9kra1aRpOUVbfmfJKRZ0_wiOvgJplnMnjYyUbYcjVH4cByCr5RpDne1ZAa0iBcbUQz7XvBsF73OiA08_ghNjPToxwz17zN1fFdXjNFunvdGssIpQIQGS6z8M1f8phXBnjWqYkKALRhQ9CGu4 |
	| project_id | 3812f3c0fae44138af238f6e1a0a7d1b                                                                                                                                                        |
	| user_id    | 66800ea91f6f42ea80c8050b5f6557d5                                                                                                                                                        |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	+------------------+------------------------------------------------------------------------------+
	| Field            | Value                                                                        |
	+------------------+------------------------------------------------------------------------------+
	| checksum         | 2065a01cacd127c2b5f23b1738113325                                             |
	| container_format | bare                                                                         |
	| created_at       | 2018-04-24T20:51:01Z                                                         |
	| disk_format      | qcow2                                                                        |
	| file             | /v2/images/349bac87-af00-4aff-a099-5fcbc944fa27/file                         |
	| id               | 349bac87-af00-4aff-a099-5fcbc944fa27                                         |
	| min_disk         | 0                                                                            |
	| min_ram          | 0                                                                            |
	| name             | rhel7                                                                        |
	| owner            | 3812f3c0fae44138af238f6e1a0a7d1b                                             |
	| properties       | direct_url='swift+config://ref1/glance/349bac87-af00-4aff-a099-5fcbc944fa27' |
	| protected        | False                                                                        |
	| schema           | /v2/schemas/image                                                            |
	| size             | 564330496                                                                    |
	| status           | active                                                                       |
	| tags             |                                                                              |
	| updated_at       | 2018-04-24T20:51:12Z                                                         |
	| virtual_size     | None                                                                         |
	| visibility       | public                                                                       |
	+------------------+------------------------------------------------------------------------------+
	+--------------------------------------+-------+--------+
	| ID                                   | Name  | Status |
	+--------------------------------------+-------+--------+
	| 349bac87-af00-4aff-a099-5fcbc944fa27 | rhel7 | active |
	+--------------------------------------+-------+--------+
	(...)

The output above has been cut down, but it should create the following resources in order:

* A Red Hat Enterprise Linux 7.4 Guest **image**
* An **external** network based on our public cloud virtualised network (192.168.122.0/24 - routable from our undercloud machine)
* An **internal** VXLAN based network for internal communication
* A virtual **router** to link our external network to the internal network
* A new **flavor** for our instance to use (2 vCPU's, 1GB memory, 10GB disk)
* Modifications to the default **security group rules** to allow ICMP and SSH access 
* Uploads the stack@undercloud secure shell **key** for passwordless access
* Boots a RHEL 7.4 **instance** based on the above configuration
* Creates and associates a **floating IP** to our instance on the external network to gain access

After 5 minutes or so, the script will be finished and it will output the server list, showing the instance running with the IP addresses (although it may take a minute or two for it to become active). You can test access with the following:

	$ ssh root@<your floating IP>

> **NOTE**: As we're using nested virtualisation here, the performance will not be optimal, but we're not actually running anything intensive, just booting a nested virtual machine.

Make sure you disconnect from your virtual machine before proceeding, by typing '**exit**' (or Ctrl-D):

	[root@test-vm ~]$ exit
	logout
	Connection to 192.168.122.205 closed.

Next, let's open up a web-browser and ensure that our Horizon dashboard is working and that it shows all of our resources that we just built up. The URL for this can be found on the lab's landing page which can be found in the email that was sent to you from RHPDS, see the hyperlink in the middle that looks like this - [http://horizon-REPL.rhpds.opentlc.com/dashboard](http://horizon-REPL.rhpds.opentlc.com/dashboard) (where REPL is your GUID that was allocated to you when we started), once opened you should see the following:

<img src="images/horizon.png" style="width: 1000px;"/>

To login, you'll need to get the automatically generated password from the recently created **~/overcloudrc** file (your password will be different to the output shown below):

	$ egrep -i '(username|password)' ~/overcloudrc
	export OS_USERNAME=admin
	export OS_PASSWORD=jZJVX3D4xZaKeDJPfWs8CEBUB

> **NOTE**: It's possible to pre-set the passwords, but we've kept our configuration relatively vanilla and therefore we're using the default behaviour which is for OSP director to generate the passwords for all services automatically.

Make sure you select the '**Project**' tab at the top of the screen, as it should take you to the '**Identity**' tab by default as we're doing everything as the 'admin' user. Verify that your instance is running and is in the **'Active'** state:

<img src="images/instance-running.png" style="width: 1000px;"/>

Feel free to play around with the OpenStack deployment if you've made it this far with plenty of time to spare. Once you're finished, you can close the browser and return to the terminal emulator before continuing on with the next lab section.
