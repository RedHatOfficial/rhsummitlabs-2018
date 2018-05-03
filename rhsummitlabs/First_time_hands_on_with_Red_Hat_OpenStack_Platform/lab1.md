#**Lab 1: Getting Started**

The OpenStack environment that we're going to be using has been preinstalled and largely preconfigured for the purposes of this lab. We've done our best to preconfigure the classroom and ensure that the cloud-based virtual machines that make up the infrastruture are ready to go at the start of the lab, but we need to ensure that you're able to log in to the environment, as the workstation you're at will be used for multiple different labs during the Red Hat Summit.

# Lab Environment

The environment, despite running in the public cloud, has been deployed with OSP director, Red Hat's deployment and lifecycle management platform for OpenStack. As such, it's been deployed with the TripleO methodology, i.e. a smaller 'bootstrap' OpenStack cloud, known as the **undercloud** deploys the 'production' cloud known as the **overcloud**, i.e. where your workloads would actually run. The overcloud that has been deployed in the lab environment is a simple configuration, comprising of a single controller, single networker, and dual compute node setup. All of these nodes, the undercloud, and all overcloud nodes are virtual machines running within a unique session on-top of the public-cloud ready for your consumption, roughly looking like the following:

<img src="images/lab-setup.png" style="width: 1000px;"/>

> **NOTE**: In the above image you'll see the **bastion** host is the one that we'll be using as a jump host, i.e. the machine that we connect to first to establish a connection into the lab environment that we'll be using for the rest of the labs; this is the only virtual machine in your unique environment that has secure shell access exposed to the public internet.

In this lab, you **won't** be utilising the undercloud platform to deploy any overcloud infrastructure - this has already been done for you, but you will be using it as the base in which we carry out our labs. If you're interested in learning more, there's a later lab today, **L1010** ([Hands on with Red Hat OpenStack Platform director](https://agenda.summit.redhat.com/SessionDetail.aspx?id=153520)) in the same room, which will allow you to build upon your OpenStack knowledge and experience the deployment side.

Using a virtualised infrastructure inside of the public cloud allows us to have full control over all of the network and storage without impacting other lab users, and whilst we won't be running any intensive workloads, it allows us to build up and test OpenStack in a short amount of time and with great flexibility. To re-iterate, we'll first be connecting to the **jump host**, and then further connecting to our **undercloud** machine as the conduit into our overcloud OpenStack environment. The undercloud will be used for both executing commands on the overcloud, and also as a conduit for connecting to our overcloud nodes and any deployed resources when required to do so.

# Connecting

As previously highlighted, the workstation you're sat at will be used by many different lab sessions, yet each of you will have been allocated a unique environment based within the public cloud for you to use to complete the lab steps associated with each session. On screen you should already have a tab open to the lab request form where we need to select the correct lab. If you don't have this in front of you, it's available [here](https://www.opentlc.com/guidgrabber/guidgrabber.cgi).

What you should see is as follows, noting that I've already selected the correct lab (**'L1009 - First time hands-on with Red Hat OpenStack Platform'**) and have entered the activation key '**openstack**', which you will need to do too:

<img src="images/lab-request1.png" style="width: 1000px;"/>

This will allocate a pre-deployed session for your usage with **GUID** that's used to uniquely identify your session, and will provide you with instructions on how to connect to the **jumphost** that we discussed above. Here's an example below:

<img src="images/lab-request2.png" style="width: 1000px;"/>

You'll see that my assigned lab GUID is '**faa6**' and is used to uniquely identify my session, and is used as part of the connection address. Halfway down the page you'll see an ssh command that you'll need to use to connect to the environment. The environment takes around 20 minutes to power-up, and this should have already been done for you prior to the session starting, but don't be alarmed if you cannot connect in straight away, it may just require a few more minutes. Use the exact connection address that it provides you on your screen by copying and pasting the text from the webpage into a terminal emulator, here I'm using my example but you'll need to **replace** this with your own:

	$ ssh osp-faa6.rhpds.opentlc.com
	The authenticity of host 'osp-faa6.rhpds.opentlc.com (129.146.91.32)' can't be established.
	ECDSA key fingerprint is SHA256:SqbVF0TGdHuTsoDChp6/cw4jFHqwJlBWFOeqwd88Bi4.
	Are you sure you want to continue connecting (yes/no)? yes
	(...)

> **NOTE**: If you lose your connection details you can return [here](https://www.opentlc.com/guidgrabber/guidgrabber.cgi) at any time.

<br />
Next, we can jump straight to our **undercloud** machine, as this is the one that we're going to be using for all of the lab sections, note that we're using sudo below as the root user on the jump host is the only one configured with the ssk-keys:

	$ sudo ssh stack@undercloud

**Only** if this is unsuccessful (e.g. for some reason that there's no entry in /etc/hosts), attempt the following:

	$ sudo ssh stack@192.168.122.253
	
You will have full root access (via sudo) and control over this virtual machine, and we'll run our tasks directly here. If you're still unable to connect into your environment after a few minutes, please ask for assistance.
