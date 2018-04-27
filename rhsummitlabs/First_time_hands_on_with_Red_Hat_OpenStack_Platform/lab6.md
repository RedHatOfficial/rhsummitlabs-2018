#**Lab 6: Creation and Attachment of Block Devices with Cinder (Volume Service)**


##**Introduction**

Cinder is OpenStack's volume service, it's responsible for managing persistent block storage, i.e. the creation of a block device, it's lifecycle, connections to instances and it's eventual deletion. Block storage is an optional requirement in OpenStack but has many advantages, firstly persistence but also for performance scenarios, e.g. access to data backed by tiered storage. We call it optional because by default, an instances root disk space is provided by the hypervisor and resides on a local disk; it's tied to the lifecycle of an instance - if you remove the instance, this disk also gets removed, whereas with a Cinder it's external to the hypervisor and provides persistence. There are two main reasons why people would want to use a Cinder volume-

* To remove the reliance on the underlying hypervisor storage (i.e. the root disk of an instance resides on a storage volume), which may be capacity-limited, is not necessarily **performant**, is **ephemeral** by default - there's no **persistence**, and if the hypervisor is lost, so is your data!

* To provide **additional** storage, either in the situation where you are using default hypervisor storage space, or the root disk is also a Cinder volume. In this scenario, additional volumes can be provides from one or more backend storage types to provide additional capacity for an instance that isn't tied to the flavor size of an instance.

Cinder supports many different back-ends (with various protocols, e.g. iSCSI, FC, and file) in which it can connect to and manage storage for OpenStack, including a wide variety of hardware arrays, e.g. EMC, 3PAR, NetApp, etc, as well as software implementations such as Ceph. However, for testing, Cinder provides support for a basic Linux storage model, based on exposing logical volumes (LVM) via iSCSI - as we're doing this on our workstations, we'll be using this model, and has already been configured for you.

##**Testing Cinder**

Let's test our Cinder configuration, making sure that it can create a volume, note that you'll need to be authenticated with Keystone for this to work:

	$ source ~/demorc
	$ openstack volume create --size 5 my_volume
	+---------------------+--------------------------------------+
	| Field               | Value                                |
	+---------------------+--------------------------------------+
	| attachments         | []                                   |
	| availability_zone   | nova                                 |
	| bootable            | false                                |
	| consistencygroup_id | None                                 |
	| created_at          | 2018-04-09T22:04:31.013598           |
	| description         | None                                 |
	| encrypted           | False                                |
	| id                  | da6c2b7b-d10c-4171-89e3-e2e76e7638f0 |
	| migration_status    | None                                 |
	| multiattach         | False                                |
	| name                | my_volume                            |
	| properties          |                                      |
	| replication_status  | None                                 |
	| size                | 5                                    |
	| snapshot_id         | None                                 |
	| source_volid        | None                                 |
	| status              | creating                             |
	| type                | None                                 |
	| updated_at          | None                                 |
	| user_id             | 2c580c9e773143f5b4d82b9a6131b47a     |
	+---------------------+--------------------------------------+

The above command will create a 5GB block device within the test environment although it won't be attached to any instances yet. The volume is created based on a type, or backend, and as we only have one type (LVM-backed iSCSI, or as we've configured it, "**tripleo_iscsi**") it default to this type, hence we don't have to specifically set it on the command line. Each backend is fullfilled by a **cinder-volume** service, and it's possible to view all of these services with the following command:

	$ openstack volume service list
	+------------------+-------------------------+------+---------+-------+----------------------------+
	| Binary           | Host                    | Zone | Status  | State | Updated At                 |
	+------------------+-------------------------+------+---------+-------+----------------------------+
	| cinder-scheduler | hostgroup               | nova | enabled | up    | 2018-04-09T22:04:45.000000 |
	| cinder-volume    | hostgroup@tripleo_iscsi | nova | enabled | up    | 2018-04-09T22:04:52.000000 |
	+------------------+-------------------------+------+---------+-------+----------------------------+

In our environment, this cinder-volume service is running on the **summit-controller**, and we'll explore how it creates and implements volumes over the next few sections. After a minute or so, our created volume should become available:

	$ openstack volume list
	+--------------------------------------+-----------+-----------+------+-------------+
	| ID                                   | Name      | Status    | Size | Attached to |
	+--------------------------------------+-----------+-----------+------+-------------+
	| da6c2b7b-d10c-4171-89e3-e2e76e7638f0 | my_volume | available |    5 |             |
	+--------------------------------------+-----------+-----------+------+-------------+

Now that's been created, let's see if we can find the logical volume that backs that Cinder volume on our **summit-controller** machine, noting that we're executing this command remotely:

	$ ssh root@summit-controller lvscan
	  ACTIVE            '/dev/cinder-volumes/cinder-volumes-pool' [<9.54 GiB] inherit
	  ACTIVE            '/dev/cinder-volumes/volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0' [5.00 GiB] inherit

OK, so that volume corresponds with our UUID for our Cinder volume (note that your ID will be different to the above example). When we attach this to a volume, the node that runs the **cinder-volume** service will expose the LVM over iSCSI to the hypervisor running the instance, which, in turn, will attach it to the virtual machine as a local SCSI disk.

Let's attach our volume to the running instance from the previous lab; if you don't have this - create a new instance as part of the previous lab before you proceed. The following also assumes that you have called your VM "**my_vm**" as per the previous instructions. You can verify that your VM is still running with this command:

	$ openstack server list
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| ID                                   | Name  | Status | Networks                             | Image | Flavor  |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
	| 9f5c7572-f64b-4509-8522-ad2b39fe65c5 | my_vm | ACTIVE | internal=172.16.1.9, 192.168.122.202 | rhel7 | m1.labs |
	+--------------------------------------+-------+--------+--------------------------------------+-------+---------+
##**Attaching your volume**

We'll use the command line to attach this volume to our running instance:

	$ openstack server add volume my_vm my_volume

> **NOTE**: Unless there was an error, this command provides no feedback.

Verify that it has been successfully attached with the following, noting that it may take a minute or so to appear as **in-use**:

	$ openstack volume list
	+--------------------------------------+-----------+--------+------+--------------------------------+
	| ID                                   | Name      | Status | Size | Attached to                    |
	+--------------------------------------+-----------+--------+------+--------------------------------+
	| da6c2b7b-d10c-4171-89e3-e2e76e7638f0 | my_volume | in-use |    5 | Attached to my_vm on /dev/vdb  |
	+--------------------------------------+-----------+--------+------+--------------------------------+

So, OpenStack is telling us that the volume has been created and attached, but there's one way to test that the instance can see it. Note that in the output you'll see that it has attached the volume to **'/dev/vdb'**, the second interface for that machine; the first (**'/dev/vda'**) will be the default ephemeral hypervisor storage, which resides in '**/var/lib/nova/instances/[uuid]/disk**' on the hyperisor as shown below. First get the uuid of your instance and the compute node it's running on:

	$ openstack server show my_vm -c id -c OS-EXT-SRV-ATTR:host
	+----------------------+--------------------------------------+
	| Field                | Value                                |
	+----------------------+--------------------------------------+
	| OS-EXT-SRV-ATTR:host | summit-compute2.localdomain          |
	| id                   | 9f5c7572-f64b-4509-8522-ad2b39fe65c5 |
	+----------------------+--------------------------------------+

Now take a look at the instance's ephemeral disk on our dedicated hypervisor/compute node (**summit-compute2** in my example, yours may be different), noting that we'll be running this command remotely:

	$ ssh root@summit-compute2 \
		qemu-img info /var/lib/nova/instances/9f5c7572-f64b-4509-8522-ad2b39fe65c5/disk
		
	image: /var/lib/nova/instances/9f5c7572-f64b-4509-8522-ad2b39fe65c5/disk
	file format: qcow2
	virtual size: 10G (10737418240 bytes)
	disk size: 42M
	cluster_size: 65536
	backing file: /var/lib/nova/instances/_base/fd2beb46d533a0209d6e0be31715938351ed7dde
	Format specific information:
	    compat: 1.1
	    lazy refcounts: false
	    refcount bits: 16
	    corrupt: false

> **NOTE:** For reference, you'll see that there's a **'backing_file'** listed above. This is used by OpenStack to layer a much smaller file (in our case 41MB) on-top of the Glance image and is used as a template. This allows us to be very efficient with storage utilisation by using copy-on-write (COW).

Let's reconnect back into our instance and verify that the new volume is available, replace the IP address as necessary for your machine:

	$ ssh root@192.168.122.202
	
Once connected, let's ensure that the block device is available for us to use:

	[root@my-vm ~]# fdisk -l
	Disk /dev/vda: 10.7 GB, 10737418240 bytes, 20971520 sectors
	Units = sectors of 1 * 512 = 512 bytes
	Sector size (logical/physical): 512 bytes / 512 bytes
	I/O size (minimum/optimal): 512 bytes / 512 bytes
	Disk label type: dos
	Disk identifier: 0x000a73bb
	
	   Device Boot      Start         End      Blocks   Id  System
	/dev/vda1   *        2048    20971486    10484719+  83  Linux
	
	Disk /dev/vdb: 5368 MB, 5368709120 bytes, 10485760 sectors
	Units = sectors of 1 * 512 = 512 bytes
	Sector size (logical/physical): 512 bytes / 512 bytes
	I/O size (minimum/optimal): 512 bytes / 512 bytes

So we can see the second disk that it can see is **/dev/vdb** at a size of **5GB**, proving that Nova has successfully attached our Cinder volume to our instance. Next, we can create a filesystem and mount that up:

	[root@my-vm ~]# mkfs.xfs /dev/vdb
	meta-data=/dev/vdb               isize=512    agcount=4, agsize=327680 blks
	         =                       sectsz=512   attr=2, projid32bit=1
	         =                       crc=1        finobt=0, sparse=0
	data     =                       bsize=4096   blocks=1310720, imaxpct=25
	         =                       sunit=0      swidth=0 blks
	naming   =version 2              bsize=4096   ascii-ci=0 ftype=1
	log      =internal log           bsize=4096   blocks=2560, version=2
	         =                       sectsz=512   sunit=0 blks, lazy-count=1
	realtime =none                   extsz=4096   blocks=0, rtextents=0	
	[root@my-vm ~]# mount /dev/vdb /mnt
	[root@my-vm ~]# df -Th
	Filesystem     Type      Size  Used Avail Use% Mounted on
	/dev/vda1      xfs        10G  885M  9.2G   9% /
	devtmpfs       devtmpfs  900M     0  900M   0% /dev
	tmpfs          tmpfs     920M     0  920M   0% /dev/shm
	tmpfs          tmpfs     920M  8.4M  912M   1% /run
	tmpfs          tmpfs     920M     0  920M   0% /sys/fs/cgroup
	tmpfs          tmpfs     184M     0  184M   0% /run/user/0
	/dev/vdb       xfs       5.0G   33M  5.0G   1% /mnt

Next unmount your volume and disconnect from the VM:

	[root@my-vm ~]# umount /mnt
	[root@my-vm ~]# exit
	logout
	Connection to 192.168.122.202 closed.
	
Let's confirm how the iSCSI export has worked to our hypervisor via **targetcli**, noting that in the printout this information will have gone off the page in the printed version, but the electronic HTML copy should allow you to scroll through. We'll need to execute this command on our controller node (**summit-controller**) as this is where the volume resides, and is where it's exported from, hence we're executing this command remotely:

	$ ssh root@summit-controller targetcli ls
	o- / ......................................................................................................................... [...]
	  o- backstores .............................................................................................................. [...]
	  | o- block .................................................................................................. [Storage Objects: 1]
	  | | o- iqn.2010-10.org.openstack:volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0  [/dev/cinder-volumes/volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0 (5.0GiB) write-thru activated]
	  | |   o- alua ................................................................................................... [ALUA Groups: 1]
	  | |     o- default_tg_pt_gp ....................................................................... [ALUA state: Active/optimized]
	  | o- fileio ................................................................................................. [Storage Objects: 0]
	  | o- pscsi .................................................................................................. [Storage Objects: 0]
	  | o- ramdisk ................................................................................................ [Storage Objects: 0]
	  o- iscsi ............................................................................................................ [Targets: 1]
	  | o- iqn.2010-10.org.openstack:volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0 ............................................. [TPGs: 1]
	  |   o- tpg1 .......................................................................................... [no-gen-acls, auth per-acl]
	  |     o- acls .......................................................................................................... [ACLs: 1]
	  |     | o- iqn.1994-05.com.redhat:1fa3396fe29e ...................................................... [1-way auth, Mapped LUNs: 1]
	  |     |   o- mapped_lun0 ................. [lun0 block/iqn.2010-10.org.openstack:volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0 (rw)]
	  |     o- luns .......................................................................................................... [LUNs: 1]
	  |     | o- lun0  [block/iqn.2010-10.org.openstack:volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0 (/dev/cinder-volumes/volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0) (default_tg_pt_gp)]
	  |     o- portals .................................................................................................... [Portals: 1]
	  |       o- 172.17.3.13:3260 ................................................................................................. [OK]
	  o- loopback ......................................................................................................... [Targets: 0]
	  o- xen_pvscsi ....................................................................................................... [Targets: 0]  
What this shows is that we're currently exporting one block device **"/dev/cinder-volumes/volume-da6c2b7b-d10c-4171-89e3-e2e76e7638f0 (5.0GiB)"**, with one connected node (restricted via ACL's), **"iqn.1994-05.com.redhat:1fa3396fe29e"**. Non LVM/iSCSI Cinder backends have their own way of exporting and providing/zoning the volumes to necessary nodes for utilisation.

Let's now clean up by detaching the volume from the instance:

	$ openstack server remove volume my_vm my_volume
	
And then remove our volume and the instance before we move onto the next lab:

	$ openstack volume delete my_volume
	$ openstack server delete my_vm

After a minute or so the following should be empty:

	$ openstack server list
	[Empty]
