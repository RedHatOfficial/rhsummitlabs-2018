#**Lab 3: Adding Images into Glance (Image Service)**

##**Introduction**

Glance is OpenStack's image service, it provides a mechanism for discovering, registering and retrieving virtual machine images. These images are typically standardised and generalised so that they will require post-boot configuration applied. Glance supports a wide variety of disk image formats, including raw, qcow2, vmdk, ami and iso, all of which can be stored in multiple types of backends, including OpenStack Swift, Ceph, or basic implementations such as NFS, although by default it will use the local filesystem. When a user requests an instance with OpenStack, it's Glance's responsibility to provide that image and allow it to be retrieved prior to instantiation.

Glance stores metadata alongside each image which helps identify it and describe the image, it accomodates multiple container types, e.g. an image could be completely self contained such as a qcow2 image, or it could be a separate kernel and initrd file which need to be tied together to successfully boot an instance of that machine. Glance is made up of two components, the glance-registry which is the actual image registry service and glance-api which provides the RESTful API end-point to the rest of the OpenStack services.

##**Adding an image to Glance**

As previously mentioned, images uploaded to Glance should be 're-initialised' or 'sysprepped' so that any system-specific configuration is wiped away, this ensures that there are no conflicts between instances that are started. It's common practice to find pre-defined virtual machine images online that contain a base operating system and perhaps a set of packages for a particular purpose. The next few steps will allow you to take such an image and upload it into the Glance registry. 

As part of the lab, we'll use an image already residing on the filesystem for you (although with the CLI tools, it's possible to point to a URL) and upload it as our own. Let's verify that the disk image is as expected and has the correct properties:

	$ qemu-img info ~/labs/rhel-server-7.4-x86_64-kvm.qcow2
	image: /home/stack/labs/rhel-server-7.4-x86_64-kvm.qcow2
	file format: qcow2
	virtual size: 10G (10737418240 bytes)
	disk size: 538M
	cluster_size: 65536
	Format specific information:
	    compat: 0.10
	    refcount bits: 16

Next we can create a new image within Glance and import its contents, it may take a few minutes to copy the data. Let's ensure that we've sourced our demo user environment file (noting that it doesn't matter if you've already sourced this file - repeating the source command is safe), and proceed with the image creation:

	$ source ~/demorc

	$ openstack image create rhel7 --public \
		--disk-format qcow2 --container-format bare \
		--file ~/labs/rhel-server-7.4-x86_64-kvm.qcow2
	+------------------+------------------------------------------------------------------------------+
	| Field            | Value                                                                        |
	+------------------+------------------------------------------------------------------------------+
	| checksum         | 2065a01cacd127c2b5f23b1738113325                                             |
	| container_format | bare                                                                         |
	| created_at       | 2018-04-09T13:35:25Z                                                         |
	| disk_format      | qcow2                                                                        |
	| file             | /v2/images/4568dd3f-b6f6-4a8e-b551-473e885cf7c5/file                         |
	| id               | 4568dd3f-b6f6-4a8e-b551-473e885cf7c5                                         |
	| min_disk         | 0                                                                            |
	| min_ram          | 0                                                                            |
	| name             | rhel7                                                                        |
	| owner            | f991d44fac91419c8e6016184381871a                                             |
	| properties       | direct_url='swift+config://ref1/glance/4568dd3f-b6f6-4a8e-b551-473e885cf7c5' |
	| protected        | False                                                                        |
	| schema           | /v2/schemas/image                                                            |
	| size             | 564330496                                                                    |
	| status           | active                                                                       |
	| tags             |                                                                              |
	| updated_at       | 2018-04-09T13:35:34Z                                                         |
	| virtual_size     | None                                                                         |
	| visibility       | public                                                                       |
	+------------------+------------------------------------------------------------------------------+	

The container format is '**bare**' because it doesn't require any additional images such as a kernel or initrd to support it, it's completely self-contained. The '**--public**' option allows any projects to use the image rather than locking it down for the specific project uploading the image. We could have used the parameter '**--private**' to set the opposite if we would have preferred.

In our environment, we use **Swift** as a backing store for our Glance images; Swift is an OpenStack project that provides RESTful object storage, and is deployed automatically with Red Hat OpenStack Platform by default, and provides resiliency and load-balancing for objects right out of the box. When we uploaded our image to Glance, it automatically stored it as an object, the location of which is reflected in the output:

> **direct_url='swift+config://ref1/glance/4568dd3f-b6f6-4a8e-b551-473e885cf7c5'**

If we had multiple OpenStack Controllers, Swift would ensure that this object was replicated across multiple nodes, ensuring that the Glance image (and any other objects held by Swift) are highly available. There are many other storage backends that can be used for image storage with Glance; **Ceph** is one of the most common.

Finally, vertify that the image is available in the repository, and it's status is set to 'active':

	$ openstack image list
	+--------------------------------------+-------+--------+
	| ID                                   | Name  | Status |
	+--------------------------------------+-------+--------+
	| 4568dd3f-b6f6-4a8e-b551-473e885cf7c5 | rhel7 | active |
	+--------------------------------------+-------+--------+