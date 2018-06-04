<img src="images/redhat.png" style="width: 350px;" border=0/>

<font color="red">
**Lab Update - 4th June 2018**

**This lab has now been updated to run on the Red Hat Product Demo System (RHPDS) and so Summit instructions have been removed in favour of specific instructions for RHPDS. You can skip to the first lab section if you're following this post-Summit. If you have any questions or any problems accessing this content, please let us know.**
</font>

<h2>Red Hat Summit, San Francisco 2018</h2>
**Title**: First time hands-on with Red Hat OpenStack Platform (**L1009**)<br>
**Date**: 9th May 2018<br>

**Authors/Lab Owners**:
<ul class="tab">
<li>Rhys Oxenham <<roxenham@redhat.com>></li>
<li>Jacob Liberman <<jliberma@redhat.com>></li>
</ul>


#**Lab Contents**#

1. **Getting Started**
2. **Creating Users, Roles, Tenants, and policies via Keystone (Identity Service)**
3. **Creating/Adding Images into Glance (Image Service)**
4. **Creation and configuration of Networks in Neutron (Networking Service)**
5. **Creation of Instances in Nova (Compute Service)**
6. **Creation and Attachment of Block Devices with Cinder (Volume Service)**
7. **Deployment of Application Stacks using Heat (Orchestration)**
8. **Deployment of Load Balancers using Neutron's LBaaS feature**


<!--BREAK-->

#**Lab Overview**

First of all, it's my pleasure to welcome you to the Red Hat Summit 2018, here at the San Francisco Moscone Centre! The past few years have been an exciting time for both Red Hat and the OpenStack community; we've seen unprecedented interest and development in this new revolutionary technology and we're proud to be at the heart of it all. Red Hat is firmly committed to the future of OpenStack; our goal is to continue to enhance the technology, make it more readily consumable and to enable our customers to be successful when using it.

This hands-on lab aims to get you, the attendees, a bit closer to Red Hat OpenStack Platform. It's comprised of a number of individual steps inside of this lab-guide that will run you through some of the more common tasks, such as initial configuration, network management, image deployment, block storage assignment, and virtual machine provisioning; giving you a hands-on overview of OpenStack, how the components fit together, and how to use it. We will use a combination of command-line tools and interaction via the OpenStack Dashboard (Horizon).

Whilst you'll be asked to configure some fundamental components within OpenStack, you won't need to install OpenStack from scratch within this lab, we've provided a pre-installed OpenStack environment with a simple configuration - a one controller, one networker, and two compute node layout. These machines will be virtual machines themselves, running on-top of a shared public cloud environment. You will have been provided with the necessary connection details on-screen and the first lab will demonstrate how to connect into the environment and how to get started with the lab sections.

> **NOTE**: If you've **not** been provided with connection details or you do not see your unique session information on-screen, please ask and we'll ensure that access is provided.

If you have any problems at all or have any questions about Red Hat or our OpenStack distribution, please put your hand-up and a lab moderator will be with you shortly to assist - we've asked many of our OpenStack experts to be here today, so please make use of their time. If you have printed materials, they're yours to take away with you, otherwise the online copy will be available for the foreseeable future; I hope that they'll be useful assets in your OpenStack endeavours.
