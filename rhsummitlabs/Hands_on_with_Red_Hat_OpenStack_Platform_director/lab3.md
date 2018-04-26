#**Lab 3: Import of Node Definitions**

Administrators that want to provision OpenStack environments via OSP director need to register their nodes directly with Ironic, the OpenStack component that is responsible for controlling and provisioning baremetal machines. Ironic started life as an alternative Nova "**baremetal driver**", enabling administrators to provide ‘baremetal-to-tenant' use cases, in other words, rather than Nova deploying virtual machines for end-users, Nova could now offer the provisioning of dedicated baremetal hosts to end users via self-service. End-users would be guaranteed isolation but also bare metal performance, not needing to share compute resources with other users. Today, Ironic is its own OpenStack project, with it’s own respective API and command line utilities, and from Red Hat it’s available for two primary purposes. Firstly, Ironic is available for customers that do want to offer the baremetal-to-tenant use-case to their end-users. Secondly, Ironic as a core component of OSP director can be used as a mechanism for controlling, and deploying the baremetal hardware or physical nodes that are required for an OpenStack deployment in our overcloud.

For Ironic to be able to provision hardware, administrations need to specify their IPMI-based out of band management credentials and network addresses for each node, although there are also vendor-specific drivers, for example HP iLO, Cisco UCS, Dell DRAC, amongst others, in the Ironic community. The IPMI details are required for Ironic to manage the power-state of bare metal nodes that will be used for the overcloud deployment - Ironic remotely connects to the power-management platform of each node and can turn the nodes on and off at-will. As we're using a virtual environment for our lab (certainly not recommended for production, but just fine for demonstration purposes) we use a specific Ironic driver called **pxe_ssh**, which doesn't use native IPMI, but allows us to control virtual machines just as if they were baremetal nodes.

Once machines are powered on, Ironic provisions a disk image during hardware bootstrap (via DHCP and PXE). OSP director uses Glance on the undercloud to store the necessary disk images, supplied by Red Hat, that are to be used by booting overcloud nodes. These disk images typically contain Red Hat Enterprise Linux and all OpenStack components, which minimises any post-deployment software installation. They can, of course, be customised further prior to upload into Glance, for example, customers often want to integrate additional software or configurations as per their requirements. Ironic pulls down the generic image from Glance during deployment and simply writes it out on the root disk of the booting node.

> **NOTE**: From OSP12 onwards, OSP director/TripleO utilises a containerised overcloud, and whilst containers are used to house OpenStack services, we still need to deploy a disk image onto our nodes, and the same mechanism is used here.

As we saw in the previous lab section, we currently have no defined Ironic nodes to deploy OpenStack onto, although they are defined within the public cloud platform. In this lab section we're going to be working on getting them imported into our environment, and uploading the required images for node bootstrap and deployment.<br />
<br />

# Importing Images

As mentioned above, OSP director uses Glance to stores a number of disk images that are required for hardware deployment. OSP director relies on a component called **ironic-python-agent**, a small bootstrap kernel and ramdisk that contains all of the tools required to bootstrap a node. In addition, OSP director requires a full disk-image (with an associated kernel and ramdisk) known as **overcloud-full.{qcow2,vmlinuz,initrd}** which will be written out to the booting node and will become it's bootup disk. We'll need to upload these images into Glance so that Ironic can use them at deployment time.

Let's ensure that we currently have no disk images available in Glance on our undercloud, remembering to source the undercloud's environment file (~/stackrc):

	$ source ~/stackrc
	$ openstack image list
	(Empty)

> **NOTE**: We expect this to return no images - this is a fresh install and we have no images installed!

When we install the undercloud we also install the default, unmodified disk images as shipped by Red Hat. Let's extract these to a temporary directory before we upload them into Glance:

	$ mkdir -p ~/images/
	
	$ tar xvf /usr/share/rhosp-director-images/ironic-python-agent-latest-12.0.tar -C ~/images
	ironic-python-agent.initramfs
	ironic-python-agent.kernel	
	
	$ tar xvf /usr/share/rhosp-director-images/overcloud-full-latest-12.0.tar -C ~/images/
	overcloud-full.qcow2
	overcloud-full.initrd
	overcloud-full.vmlinuz
	overcloud-full-rpm.manifest
	overcloud-full-signature.manifest

We should now have all of these files in our ~/images directory:

	$ ll ~/images/
	total 1724772
	-rw-r--r--. 1 stack stack  381063155 Mar  9 08:17 ironic-python-agent.initramfs
	-rwxr-xr-x. 1 stack stack    5915568 Mar  9 08:17 ironic-python-agent.kernel
	-rw-r--r--. 1 stack stack   58671593 Mar  9 08:31 overcloud-full.initrd
	-rw-r--r--. 1 stack stack 1314390016 Mar  9 08:39 overcloud-full.qcow2
	-rw-r--r--. 1 stack stack      54240 Mar  9 08:39 overcloud-full-rpm.manifest
	-rw-r--r--. 1 stack stack     143052 Mar  9 08:39 overcloud-full-signature.manifest
	-rwxr-xr-x. 1 stack stack    5915568 Mar  9 08:31 overcloud-full.vmlinuz

As you can see, there are five files here (excluding the manifest files), both the bootstrap kernel and ramdisk (ironic-python-agent) and the kernel, ramdisk, and qcow2 that provide the content for the boot disk for each node that gets provisioned by Ironic. Note that the same generic image gets deployed to each node, and this image, with the exception of the container images, already contains all of the content that's required to get an overcloud deployed - we do not need access to any package repositories to complete the deployment.

Before we upload these images, let's customise the overcloud-full.qcow2 image by resetting the root password to something we know; this may be helpful later if we need to troubleshoot anything:

	$ virt-customize -a ~/images/overcloud-full.qcow2 --root-password password:redhat
	[   0.0] Examining the guest ...
	[  20.2] Setting a random seed
	[  20.3] Setting passwords
	[  22.1] Finishing off

Next, let's **upload** the images to our environment, making sure that we specify the location of our images:

	$ openstack overcloud image upload --image-path ~/images/
	Image "overcloud-full-vmlinuz" was uploaded.
	+--------------------------------------+------------------------+-------------+---------+--------+
	|                  ID                  |          Name          | Disk Format |   Size  | Status |
	+--------------------------------------+------------------------+-------------+---------+--------+
	| 3b3a7070-d4dc-4d7d-8844-8242fd2e4571 | overcloud-full-vmlinuz |     aki     | 5915568 | active |
	+--------------------------------------+------------------------+-------------+---------+--------+
	Image "overcloud-full-initrd" was uploaded.
	+--------------------------------------+-----------------------+-------------+----------+--------+
	|                  ID                  |          Name         | Disk Format |   Size   | Status |
	+--------------------------------------+-----------------------+-------------+----------+--------+
	| 1a1da22c-42c8-4ec3-8fd9-7c4cca7cf6f9 | overcloud-full-initrd |     ari     | 58671593 | active |
	+--------------------------------------+-----------------------+-------------+----------+--------+
	Image "overcloud-full" was uploaded.
	+--------------------------------------+----------------+-------------+------------+--------+
	|                  ID                  |      Name      | Disk Format |    Size    | Status |
	+--------------------------------------+----------------+-------------+------------+--------+
	| b498c808-9d5e-46d4-b788-aa12074b7e34 | overcloud-full |    qcow2    | 1315897344 | active |
	+--------------------------------------+----------------+-------------+------------+--------+
	Image "bm-deploy-kernel" was uploaded.
	+--------------------------------------+------------------+-------------+---------+--------+
	|                  ID                  |       Name       | Disk Format |   Size  | Status |
	+--------------------------------------+------------------+-------------+---------+--------+
	| f6bb308d-bd77-4159-8a09-708aa9806b85 | bm-deploy-kernel |     aki     | 5915568 | active |
	+--------------------------------------+------------------+-------------+---------+--------+
	Image "bm-deploy-ramdisk" was uploaded.
	+--------------------------------------+-------------------+-------------+-----------+--------+
	|                  ID                  |        Name       | Disk Format |    Size   | Status |
	+--------------------------------------+-------------------+-------------+-----------+--------+
	| f804f73f-eac7-40fa-aa35-088cfb599f19 | bm-deploy-ramdisk |     ari     | 381063155 | active |
	+--------------------------------------+-------------------+-------------+-----------+--------+

Great! Now we've got these images ready to go, we can now proceed onto registering our nodes.

# The instackenv.json file

Whilst it's possible to import nodes manually on a one-by-one basis using the OpenStack client tools for Ironic, it's certainly not time efficient and is error-prone. Therefore, it's common to generate a JSON structured file with all of the nodes details and running it through a validation tool prior to import into Ironic. By default this is known as '**instackenv.json**', but it can be named anything you like.

Let's first take a look at the structure of our node definition file which has been pre-prepared for you just to avoid potential syntax errors and to save on time:

	$ cat ~/labs/director/instackenv.json | python -m json.tool
	{
    	"nodes": [
        {
            "mac": [
                "2c:c2:60:01:02:02"
            ],
            "name": "summit-controller1",
            "pm_addr": "172.16.0.131",
            "pm_password": "redhat",
            "pm_type": "pxe_ipmitool",
            "pm_user": "admin"
        },
        {
            "mac": [
                "2c:c2:60:01:02:05"
            ],
            "name": "summit-compute1",
            "pm_addr": "172.16.0.134",
            "pm_password": "redhat",
            "pm_type": "pxe_ipmitool",
            "pm_user": "admin"
        },
        {
            "mac": [
                "2c:c2:60:01:02:06"
            ],
            "name": "summit-compute2",
            "pm_addr": "172.16.0.135",
            "pm_password": "redhat",
            "pm_type": "pxe_ipmitool",
            "pm_user": "admin"
        },
        {
            "mac": [
                "2c:c2:60:79:05:32"
            ],
            "name": "summit-networker1",
            "pm_addr": "172.16.0.136",
            "pm_password": "redhat",
            "pm_type": "pxe_ipmitool",
            "pm_user": "admin"
        }
      ]
	}

As you can see, for each of our nodes we have an entry in the 'nodes' dictionary. Each node is uniquely identified by the MAC address of the network interface used for provisioning (i.e. the NIC that will be used for the DHCP+PXE provisioning process) and has a unique name to identify it. You'll notice that all of the nodes have the same "**pm_addr**", i.e. the address that the Ironic driver contacts, which for us is public cloud platform's API that hosts all of our virtual nodes.

Next, let's make sure there are no errors with our file by running it through the verification tool supplied within OSP director:

	$ openstack baremetal instackenv validate -f ~/labs/director/instackenv.json
	System Power         : off
	Power Overload       : false
	Power Interlock      : inactive
	Main Power Fault     : false
	Power Control Fault  : false
	Power Restore Policy : always-off
	Last Power Event     :
	Chassis Intrusion    : inactive
	Front-Panel Lockout  : inactive
	Drive Fault          : false
	Cooling/Fan Fault    : false
	(...)
	SUCCESS: found 0 errors

We've cut down on the output above, but hopefully you see that there are no errors - e.g. duplicate MAC addresses, improper JSON formatting, or any missing parameters that may be required for node import. It also outputs some of the IPMI (power management) outputs to validate that it was able to talk with the public cloud platforms API for managing the node state.

We can now import our nodes into Ironic, making sure that we specify the location of our instackenv.json file:

	$ openstack overcloud node import ~/labs/director/instackenv.json
	Started Mistral Workflow tripleo.baremetal.v1.register_or_update. Execution ID: f6b35f95-7075-4175-9f37-80b9302bd68f
	Waiting for messages on queue 'c1b05203-ecec-4cc0-92a6-23dc4ad9384c' with no timeout.

	Nodes set to managed.
	Successfully registered node UUID fcedca39-cc8f-4758-bc84-0c7a80a6c586
	Successfully registered node UUID e331a74e-bcc0-49d9-b260-3afffbccb305
	Successfully registered node UUID 41919840-5115-4b9f-a24d-cb9930c58029
	Successfully registered node UUID 7826c8ab-0130-4e9d-95f0-94923dd530d1

What this means is that Ironic was successful in importing the nodes into the database. We can confirm that they're ready by re-running the node list:

	$ openstack baremetal node list
	+--------------------------------------+--------------------+---------------+-------------+--------------------+-------------+
	| UUID                                 | Name               | Instance UUID | Power State | Provisioning State | Maintenance |
	+--------------------------------------+--------------------+---------------+-------------+--------------------+-------------+
	| fcedca39-cc8f-4758-bc84-0c7a80a6c586 | summit-controller1 | None          | power off   | manageable         | False       |
	| e331a74e-bcc0-49d9-b260-3afffbccb305 | summit-compute1    | None          | power off   | manageable         | False       |
	| 41919840-5115-4b9f-a24d-cb9930c58029 | summit-compute2    | None          | power off   | manageable         | False       |
	| 7826c8ab-0130-4e9d-95f0-94923dd530d1 | summit-networker1  | None          | power off   | manageable         | False       |
	+--------------------------------------+--------------------+---------------+-------------+--------------------+-------------+
	
> **NOTE**: You can verify that Ironic is able to communicate with the underlying power management platform by the above output showing 'power off' for the power state. If there are any problems with communication, this would be in an error state.

Finally, if we ask Ironic for some details about one of our nodes, you can see more details about the node:

	$ openstack baremetal node show summit-controller1
	+------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field                  | Value                                                                                                                                                                                                              |
	+------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| boot_interface         | None                                                                                                                                                                                                               |
	| chassis_uuid           | None                                                                                                                                                                                                               |
	| clean_step             | {}                                                                                                                                                                                                                 |
	| console_enabled        | False                                                                                                                                                                                                              |
	| console_interface      | None                                                                                                                                                                                                               |
	| created_at             | 2018-04-20T00:37:36+00:00                                                                                                                                                                                          |
	| deploy_interface       | None                                                                                                                                                                                                               |
	| driver                 | pxe_ipmitool                                                                                                                                                                                                       |
	| driver_info            | {u'ipmi_password': u'******', u'ipmi_address': u'172.16.0.131', u'deploy_ramdisk': u'f804f73f-eac7-40fa-aa35-088cfb599f19', u'deploy_kernel': u'f6bb308d-bd77-4159-8a09-708aa9806b85', u'ipmi_username': u'admin'} |
	| driver_internal_info   | {}                                                                                                                                                                                                                 |
	| extra                  | {}                                                                                                                                                                                                                 |
	| inspect_interface      | None                                                                                                                                                                                                               |
	| inspection_finished_at | None                                                                                                                                                                                                               |
	| inspection_started_at  | None                                                                                                                                                                                                               |
	| instance_info          | {}                                                                                                                                                                                                                 |
	| instance_uuid          | None                                                                                                                                                                                                               |
	| last_error             | None                                                                                                                                                                                                               |
	| maintenance            | False                                                                                                                                                                                                              |
	| maintenance_reason     | None                                                                                                                                                                                                               |
	| management_interface   | None                                                                                                                                                                                                               |
	| name                   | summit-controller1                                                                                                                                                                                                 |
	| network_interface      | flat                                                                                                                                                                                                               |
	| power_interface        | None                                                                                                                                                                                                               |
	| power_state            | power off                                                                                                                                                                                                          |
	| properties             | {u'capabilities': u'boot_option:local'}                                                                                                                                                                            |
	| provision_state        | manageable                                                                                                                                                                                                         |
	| provision_updated_at   | 2018-04-20T00:37:41+00:00                                                                                                                                                                                          |
	| raid_config            | {}                                                                                                                                                                                                                 |
	| raid_interface         | None                                                                                                                                                                                                               |
	| reservation            | None                                                                                                                                                                                                               |
	| resource_class         | baremetal                                                                                                                                                                                                          |
	| storage_interface      | noop                                                                                                                                                                                                               |
	| target_power_state     | None                                                                                                                                                                                                               |
	| target_provision_state | None                                                                                                                                                                                                               |
	| target_raid_config     | {}                                                                                                                                                                                                                 |
	| updated_at             | 2018-04-20T00:37:41+00:00                                                                                                                                                                                          |
	| uuid                   | fcedca39-cc8f-4758-bc84-0c7a80a6c586                                                                                                                                                                               |
	| vendor_interface       | None                                                                                                                                                                                                               |
	+------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

So far Ironic knows very little about our nodes; it has little visibility into the specification, and only knows that it's likely going to be an OpenStack controller because that's what we named it as in our instackenv.json file. We'll fix that in the next step.