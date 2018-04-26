#**Lab 9: Environment Scale Out**

As we discussed previously, the concept of OSP director was to provide a tool that wasn’t just for the installation of OpenStack, but to truly become an ongoing lifecycle management platform. We know that the initial installation of OpenStack was only a small piece of the puzzle when it came to our customers wanting to use the technology in production. As such, OSP director not only provides administrators with a mechanism for OpenStack installation but to provide tools for day-two management also.

Today, OSP director can provide capabilities such as automated automated minor version updates (for example moving to the latest patch-set ***within*** a given major release, e.g. Red Hat OpenStack Platform 12), automated major version upgrades (for example moving from Red Hat OpenStack Platform 11, based on OpenStack Ocata, to Red Hat OpenStack Platform 12, based on OpenStack Pike), making post-deployment changes, and also the ability to scale an existing deployment, e.g. adding additional compute nodes to accommodate additional compute demands.

In this section we're going to take our deployment of a single controller, single compute node, and dedicated networker and add an additional compute node to our existing cluster, demonstrating how simple this is with the OSP director tooling.

# Current Configuration

Our existing environment should only have one hypervisor configured - if you recall correctly we specifically told OSP director to deploy a single compute node, which it should have done (remember that we're asking the overcloud what hypervisors it has here, so source the overcloudrc file here):

	$ source ~/overcloudrc
	$ openstack hypervisor list
	+----+-----------------------------+-----------------+-------------+-------+
	| ID | Hypervisor Hostname         | Hypervisor Type | Host IP     | State |
	+----+-----------------------------+-----------------+-------------+-------+
	|  1 | summit-compute1.localdomain | QEMU            | 172.17.1.15 | up    |
	+----+-----------------------------+-----------------+-------------+-------+

Yet we actually have **two** "baremetal" machines defined within our environment (remember to source the correct undercloud file):
	
	$ source ~/stackrc
	$ openstack baremetal node list -c Name -c 'Power State' -c 'Provisioning State'
	+--------------------+-------------+--------------------+
	| Name               | Power State | Provisioning State |
	+--------------------+-------------+--------------------+
	| summit-controller1 | power on    | active             |
	| summit-compute1    | power off   | available          |      <--- here's our free node
	| summit-compute2    | power on    | active             |
	| summit-networker1  | power on    | active             |
	+--------------------+-------------+--------------------+
	
> **NOTE**: In your environment, **summit-compute1** may be the active node - both machines were tagged with the compute profile, and Nova will choose a random machine to satisfy the single compute node requirement. Don't be alarmed if this is the case, it will make no difference to the outcome of this lab, and when we scale it out we should see the other one being chosen.

# Expanding our Compute Capacity

In this next step, we'll ask OSP director to add another compute node to our existing deployment. If there's already an overcloud deployed, OSP director assumes that any new deploy commands are simply a request to update the existing overcloud with new parameters, similar to how a 'patch' would work. Therefore, all we need to do is re-run our deploy command but increase the compute-scale parameter to '2'.

	$ sed -i "s/ComputeCount:.*/ComputeCount: 2/g" ~/templates/config.yaml

You'll need to ensure that we're attempting to run this command on the correct cloud, the undercloud (where OSP director runs), and not the overcloud that we were just interacting with. Then, run the deploy command again with the increased count for compute in our **~/templates/config.yaml** file:

	$ source ~/stackrc
	$ openstack overcloud deploy --templates \
		-r ~/templates/custom_roles.yaml \
		-e ~/templates/config.yaml \
		-e ~/templates/network-config.yaml \
		-e ~/templates/extra-config.yaml \
		-e ~/templates/docker_registry.yaml \
		-e /usr/share/openstack-tripleo-heat-templates/environments/network-isolation.yaml
	  
	Started Mistral Workflow tripleo.validations.v1.check_pre_deployment_validations. Execution ID: 5adca625-088b-4884-b96b-9f89b8e023ab
	Waiting for messages on queue 'e2965c0a-8c50-4d8d-aeff-8867ca7fb00e' with no timeout.
	Removing the current plan files
	Uploading new plan files
	Started Mistral Workflow tripleo.plan_management.v1.update_deployment_plan. Execution ID: 7c786da0-0f8d-48ff-990e-6f7ce37a0fc7
	Plan updated.
	Processing templates in the directory /tmp/tripleoclient-6Xvvpq/tripleo-heat-templates
	Started Mistral Workflow tripleo.plan_management.v1.get_deprecated_parameters. Execution ID: 2cc2d3ba-227e-4c7c-af10-21f1efd12503
	Deploying templates in the directory /tmp/tripleoclient-6Xvvpq/tripleo-heat-templates
	Started Mistral Workflow tripleo.deployment.v1.deploy_plan. Execution ID: 057ad746-72db-4328-ab22-93347a4f943b
	2018-04-24 21:04:28Z [ServiceNetMap]: UPDATE_IN_PROGRESS  state changed
	(...)

> **NOTE**: Make sure that you've sourced the ~/stackrc file - the deploy command will error if you still have the ~/overcloudrc file sourced as this points to your overcloud - which, whilst it has Heat available, it doesn't have any of the required resources, and it's the completely wrong cloud! :-)

The time taken to deploy the additional compute node should be less time than the initial deployment, although as it runs through all initial deployment steps to ensure that the configuration is inline with the requested parameters it does still take some time. When testing, this took approximately 30 minutes, but your mileage may vary - we apologise if this takes longer than allocated within the lab. Once it's complete, you should see a somewhat familiar output, although instead of saying **CREATE_COMPLETE** you should now see an **UPDATE_COMPLETE** message:

	(...)
	2018-04-24 21:30:27Z [overcloud]: UPDATE_COMPLETE  Stack UPDATE completed successfully

	Stack overcloud UPDATE_COMPLETE

	Overcloud Endpoint: http://192.168.122.100:5000/v2.0
	Overcloud Deployed

Now let's verify that it did actually add the second compute node, remembering to source the ~/overcloudrc file, as it's the **overcloud** we want to interact with now:

	$ source ~/overcloudrc
	$ openstack hypervisor list
	+----+---------------------------------+-----------------+-------------+-------+
	| ID | Hypervisor Hostname             | Hypervisor Type | Host IP     | State |
	+----+---------------------------------+-----------------+-------------+-------+
	|  1 | summit-compute1.localdomain     | QEMU            | 172.17.1.15 | up    |
	|  2 | summit-compute2.localdomain     | QEMU            | 172.17.1.22 | up    |
	+----+---------------------------------+-----------------+-------------+-------+

And for good measure, let's ensure that our undercloud is registering all nodes as being utilised:

	$ source ~/stackrc
	$ openstack baremetal node list -c Name -c 'Power State' -c 'Provisioning State'
	+--------------------+-------------+--------------------+
	| Name               | Power State | Provisioning State |
	+--------------------+-------------+--------------------+
	| summit-controller1 | power on    | active             |
	| summit-compute1    | power on    | active             |
	| summit-compute2    | power on    | active             |
	| summit-networker1  | power on    | active             |
	+--------------------+-------------+--------------------+

Great! Our environment has been successfully scaled-out, and we will immediately have additional compute capacity within our OpenStack environment!

# Cleaning Up

That's it! We're done! We need to clean up the overcloud deployment (Heat stack), and we'll take care of the rest before some of the other labs can proceed later on today:
	$ openstack stack delete overcloud	Are you sure you want to delete this stack(s) [y/N]? y
**Thank you very much** for attending this lab, I hope that it gave you a bit of insight into how to use Red Hat OpenStack Platform director, how its components fit together and how it all works with practical examples. If you have any feedback please share it with us, or if there's anything we can do to assist you in your OpenStack journey, please don't hesitate to ask!

