#**Lab 2: Investigation of Lab Environment**

In this section we're going to take a brief look of our lab environment and how it works. At this stage it will be very bare and won't have a lot to see, but it's important that we explore the core components and demonstrate some of the files that we're going to be interacting with. Here's what the lab setup looks like:

<img src="images/lab-environment.png" style="width: 1000px;"/>

There are six total machines, if you include the jump-host, that we'll be utilising. In a previous step we used the jump-host to connect into the undercloud (in **blue**), which is a small bootstrap OpenStack environment that's used to deploy the overcloud (in **red**). The overcloud is what we're going to be configuring and deploying in this Red Hat Summit Lab. Sitting on-top of all of these systems are dedicated VLANs that are used to segment OpenStack network traffic, e.g. ensuring that internal API communication is isolated from tenant network traffic, and so on.

Now that we've successfully connected from the public internet to the jump-host, and have used that to get into our undercloud machine, let's explore what the current setup looks like a little further. All steps, unless explicitly mentioned, will be executed as the '**stack**' user on the undercloud machine; this is a non-privileged user account and will be used to perform all requirements of the lab sections - we will not need the root account, although you should have sudo access if you want to explore a bit further.

As highlighted previously, we'll be operating with two separate clouds over the next two hours; both of which will be running OpenStack - the **undercloud** and the **overcloud**; it's the undercloud that's used to **bootstrap** the overcloud and is the only one currently running.

# The ~/stackrc file

Assuming that you're still connected to your **undercloud** machine as the stack user,  you'll notice that we've left a few files in the home directory. The most important file here is going to be the '~/stackrc' file. Upon initial installation of the undercloud (not covered in this lab), OSP director generates a file called '**stackrc**'; this file resides in the stack user's home directory and is a source of environment variables that enable the OpenStack command line tools to execute commands against the **undercloud** itself. Let's check this file out:

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
	
<br />
The token that was issued to you is from the undercloud - there's no deployed overcloud at this stage - all we've done is proven that authentication is working and that we're able to interrogate core OpenStack services. If we take this further, we can demonstrate all of the OpenStack services that are being utilised by the undercloud:

	$ openstack service list
	+----------------------------------+------------------+-------------------------+
	| ID                               | Name             | Type                    |
	+----------------------------------+------------------+-------------------------+
	| 020e49c02aff485a92bebde62f6a6d8f | zaqar-websocket  | messaging-websocket     |
	| 06d83ad6e555412081518d9e0658474a | ironic-inspector | baremetal-introspection |
	| 1310a1e39a844926b530b349d9155565 | keystone         | identity                |
	| 17ae013660c44ce0a13f84e05d3ebd21 | nova             | compute                 |
	| 2cd7958e466143dc90172d51515c83e2 | heat-cfn         | cloudformation          |
	| 30d74847e8bf4bd0b78e2c041ac17992 | mistral          | workflowv2              |
	| 4dabb73147ce4ddbb2b5ae5ecadb0b66 | ironic           | baremetal               |
	| 599a0291ee6b4f3eb8e843413077078c | neutron          | network                 |
	| 799aa7da322e4ce0a3e3872eff1faddf | placement        | placement               |
	| 9cfa0d0b1b554046911825cf73ee2467 | zaqar            | messaging               |
	| b75c135b59d94a84a1468c9166f6179f | glance           | image                   |
	| be8018595e6e486bab00508c6241ef02 | swift            | object-store            |
	| daaf74254f2645ff8c46adcc28b177a8 | heat             | orchestration           |
	+----------------------------------+------------------+-------------------------+

The vast majority of the TripleO functionality is now built into the OpenStack client tools, and can typically be invoked by either '**openstack undercloud \<command>**' or '**openstack overcloud \<command>**', depending on the cloud that needs to be actioned, although certain components may need to be interrogated individually, for example:

	$ openstack baremetal node list
	(Empty)

> **NOTE**: It's intended that the above command returns nothing - we have the "baremetal" nodes defined on the underlying hypervisor, but they're not yet known to the undercloud. In a later lab we're going to be pulling them into the control of the undercloud.

#**Extra Files**

In addition to these files you'll find a '**labs**' directory within the stack user's home directory. This will contain a number of different files that we'll be using over the next few lab sections, including examples of all of the template files and test scripts that we may want to use. As we have a number of different OpenStack labs going on during the Red Hat Summit that our team have prepared, the same shared directory is available on all systems:

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
