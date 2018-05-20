:scrollbar:
:data-uri:
:toc2:

== Linux Container Internals - Part 1

:numbered:

== Overview

Description:

In this lab you'll gain a basic understanding of the moving parts that make up the typical container architecture. This will cover container hosts, daemons, runtimes, images, orchestration, etc.

. Goal

By the end of this lab you should be able to:

* Draw a diagram showing how the Linux kernel, services and daemons work together to create and deploy containers.
* Internalize how the architecture of the kernel and supporting services affect security and performance.
* Explain the API interactions of daemons and the host kernel to create isolated processes.
* Command the nomenclature necessary to technically discuss container repositories, image layers, tags, registry server and other components.
* Understand what the Open Containers Initiative and why this standard is important for your container images.
* Internalize the difference between base images and multi-layered images.
* Understand the full URL to an image/repository.
* Command a complete understanding of what is inside of a container image.
* Use layers appropriately in your architecture and design.


=== Environment

The demo environment is deployed using katacoda at https://katacoda.com/fatherlinux/courses/subsystems/container-internals-lab-1.

== Conclusion
In this course you learned:

- Containers Are Linux: Userspace libraries interact with the kernel to isolate processes
- Single Host Toolchain: Includes Docker runtime, Systemd, and Lincontainer
- Multi-Host Toolchain: Includes Kubernetes/OpenShift
- Typical Architecture: Explains what a production cluster looks like
- Community Landscape: Explains the basics of the upstream projects and how they are contributing
- Understand what the Open Containers Intiative and why this standard is important for your container images
- Internalize the difference between base images and multi-layered images
- Understand the full URL to an image/repository
- Command a complete understanding of what is inside of a container image
- Use layers appropriately in your architecure and design
