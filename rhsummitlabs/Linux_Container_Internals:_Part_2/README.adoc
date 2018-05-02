:scrollbar:
:data-uri:
:toc2:

== Linux Container Internals - Part 2

:numbered:

== Overview

Description:

This lab is focused on understanding how container images are built, tagged, organized and leveraged to deliver software in a range of use cases.

. Goal

By the end of this lab you should be able to:

- Understand the basic interactions of the major daemons and APIs in a typical container environment.
- Internalize the function of system calls and kernel namespaces.
- Understand how SELinux and sVirt secures containers.
- Command a conceptual understanding of how cgroups limit containers.
- Use SECCOMP to limit the system calls a container can make.
- Have a basic understanding of container storage and how it compares to normal Linux storage concepts.
- Gain a basic understanding of container networking and namespaces.
- Troubleshoot a basic Open vSwitch setup with Kubernetes/OpenShift.
- Understand the uses of multi-container applications.
- Internalize the difference between orchestration and application definition.
- Command basic container scaling principles.
- Use tools to troubleshoot containers in a clustered environment.


=== Environment

The demo environment is deployed using katacoda at https://katacoda.com/fatherlinux/courses/subsystems/container-internals-lab-2.

== Conclusion
In this course you learned:

- Understand the basic interactions of the major daemons and APIs in a typical container environment
- Internalize the function of system calls and kernel namespaces
- Understand how SELinux and sVirt secures containers
- Command a conceptual understanding of how cgroups limit containers
- Use SECCOMP to limit the system calls a container can make
- Have a basic understanding of container storage and how it compares to normal Linux storage concepts
- Gain a basic understanding of container networking and namespaces
- Troubleshoot a basic Open vSwitch setup with Kubernetes/OpenShift
- Multi-Container Applications: The classic two-tiered, wordpress application
- Cluster Performance: Scaling applications horizontally with containers
- Cluster Debuggin: Troubleshooting in a distributed systems environment
