#**Lab 1: Getting Started**

The OpenStack environment that we're going to be using has been preinstalled and largely preconfigured for the purposes of this lab. We've done our best to preconfigure the classroom and ensure that the cloud-based virtual machines that make up the infrastruture are ready to go at the start of the lab, but we need to ensure that you're able to log in to the environment, as the workstation you're at will be used for multiple different labs during the Red Hat Summit.

# Lab Environment

The environment, despite running in the public cloud, has been deployed with OSP director, Red Hat's deployment and lifecycle management platform for OpenStack. As such, it's been deployed with the TripleO methodology, i.e. a smaller 'bootstrap' OpenStack cloud, known as the **undercloud** deploys the 'production' cloud known as the **overcloud**, i.e. where your workloads would actually run. The overcloud that has been deployed in the lab environment is a simple configuration, comprising of a single controller, single networker, and dual compute node setup. All of these nodes, the undercloud, and all overcloud nodes are virtual machines running within a unique session on-top of the public-cloud ready for your consumption, roughly looking like the following:

<img src="images/lab-setup.png" style="width: 1000px;"/>

> **NOTE**: In the above image you'll see the **bastion** host is the one that we'll be using as a jump host, i.e. the machine that we connect to first to establish a connection into the lab environment that we'll be using for the rest of the labs; this is the only virtual machine in your unique environment that has secure shell access exposed to the public internet.

In this lab, you **won't** be utilising the undercloud platform to deploy any overcloud infrastructure - this has already been done for you, but you will be using it as the base in which we carry out our labs. If you're interested in learning more, there's a later lab today, **L1010** ([Hands on with Red Hat OpenStack Platform director](https://agenda.summit.redhat.com/SessionDetail.aspx?id=153520)) in the same room, which will allow you to build upon your OpenStack knowledge and experience the deployment side.

Using a virtualised infrastructure inside of the public cloud allows us to have full control over all of the network and storage without impacting other lab users, and whilst we won't be running any intensive workloads, it allows us to build up and test OpenStack in a short amount of time and with great flexibility. To re-iterate, we'll first be connecting to the **jump host**, and then further connecting to our **undercloud** machine as the conduit into our overcloud OpenStack environment. The undercloud will be used for both executing commands on the overcloud, and also as a conduit for connecting to our overcloud nodes and any deployed resources when required to do so.

# Lab Access

We're using the Red Hat Product Demo Suite (RHPDS) for our labs, and therefore we need to request and get access to a unique environment based within the public cloud for you to use to complete the lab steps. If you're a Red Hat employee you'll need to follow these instructions to generate a session, othewise please get the connection details from your Red Hat representative and skip to the '**Connecting**' part below where we're connecting via secure-shell to the environment provided.

> **NOTE**: Only proceed with the RHPDS creation instructions below if you're a Red Hat employee, or have been given access to RHPDS as a partner.

First you'll need to request a session via RHPDS, the WebUI (and associated login page) can be found at [https://rhpds.redhat.com/](https://rhpds.redhat.com/). Once you've logged in, navigate to the service catalogue by selecting '**Services**' --> '**Catalogs**', and navigate to the correct lab that you want to access by clicking '**Order**' on the right hand side. This lab is '**Hands on with RH OSP**' and should look like the following:

<img src="images/order-osp1.png" style="width: 1000px;"/>

Once you select 'Order' you'll be presented with the following page which you'll need to accept some terms about the order time and the expiry:

<img src="images/order-osp2.png" style="width: 1000px;"/>

Select **'Submit'** at the bottom of the page and it should generate the environment for you, and will show up in your requests:

<img src="images/requests.png" style="width: 1000px;"/>

> **NOTE**: This is a generic screenshot above, your output might look slightly different if you're using a different lab.

The RHPDS system will now generate a unique environment for you to use and you will receive an email with some of the connection details. These details uniquely identify your session to ensure that you are connecting to your unique environment, see here for an example:

<img src="images/email-osp.png" style="width: 1000px;"/>

You'll notice that it contains some links, specifically the "**External Hostname**" for the **WORKSTATION** system - this is the **jumphost** that you'll be connecting to from the outside, and it has a unique hostname to connect to from the outside that's routable over the internet. Here, mine is **"osp-88e0.rhpds.opentlc.com"**. In addition, there are links to other areas such as the Horizon dashboard that you'll likely use later in the lab, as well as a link to these labs.

# Connecting

You'll see that my assigned lab UUID for my environment is '**88e0**' and is used to uniquely identify my session, and is used as part of the connection address. The environment takes around 20-30 minutes to power-up, and this should have already been done for you prior to the session starting, but don't be alarmed if you cannot connect in straight away, it may just require a few more minutes. Use the exact connection address that it provides you on your screen by copying and pasting the text from the webpage into a terminal emulator, here I'm using my example but **you'll need to replace this with your own username and unique session**:

	$ ssh osp-88e0.rhpds.opentlc.com -l (your RHPDS username)
	The authenticity of host 'osp-88e0.rhpds.opentlc.com (129.146.91.32)' can't be established.
	ECDSA key fingerprint is SHA256:SqbVF0TGdHuTsoDChp6/cw4jFHqwJlBWFOeqwd88Bi4.
	Are you sure you want to continue connecting (yes/no)? yes
	(...)

> **NOTE**: The above assumes that you've associated your public secure shell key with RHPDS - if you have not done so, please update it [here](https://account.opentlc.com/update/). If you have associated your key already then you're good to go and you shouldn't be required to use a password. **If you have been assigned a system from a Red Hat employee, ensure he/she provides you with a username and keypair to use**.

<br />
If successful, we can jump straight to our **undercloud** machine, as this is the one that we're going to be using for all of the lab sections, note that we're using sudo below as the root user on the jump host is the only one configured with the ssk-keys:

	$ sudo ssh stack@undercloud

**Only** if this is unsuccessful (e.g. for some reason that there's no entry in /etc/hosts), attempt the following:

	$ sudo ssh stack@192.168.122.253
	
You will have full root access (via sudo) and control over this virtual machine, and we'll run our tasks directly here. If you're still unable to connect into your environment after a few minutes, please ask for assistance.
