#**Lab 2: Creating Users, Roles and Tenants via Keystone (Identity Service)**

##**Introduction**

Keystone is the identity management component of OpenStack, a common authentication and authorisation store. It's primarily responsible for managing users, their roles and the projects (tenants) that they belong to, in other words - who's who, what can they do, and to which groups they belong to; essentially a centralised directory of users mapped to the services they are granted to use. In addition to providing a centralised repository of users, Keystone provides a catalogue of services deployed in the environment, allowing service discovery with their endpoints (or API entry-points) for access. Keystone is responsible for governance in an OpenStack cloud, it provides a policy framework for allowing fine grained access control over various components and responsibilities.

As a crucial and critical part of OpenStack infrastructure, Keystone is used to verify *all* requests from end-users to ensure that what clients are trying to do is both authenticated and authorised. In modern day implementations of Keystone, we rely on non-persistent **tokens** that are issued to authenticated users; these are signed by Keystone and contain all of the necessary information for all OpenStack components to validate the users authority over the tasks that they're being asked to perform.


##**Creating Users**

As part of the lab we're going to allow you to create your own user and project using Keystone.

As we highlighted previously, we're going to use the **undercloud** machine as our 'command and control' system, and will be used to carry out all tasks. So, once you've logged onto your undercloud lab machine, you'll notice that there are a number of files sitting within the 'stack' users home directory. There are two (but really three) primary files that we need to be concerned with:

* **~/stackrc** - the environment 'source' file in which we can load in environment variables to configure the **undercloud** with the OpenStack command line tools (we won't be using this file during this particular session, but if you join for the OSP director lab later in the day, you will use this file extensively)<br><br>
* **~/overcloudrc** - the primary environment 'source' file that contains the OpenStack environment variables that will allow us to use the OpenStack command line tools against our **overcloud** deployment.<br><br>
* **~/overcloudrc.v3** - Essentially the same as the above, but uses Keystone v3 API as opposed to v2 by default; Red Hat OpenStack Platform is almost entirely utilising v3 at present, but some things still require v2, hence why we provide both of these files.<br><br>

The **overcloudrc** file will allow you to have admin access to the environment so that you can create your own user and project - just like Linux administration, the root user is not used for day-to-day tasks, hence why we're going to create our own one. Note that we're only going to be using this overcloudrc file for the purpose of this lab, we'll generate our own 'rc file' later on. First, let's source that file:

	$ source ~/overcloudrc
	
> **NOTE**: This file allows users to **administratively** control the overcloud, we'll need to use this with caution.
	
By running the above command, you will have configured environment variables that the OpenStack command line tools will use, e.g. the API location of Keystone, and the admin username and password. You'll also notice that the command line has the "**(overcloud)**" prefix to signify that you'll be issuing OpenStack commands against the overcloud by default.

The next few steps create a '**user**' account, a '**project**' (or 'tenant') which is a group of users, and a '**role**' which is used to determine permissions across the stack. You can choose your own username and password here, just remember what they are as we'll use them later, and choose something **generic** (i.e. not your usual password) as it'll be used in plaintext later in the lab. I've used my initials '**rdo**' for my user below:

	$ openstack user create --password-prompt <your username>
	User Password:
	Repeat User Password:
	+----------+----------------------------------+
	| Field    | Value                            |
	+----------+----------------------------------+
	| email    | None                             |
	| enabled  | True                             |
	| id       | 2c580c9e773143f5b4d82b9a6131b47a |
	| name     | rdo                              |
	| options  | {}                               |
	| username | rdo                              |
	+----------+----------------------------------+

> **NOTE**: The command shown above will ask you to choose a new password, but it will not be visible, even when typing it out.

Next, create a project for your user to reside in, let's use the project name '**demo**':

	$ openstack project create demo
	+-------------+----------------------------------+
	| Field       | Value                            |
	+-------------+----------------------------------+
	| description | None                             |
	| enabled     | True                             |
	| id          | f991d44fac91419c8e6016184381871a |
	| name        | demo                             |
	+-------------+----------------------------------+

Finally we can give the user a role and assign that user to the project we just created. Note that we're using usernames, roles and projects by their name here, but it's possible to use their id's instead. Let's assign our user the '**admin**' role, and place them into our new project, remebering to use the username and project name from the earlier commands:

	$ openstack role add --user <your username> --project demo admin
	+-----------+----------------------------------+
	| Field     | Value                            |
	+-----------+----------------------------------+
	| domain_id | None                             |
	| id        | 221700f25ec24d94b69fea75b6141da1 |
	| name      | admin                            |
	+-----------+----------------------------------+

To save time and to not have to worry about specifying usernames/passwords on the command-line, it's prudent to create another 'rc' or 'source' file which will load in environment variables as we used previously for the main admin user. Let's do the same for your new user. We first need to set some temporary environment variables, making sure that you substitute these values with your own:

	$ export USERNAME=<your username>
	$ export PASSWORD=<your password>
	
Now we can create our file:

	$ cat > ~/demorc <<EOF
	export OS_USERNAME=$USERNAME
	export OS_PROJECT_NAME=demo
	export OS_PASSWORD=$PASSWORD
	export OS_AUTH_URL=http://192.168.122.100:5000/v2.0/
	export NOVA_VERSION=1.1
	export OS_NO_CACHE=True
	export COMPUTE_API_VERSION=1.1
	export no_proxy=,192.168.122.100,172.16.0.30
	export OS_VOLUME_API_VERSION=3
	export OS_IMAGE_API_VERSION=2
	export OS_AUTH_TYPE=password
	export PS1="[\u@\h \W]\$ "
	export PS1="(overcloud-demo) \$PS1"
	EOF

You can test the file by logging out of your ssh session to the OpenStack virtual machine, logging back in and trying the following-

	$ logout
	$ ssh stack@undercloud

	$ openstack token issue
	Missing value auth-url required for auth plugin password
	
As you can see, without setting the environment variables, the authentication token request has failed. Now let's try it again when sourcing our new **demo** environment file for our new user:

	$ source ~/demorc
	$ openstack token issue
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| Field      | Value                                                                                                                                                                                   |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
	| expires    | 2018-04-09T14:28:04+0000                                                                                                                                                                |
	| id         | gAAAAABay2pkh0i9AQQJyNolx7zkdFQ-3xg102pW2aatmTjutWkl9TX5tV6_pcMxUraCZUCtnWOOIfgRnGt4zvhAQ3G9oks_c4aMHfBaXvyjSDKcj2-aotiT5YicrF5tLnrk-iE2cJbTi7iKbPnjljF7t6Pobf2sj2u3n8lknHynOpzEXqqU-Zs |
	| project_id | f991d44fac91419c8e6016184381871a                                                                                                                                                        |
	| user_id    | 2c580c9e773143f5b4d82b9a6131b47a                                                                                                                                                        |
	+------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

Note that the first attempt at running the "**openstack token issue**" command failed; it was either expecting you to specify the authorisation credentials as parameters or via environment variables. The second time it should have succeeded (and you'll be presented with a token id starting "**gAAA...**", if not, please check the contents of your **~/demorc** file, and ensure they match the username and password you created earlier in the lab. Once you save the file, you'll need to run '**source ~/demorc**' again to use the new environment variables.
