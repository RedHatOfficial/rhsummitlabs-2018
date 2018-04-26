#**Lab 4: Inspection of Nodes**

When we registered the "baremetal" nodes into Ironic we provided no information about the size of the machines, e.g. CPU count, available memory, disk specifications. Whilst this information is not essential to register the nodes, it *is* essential when it comes to deployment time - we want to make sure that the specification of the nodes meets our expectations and that OSP director (via Nova) can make educated scheduling decisions based on available hardware, ensuring that the correct roles are allocated to the most appropriate nodes - e.g. don't provision a controller node without the correct amount of memory, etc. Therefore, the inspection stage allows us to populate this information into the Ironic database automatically via discovery.

When we start the inspection of the nodes, Ironic powers the nodes on via the appropriate power management interface, they PXE boot into a bootstrap environment (provided by **ironic-python-agent**) and a set of tools collect system information and feed it into the Ironic database for our utilisation. In this lab section we'll be carrying out the inspection of the nodes and a brief evaluation of the data that has been retrieved.

By default, when we provide Ironic with our node information and they're successfully registered, it's assumed that you'll want to perform some maintenance or validation on them before they're available to be scheduled. Therefore it puts them into the '**manageable**' provisioning state:

	$ openstack baremetal node list -c Name -c "Provisioning State"
	+--------------------+--------------------+
	| Name               | Provisioning State |
	+--------------------+--------------------+
	| summit-controller1 | manageable         |
	| summit-compute1    | manageable         |
	| summit-compute2    | manageable         |
	| summit-networker1  | manageable         |
	+--------------------+--------------------+
	
# The Inspection Process

Now we're sure the nodes are in the correct state, we can start the inspection process, noting that via the command line tools this is refered to as "**introspection**":

	$ openstack overcloud node introspect --all-manageable --provide
	Waiting for introspection to finish...
	Started Mistral Workflow tripleo.baremetal.v1.introspect_manageable_nodes. Execution ID: e119d4c2-0553-4f44-804a-907108d9d58f
	Waiting for messages on queue '3ecc1415-3897-42bb-951b-beb14515a857' with no timeout.
	Introspection of node fcedca39-cc8f-4758-bc84-0c7a80a6c586 completed. Status:SUCCESS. Errors:None
	Introspection of node e331a74e-bcc0-49d9-b260-3afffbccb305 completed. Status:SUCCESS. Errors:None
	Introspection of node 7826c8ab-0130-4e9d-95f0-94923dd530d1 completed. Status:SUCCESS. Errors:None
	Introspection of node 41919840-5115-4b9f-a24d-cb9930c58029 completed. Status:SUCCESS. Errors:None
	Successfully introspected nodes.
	Nodes introspected successfully.
	Introspection completed.
	Started Mistral Workflow tripleo.baremetal.v1.provide_manageable_nodes. Execution ID: 0dd1201a-e365-4b69-954b-042f8a861ba6
	Waiting for messages on queue '3ecc1415-3897-42bb-951b-beb14515a857' with no timeout.
	
	Successfully set nodes state to available.

> **NOTE**: The command line won't return the above output immediately, it will wait and provide you with feedback as and when the inspection process has succeeded or not. During this process the machines will be automatically powered on by Ironic, and should only take a few minutes.

Upon completion, the nodes should automatically be placed back into the "**available**" provision state, this is because we opted for the "**--provide**" flag on the previous command. This will allow us to utilise these nodes for a deployment in a later stage without manually switching them to an **available** state. Before proceeding, let's just verify that:

	$ openstack baremetal node list -c Name -c "Provisioning State"
	+--------------------+--------------------+
	| Name               | Provisioning State |
	+--------------------+--------------------+
	| summit-controller1 | available          |
	| summit-compute1    | available          |
	| summit-compute2    | available          |
	| summit-networker1  | available          |
	+--------------------+--------------------+

# Exploring the Results

The data retrieved during the inspection process is stored in two places, firstly the basic system sizing (e.g. CPU count, memory capacity, and available disk space) is stored in the Ironic database directly, and secondly much deeper-dive hardware specification (e.g. network interface details, CPU specifications, disk serial numbers, hardware/OEM details, and if possible the IPMI details) are stored in a Swift object.

We can view both of these pieces of information by querying Ironic for the following fields (using the controller node as our node to query):

	$ openstack baremetal node show summit-controller1 --fields properties extra
	+------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field      | Value                                                                                                                                                        |
	+------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| extra      | {u'hardware_swift_object': u'extra_hardware-fcedca39-cc8f-4758-bc84-0c7a80a6c586'}                                                                           |
	| properties | {u'memory_mb': u'12288', u'cpu_arch': u'x86_64', u'local_gb': u'59', u'cpus': u'4', u'capabilities': u'boot_mode:bios,cpu_hugepages:true,boot_option:local'} |
	+------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------+

In the above output you can see that the '**extra**' field refers to the location of the Swift object (noting that the UUID matches the UUID of the node in the Ironic database) and the '**properties**' field displays the basic hardware specification that was loaded into the Ironic database directly. You'll see that this matches the **2 CPU, 12GB memory, and 60GB disk** that was allocated to us by the underlying hypervisor.

It's also possible to view the deep-dive information stored within Swift by downloading the object, noting that it's stored as a JSON structure:

	$ openstack baremetal introspection data save summit-controller1 | python -m json.tool
	{
	    "all_interfaces": {
	        "eth0": {
	            "client_id": null,
	            "ip": "172.16.0.150",
	            "mac": "2c:c2:60:01:02:02",
	            "pxe": true
	        },
	        "eth1": {
	            "client_id": null,
	            "ip": null,
	            "mac": "2c:c2:60:1f:6d:0e",
	            "pxe": false
	        },
	        "eth2": {
	            "client_id": null,
	            "ip": null,
	            "mac": "2c:c2:60:48:07:32",
	            "pxe": false
	        }
	    },
	    "boot_interface": "2c:c2:60:01:02:02",
	    "cpu_arch": "x86_64",
	    "cpus": 4,
	    "error": null,
	    "extra": {
	        "cpu": {
	            "logical": {
	                "number": 4
	            },
	            "physical": {
	                "number": 4
	            },
	            "physical_0": {
	                "cores": 1,
	                "enabled_cores": 1,
	                "flags": "fpu fpu_exception wp vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ss syscall nx x86-64 constant_tsc rep_good nopl pni ssse3 cx16 x2apic hypervisor lahf_lm",
	                "physid": 400,
	                "product": "Intel(R) Core(TM)2 Duo CPU     T7700  @ 2.40GHz",
	                "threads": 1,
	                "vendor": "Intel Corp.",
	                "version": "pc-i440fx-2.2"
	            },
	(...)

> **NOTE**: The above data is cut to save on the printing. Also It's possible to save the content to a file with **"--file \<location>"** instead of piping it directly into the python tool.
