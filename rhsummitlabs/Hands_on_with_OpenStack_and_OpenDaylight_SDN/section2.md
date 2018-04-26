##**Lab Environment Exploration**

In this section we're going to take a brief look of the pre-deployed lab environment to see how it has been put together. Unfortunately due to the time taken to complete a deployment end-to-end, the lab authors decided that working with a pre-deployed environment made the most sense; a deployment typically takes between 30-60 minutes on normal hardware (depending on the configuration), and when we tested within the public cloud environment it took much longer. Therefore we're not actually going to be deploying OpenStack in this lab, that step has already been done for you, but we'll investigate the integration of OpenDaylight and OpenStack over the next couple of hours, and will also demonstrate how it could have been configured through TripleO (OSP director) later on at the end.

The lab environment has been configured to look like the following-

<img src="images/lab-environment.png" style="width: 1000px;"/>
There are six total machines, if you include the jump-host, that we'll be utilising. In a previous step we used the jump-host to connect into the undercloud (in **blue**), which is a small bootstrap OpenStack environment that's used to deploy the overcloud (in **red**). The overcloud has already been pre-deployed and already has OpenDaylight integrated, running on a dedicated '**networker**' node. Sitting on-top of all of these systems are dedicated VLANs that are used to segment OpenStack network traffic, e.g. ensuring that internal API communication is isolated from tenant network traffic, and so on.

Now that we've successfully connected from the public internet to the jump-host, and have used that to get into our undercloud machine, let's explore what the current setup looks like a little further. All steps, unless explicitly mentioned, will be executed as the '**stack**' user on the undercloud machine; this is a non-privileged user account and will be used to perform all requirements of the lab sections - we will not need the root account, although you should have sudo access if you want to explore a bit further.

As highlighted previously, we're operating with two separate clouds here, both of which are running OpenStack; the **undercloud** and the **overcloud**; it's the undercloud that's used to **bootstrap** the overcloud. As such, getting access to each of these clouds requires the correct credentials and some parameters that specify where the relevant API's reside. Typically you'd only ever need to interact with the undercloud if you were deploying, updating, or deleting an overcloud, but we're going to demonstrate the base functionality of the undercloud here just to level-set on the architecture of the overcloud.

# The ~/stackrc and ~/overcloudrc files

Upon initial installation of the undercloud (not covered in this lab), OSP director generates a file called '**stackrc**'; this file resides in the stack user's home directory and is a source of environment variables that enable the OpenStack command line tools to execute commands against the **undercloud** itself. Then, by default, after an **overcloud** has been deployed by the undercloud, OSP director (through TripleO) creates a file called **overcloudrc** (and **overcloudrc.v3**), which essentially provides very similar information to stackrc, but instead of telling the OpenStack command line tooling to point at the undercloud, it points it at the overcloud.

So, to re-iterate, the undercloud machine provides access to **two** functioning OpenStack deployments - one as the "command and control" or bootstrap cloud running on that system, and the other as the "production" OpenStack environment running within the overcloud nodes. Access is provided by either the 'stackrc' file (for the undercloud), or 'overcloudrc' file for the overcloud. The file you **source** will affect which deployment you're issuing commands to. Let's take a look at these files.

Assuming that you're still connected to your **undercloud** machine as the **stack** user,  you'll notice that these files are residing in stack's home directory, as we have both a functioning undercloud and overcloud. Let's first look at the stackrc file:

	$ cat ~/stackrc

	# Clear any old environment that may conflict.
	for key in $( set | awk '{FS="="}  /^OS_/ {print $1}' ); do unset $key ; done
	NOVA_VERSION=1.1
	export NOVA_VERSION
	OS_PASSWORD=$(sudo hiera admin_password)
	export OS_PASSWORD
	OS_AUTH_TYPE=password
	export OS_AUTH_TYPE
	OS_AUTH_URL=http://172.16.0.1:5000/
	export OS_AUTH_URL
	OS_USERNAME=admin
	OS_PROJECT_NAME=admin
	COMPUTE_API_VERSION=1.1
	# 1.34 is the latest API version in Ironic Pike supported by ironicclient
	IRONIC_API_VERSION=1.34
	OS_BAREMETAL_API_VERSION=$IRONIC_API_VERSION
	OS_NO_CACHE=True
	OS_CLOUDNAME=undercloud
	export OS_USERNAME
	export OS_PROJECT_NAME
	export COMPUTE_API_VERSION
	export IRONIC_API_VERSION
	export OS_BAREMETAL_API_VERSION
	export OS_NO_CACHE
	export OS_CLOUDNAME
	OS_IDENTITY_API_VERSION='3'
	export OS_IDENTITY_API_VERSION
	OS_PROJECT_DOMAIN_NAME='Default'
	export OS_PROJECT_DOMAIN_NAME
	OS_USER_DOMAIN_NAME='Default'
	export OS_USER_DOMAIN_NAME
	
	# Add OS_CLOUDNAME to PS1
	if [ -z "${CLOUDPROMPT_ENABLED:-}" ]; then
	    export PS1=${PS1:-""}
	    export PS1=\${OS_CLOUDNAME:+"(\$OS_CLOUDNAME)"}\ $PS1
	    export CLOUDPROMPT_ENABLED=1
	fi
	
Here you can see that we set-up the **username**, **password**, and **authentication URL** that will give us everything we need to utilise the OpenStack client command line tools that you're no doubt already familiar with, but perhaps have never used them against a TripleO based OpenStack environment. There are also a large number of other environment variables that help set specific API versions to use, and make it clear which environment file has been sourced by overriding PS1.

So, let's make sure that our undercloud works, let's **source** this file (use this file as a source of environment variables):

	$ source ~/stackrc

> **NOTE**: You'll note that your command line now shows "(undercloud)" before the command prompt to signify the cloud that you'll be executing commands against. But this isn't shown in the commands below.

Now attempt to grab an authentication token from our undercloud:


	$ openstack token issue
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field      | Value                                                                                                                                                                                   |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| expires    | 2018-04-17T00:09:02+0000                                                                                                                                                                |
	| id         | gAAAAABa1QLeKwRwfspD-8WsK9SnwleYztJf9CF9WQyBzA4u37TQL2HRz1KW8N0aHqvL2WD6Y8MAmOvtc6QFkc-aLpizdDf-lUD3UEvvMAoZ5ir3hx5sCcpKh975D344qWhb2j_eAFbEXw0dO79tFXaQ15iC4jHgnqnYSvsyZl51_eUfK8LhDT4 |
	| project_id | 4978efb1e94543c09196b23ca79e0443                                                                                                                                                        |
	| user_id    | 7246b17734a242a29fa3bc2149af2b10                                                                                                                                                        |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

> **NOTE**: The token that is issued to you will be fully privileged - there's a key differentiation between the local Linux user (being a non-privileged account) and the user that we're authenticating as within OpenStack. This source file contains credentials for the **'admin'** user within the undercloud, and has no restrictions.

The vast majority of the TripleO functionality is now built into the OpenStack client tools, and can typically be invoked by either '**openstack undercloud \<command>**' or '**openstack overcloud \<command>**', depending on the cloud that needs to be actioned, although certain components may need to be interrogated individually.

As OSP director (via TripleO) controls bare-metal hardware to use as a base for the overcloud deployment, we can verify connectivity by asking it for the current list of hardware being used:

	$ openstack baremetal node list
	+--------------------------------------+--------------------+--------------------------------------+-------------+--------------------+-------------+
	| UUID                                 | Name               | Instance UUID                        | Power State | Provisioning State | Maintenance |
	+--------------------------------------+--------------------+--------------------------------------+-------------+--------------------+-------------+
	| acfc2f77-adb2-4c25-b89e-8628d8debeae | summit-controller1 | b57eef5c-860c-43a3-9a66-77e6e592298f | power on    | active             | False       |
	| a5c6c2ba-57d4-40c6-a3c6-86271d1479ff | summit-compute1    | 673c2bc2-8680-46c6-8b03-e3eeb3cff98f | power on    | active             | False       |
	| 9622d2c1-947e-428f-a63e-e2d45c4a564d | summit-compute2    | 9d431a30-1738-421e-933b-5f357dce6fcc | power on    | active             | False       |
	| 1d70281d-1fb7-4828-90c2-c0003f98eb7c | summit-networker1  | 68545239-2fac-4348-8968-d1e6d42d05e6 | power on    | active             | False       |
	+--------------------------------------+--------------------+--------------------------------------+-------------+--------------------+-------------+

Here you can see all of the overcloud nodes from the image we showed earlier on - all four overcloud machines with their power state as '**power on**' and a provision status as '**active**', signifying that these systems are on, and being used - and we know they are, as they're our overcloud itself.

Now we're ready to move onto our overcloud itself; we won't need to worry about the **'stackrc'** file from now on as we're not going to be deploying OpenStack or making any changes to the existing configuration; using it was only to display how the undercloud is managing the hardware that makes up our overcloud. From now on we'll use the overcloudrc file, as it will allow us to interact with, and investigate the pre-deployed overcloud environment. Getting access to that environment is as easy as sourcing the overcloudrc file:

	$ source ~/overcloudrc

Now, instead of the commands going to the undercloud, the commands will go to the **overcloud**, or more specifically, the OpenStack controller(s) running there. You should also notice that the command prompt will have updated to demonstrate that we're interactiving with the overcloud.

Now, let's briefly list out the current set of services that are offered by the overcloud; this will allow us to verify connectivity to our overcloud, and that our overcloudrc file is providing us with everything we need to do so:

	$ openstack service list
	+----------------------------------+------------+----------------+
	| ID                               | Name       | Type           |
	+----------------------------------+------------+----------------+
	| 011cbcb8ec59472f986226fe42ee4a09 | placement  | placement      |
	| 1af179a63e3f4411b080accd8c4eae77 | cinderv3   | volumev3       |
	| 25e3ec7de8ea4c498aed1d90684104b1 | keystone   | identity       |
	| 2b8e5d7341014ede93e28c0366205868 | heat-cfn   | cloudformation |
	| 36733da763024e6dbcc8d700f9644b85 | gnocchi    | metric         |
	| 6106a390fe994022bd904152832ea45c | ceilometer | metering       |
	| 6bba6fea02b54bacb54c68a5379032b0 | cinderv2   | volumev2       |
	| 7008b8e0bbfc42bd8132d684e3adc749 | neutron    | network        |
	| 9d39cc71feb54138af6c061198f5f94c | nova       | compute        |
	| b0c67504cfad4322ae797451dbabc567 | aodh       | alarming       |
	| ba933279a0a34d399a6e2d36f68212f8 | cinder     | volume         |
	| c3369e4a285d469794e1ae231cb4fa4f | heat       | orchestration  |
	| c51021ec815c44a08fa3aa4370dd4622 | panko      | event          |
	| d83434146a8c497d8dc5525d8e9ccf92 | glance     | image          |
	| dc7cc4f3d155495caced459ef120c8e7 | swift      | object-store   |
	+----------------------------------+------------+----------------+
	
And one way to absolutely verify that we're talking to the overcloud and not the overcloud is to check on the endpoints for a given service. Our fixed virtual IP for our OpenStack services on the external network (routable from the undercloud) is **192.168.122.100**:
	
	$ openstack endpoint show neutron
	+--------------+----------------------------------+
	| Field        | Value                            |
	+--------------+----------------------------------+
	| adminurl     | http://172.17.1.15:9696          |
	| enabled      | True                             |
	| id           | 9359b04363a14d1987fbc4f8fed643b9 |
	| internalurl  | http://172.17.1.15:9696          |
	| publicurl    | http://192.168.122.100:9696      |           <--- see here.
	| region       | regionOne                        |
	| service_id   | 7008b8e0bbfc42bd8132d684e3adc749 |
	| service_name | neutron                          |
	| service_type | network                          |
	+--------------+----------------------------------+

#**Extra Files**

In addition to these files you'll find a '**labs**' directory within the stack user's home directory. This will contain a number of different files that we'll be using over the next few lab sections, including all of the OSP director templates that were used for deployment. As we have a number of different OpenStack labs going on during the Red Hat Summit that our team have prepared, the same shared directory is available on all systems:

	$ ls -l ~/labs
	total 564048
	-rw-rw-r--. 1 stack stack  13267968 Feb 10  2017 cirros-0.3.5-x86_64-disk.img
	drwxrwxr-x. 2 stack stack        97 Mar 26 01:20 config
	drwxrwxr-x. 3 stack stack        71 Mar 25 20:07 director
	-rw-r--r--. 1 stack stack      1071 Mar 25 20:07 instackenv.json
	drwxrwxr-x. 3 stack stack        77 Mar 25 20:07 odl
	drwxrwxr-x. 3 stack stack       172 Mar 25 20:07 osp
	-rw-rw-r--. 1 stack stack 564330496 Mar 22 07:38 rhel-server-7.4-x86_64-kvm.qcow2

Don't worry about exploring for now, just know that we have these available for us.