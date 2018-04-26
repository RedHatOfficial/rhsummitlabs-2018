<img src="images/redhat.png" style="width: 350px;" border=0/>

<h2>Red Hat Summit, Boston 2018</h2>
**Title**: Hands on with Red Hat OpenStack Platform director (**L1010**)<br>
**Date**: 4th May 2018<br>

**Authors**:
<ul class="tab">
<li>Rhys Oxenham <<roxenham@redhat.com>></li>
<li>Jacob Liberman <<jliberma@redhat.com>></li>
</ul>


#**Lab Contents**#

* **Lab 1:** Getting Started
* **Lab 2:** Investigation of Lab Environment
* **Lab 3:** Import of Node Definitions
* **Lab 4:** Inspection of Nodes
* **Lab 5:** Node Tagging
* **Lab 6:** Customised Setup (Roles and Environmental Changes)
* **Lab 7:** Network Setup
* **Lab 8:** Deployment of Overcloud
* **Lab 9:** Environment Scale Out


<!--BREAK-->

#**Lab Overview**

First of all, it's my pleasure to welcome you to the Red Hat Summit 2018, here at the San Francisco Moscone Centre! The past few years have been an exciting time for both Red Hat and the OpenStack community; we've seen unprecedented interest and development in this new revolutionary technology and we're proud to be at the heart of it all. Red Hat is firmly committed to the future of OpenStack; our goal is to continue to enhance the technology, make it more readily consumable and to enable our customers to be successful when using it.

This hands-on lab aims to get you, the attendees, a bit closer to the deployment side of Red Hat OpenStack Platform, by utilising Red Hat OpenStack Platform director, Red Hat's deployment and lifecycle management tool for our OpenStack distribution. It's comprised of a number of individual steps inside of this lab-guide that will run you through some of the more common tasks, such as node initialisation, network configuration, OpenStack deployment, custom architectures, post-deployment modifications, and scaling operations. This will give you a solid foundation and a basic understanding of the tools that Red Hat are providing for ongoing OpenStack management, but given the short amount of time that we have for the lab, the activities have to be limited.

In the interest of time, we've provided a partially installed environment where the deployment tools are ready to go, but we will have to go through the major steps of OpenStack deployment. All of the machines that you'll be interacting with are virtual machines themselves, and are running on the workstation you're sat at, but have been configured to mimic real world hardware, so you shouldn't be disadvantaged with your experiences. Please start with the first lab, this will get you used to the environment infront of you and will allow you to get started.

> **NOTE**: If you've **not** been provided with connection details or you do not see your unique session information on-screen, please ask and we'll ensure that access is provided.

If you have any problems at all, please put your hand-up and an attendee will be with you shortly to assist - we've asked many of our OpenStack experts to be here today, so please make use of their time. The materials are yours to take away with you, I hope that they'll be useful assets in the future.

#**Background Information**

> **NOTE**: This information is provided for your information and understanding of the lab content and the principles of Red Hat OpenStack Platform director (and the components in which it's built from). If you're already confident with OpenStack deployment and the components of **TripleO** then you can skip this, or perhaps refer back to it if you're unsure during the lab steps.

Those that are familiar with OpenStack will already be aware that its deployment has (historically) been a difficult task.  Deployment is a lot more than just getting the software installed - it’s about architecting your platform to make use of existing infrastructure investments as well as planning for future scalability and flexibility. OpenStack itself is designed to be a massively scaleable platform, in which its components can be distributed in a wide variety of configurations to meet the needs of the organisation. Each OpenStack component exposes a RESTful API for communication, and relies on a shared message bus and database backend for persistence and cross-service communication, and as a result there’s no “one size fits all” way of deploying an OpenStack platform, but there are some sensible choices that should be made.

Despite having a hugely distributed architecture, there are some deployment trends that have emerged since OpenStack’s inception. Most OpenStack deployments consist of machines that have a designated role, where such a role comprises of specific OpenStack components or supporting services that are grouped together to perform a specific job within the cluster, creating a much simpler architecture to design, implement, and eventually scale.


<img src="images/osp-arch.png" style="width: 1000px;"/>

In the diagram, we’re showing a typical OpenStack deployment with most common roles identified. These usually comprise of a **controller** node, in which the cluster management and orchestration takes place, **compute** nodes in which virtual machines, or the workloads typically execute, and also **storage** nodes, where access to a workload’s persistent storage is managed. The way in which these various different role types are deployed across a given set of machines is important to get right to sensibly make use of available hardware resources, to ensure cluster stability in the event of failure, and to accommodate any future growth requirements within the environment. Utilising machines with an identical specification within a given role makes the support and maintenance easier, but critically provides predictability and consistency with overall infrastructure performance.

It’s also important to note that OpenStack should really be considered a framework for how an Infrastructure as a Service cloud could be constructed, and not a true implementation. This is due to its reliance on plugins and drivers to actually implement the functionality an organisation desires based on their existing technology choices. For example, a customer may already have a preferred storage or networking vendor; OpenStack, through its extensibility and plugin frameworks allow organisations to continue to leverage existing storage and networking investments, or have flexibility in utilising alternative choices later down the line, should a plugin or driver exist for that technology choice.

So it really isn’t a case of simply installing OpenStack - deployment also encompasses a suitable architecture design as well as ensuring that the chosen technologies can be integrated successfully. Given the vast number of ways that OpenStack can be architected, vendors such as Red Hat have outlined a number of best practices on how OpenStack components should be distributed, with high availability, workload resilience, future scalability, and ease of management at the heart of the design. Whilst the vast majority of vendors agree that grouping certain services together into roles makes deployment more simple, the means in which the components are installed and configured varies greatly across OpenStack distributions. Red Hat has always been an advocate of choice; it does not discriminate against a particular means of deployment but strongly recommends that customers utilise the official installation platform shipped with our products. By using the default tooling, customers are always deploying as per Red Hat’s recommended and best practices for the OpenStack deployment. This leads to a much more supportable configuration as the topology and configuration is well known and is constantly kept up to date with our latest recommendations. These best practices ensure that if a support issue does arise, we can react quickly to it, without having to spend time understanding the architecture that’s been implemented.

Red Hat OpenStack Platform version 7, based on the upstream **Kilo** release, introduced Red Hat OpenStack Platform director as the official deployment and ongoing management platform for our OpenStack distribution, and that’s what we’re going to be utilising during this lab. The high-level aim of OSP director is to allow organisations to start from nothing, and end up with a fully operational OpenStack installation, one that is robust, resilient, ready to scale, and is capable of integrating with a wide variety of existing customer technology choices and configurations. The concept of OSP director was to provide a tool that wasn’t just for the installation of OpenStack, but to truly become an ongoing lifecycle management platform. Red Hat realised that the initial installation of OpenStack was only a small piece of the puzzle when it came to our customers using the technology in production. As such, OSP director has been designed from the ground up to not only provide administrators with a mechanism for OpenStack installation but to bridge the gap between day-one activities, and the ongoing operational requirements of the environment. Red Hat has prioritised development in three major areas:

<br />
<img src="images/lifecycle.png" style="width: 1000px;"/>


Firstly, the **pre-deployment** planning stage. OSP director provides a platform for administrators to pre-set the target architecture when it comes to networking and storage topologies, OpenStack service parameters, integrations to third party plugins, and any other configurations as may be necessary to suit the requirements of their organisation. It also ensures that target hardware nodes are prepped and are ready to be deployed used.

Secondly, the **deployment** stage. This is where the bulk of the OSP director functionality is executed. One of the most important steps is ensuring that the proposed configuration is sane, there’s no point in trying to deploy a configuration if we are sure it will fail due to pre-flight validation checking. Assuming that the configuration is valid, OSP director needs to take care of the end to end orchestration of the deployment, including hardware preparation, software deployment, and once up and running, ensuring that the OpenStack environment performs as expected.

Lastly, Red Hat has focussed on delivering day-two, or **operational** functionality to OSP director, allowing administrators to have visibility into the ongoing health of the environment and to perform life-cycle changes, such as adding or replacing OpenStack nodes for scaling and decommissioning purposes, and to also automatically upgrading between major versions, for example moving between OpenStack Mitaka and OpenStack Newton. OSP director has strong technology foundations, and is a convergence of years of upstream engineering work, established technology created for earlier deployment tooling, and technologies that came to us via acquisition. Red Hat has worked tirelessly to align these different technologies, taking in years worth of experience and expertise at deploying OpenStack at scale, which has allowed us to create a powerful, best of breed deployment tool that's in-line with the overall direction of the OpenStack project.

At the heart of OSP director is **TripleO**, short for “**OpenStack on OpenStack**”. TripleO is an OpenStack project that aims to utilise OpenStack itself as the foundations for deploying OpenStack. To clarify, TripleO advocates the use of native OpenStack components, and their respective API’s to configure, deploy, and manage OpenStack environments itself. The major benefit of utilising these existing API's with OSP director is that they're well documented, they go through extensive integration testing upstream, are mature, and for those that are already familiar with the way that OpenStack works, it's a lot easier to understand how TripleO (and therefore, OSP director) works. Feature enhancements, security patches, and bug fixes are therefore automatically inherited into OSP director, without us having to play catch up with the community. In addition Red Hat is providing, and is heavily contributing to, an upstream sanctioned mechanism of deploying OpenStack; this is not something that’s being built in isolation. Using TripleO ensures that we have community buy in and support for the future direction of the project.

One of the most important concepts to understand with TripleO is the notion of the **undercloud, vs the overcloud**. The diagram shown below attempts to visually demonstrate this:

<img src="images/overcloud-undercloud.png" style="width: 1000px;"/>

With TripleO, there are two distinct types of cloud that are spoken of. The first to consider is the **undercloud**, this is the command and control cloud in which a smaller OpenStack environment exists that's sole purpose is to **bootstrap** a larger production cloud, known as the **overcloud**, where tenants and their respective workloads reside. OSP director itself is synonymous with the undercloud; OSP director bootstraps the undercloud OpenStack deployment and provides the necessary tooling to deploy an overcloud via the mechanisms of TripleO. As we'll see over the next couple of hours, TripleO relies on a number of key OpenStack components to deploy an overcloud, namely **Heat** for orchestration, **Nova** (and **Ironic**) for baremetal deployment, **Neutron** for network management, and many more to supply supporting services. From OSP12, TripleO deploys all OpenStack services in containers, which we'll also be exploring over the next few labs.
