#**Lab 5: Node Tagging**

In the previous lab section we went through the process of inspecting our machines to understand their specification and to fill the Ironic database with the required information to allow Nova to schedule instances onto the correct baremetal nodes, i.e. ensuring that the correct roles get assigned to the most suitable hardware.

Nova uses two main metrics to determine whether a node is suitable for a given role (e.g. controller, compute node), both of which are encapsulated into a flavor. The flavor defines the required size of the machine (e.g. number of CPU's, memory capacity, and available disk space) and also has a set of properties that a node must also satisfy. For example, a node may have the required physical capacity, but it may not satisfy additional properties. In OSP director we use the additional properties to define what we call a **profile**, it's the profile that we assign to a given node so that Nova doesn't just have to rely on the minimum hardware specification, we can give it a hint to say these machines are definitely controllers, for example.

Whilst it's possible to not use profiles at all and have Nova just rely on hardware specifications, or just choose nodes at random, profiles allow us more predictability when it comes to knowing which nodes will become certain roles, and for many organisations it's exactly what they need. In this lab section we're going to assign some profiles to our nodes, as our nodes have differing hardware specification and we want to make sure the right nodes are chosen for a given role. Plus, we've already named our nodes controller, compute, networker, etc, it would be a shame not to have their roles representative of their names!

# Viewing the Profiles

As we just alluded to, Nova relies on a flavor to determine which role to assign to a given node. These flavors are defined by a required hardware specification and a set of properties that align to a profile that each node is assigned. Many flavors already exist based on the most common types of roles. If we look at the available flavors, and the controller flavor example:

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

You can see from the above output that the controller flavor (**control**) requires 4GB memory, 1 CPU, and 40GB disk, but relies on the **profile** also called "**control**" (see properties field). Hence when Nova is looking for available baremetal nodes stored by Ironic, it will want to find a node that has been associated with the correct profile. If we look at the currently assigned profiles for our nodes, you'll see that none of our nodes have been assigned one:

	$ openstack overcloud profiles list
	+--------------------------------------+--------------------+-----------------+-----------------+-------------------+
	| Node UUID                            | Node Name          | Provision State | Current Profile | Possible Profiles |
	+--------------------------------------+--------------------+-----------------+-----------------+-------------------+
	| fcedca39-cc8f-4758-bc84-0c7a80a6c586 | summit-controller1 | available       | None            |                   |
	| e331a74e-bcc0-49d9-b260-3afffbccb305 | summit-compute1    | available       | None            |                   |
	| 41919840-5115-4b9f-a24d-cb9930c58029 | summit-compute2    | available       | None            |                   |
	| 7826c8ab-0130-4e9d-95f0-94923dd530d1 | summit-networker1  | available       | None            |                   |
	+--------------------------------------+--------------------+-----------------+-----------------+-------------------+

OSP director does have tooling that allows the profiles to be matched automatically based on pre-populating the required specification and hardware characteristics required for each profile, and they'll be automatically associated, but in our environment we've not used that and have to associate our nodes with their destined profile manually.

First, let's set the **controller** profile to "**control**", note that we have to make sure that we match all of the flavors properties (boot_option, and profile):

	$ openstack baremetal node set --property capabilities='profile:control,boot_option:local' \
		summit-controller1

> **NOTE**: This command should produce no output unless there was an error.

Next, let's associate our two compute nodes with the "**compute**" profile:

	$ openstack baremetal node set --property capabilities='profile:compute,boot_option:local' \
		summit-compute1
		
	$ openstack baremetal node set --property capabilities='profile:compute,boot_option:local' \
		summit-compute2

Finally let's associate our networker node with the network profile:

	$ openstack baremetal node set --property capabilities='profile:networker,boot_option:local' \
    	summit-networker1

Let's verify that these profiles have now been associated to our nodes. We can also minimise the output that we receive to only show the node name and the profile that has been assigned:

	$ openstack overcloud profiles list -c "Node Name" -c "Current Profile"
	+--------------------+-----------------+
	| Node Name          | Current Profile |
	+--------------------+-----------------+
	| summit-controller1 | control         |
	| summit-compute1    | compute         |
	| summit-compute2    | compute         |
	| summit-networker1  | networker       |
	+--------------------+-----------------+




