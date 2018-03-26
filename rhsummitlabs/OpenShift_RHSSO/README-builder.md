# AWS

## Pre-Reqs:

- AWS account
- sudo dnf install python2-boto 
- sudo pip install boto3
- An IAM account with API key at Amazon
- vars.yml filled in. specifically set vpc and cloud_vendor
- ~/.ansibleawscreds exists like so

> aws_access_key: "--REMOVED--" 
> aws_secret_key: "--REMOVED--"

- ~/.ansiblerhsmcreds exists like so

>rhsm_username: "--REMOVED--"
>rhsm_password: "--REMOVED--"

## Usage
See the general ansible usage notes.  All of the commands there work for AWS. 


# Using Ravello

## Pre-Reqs:
- Ravello account and ssh key uploaded
- vars.yml filled in. specifically set cloud_vendor
 - ~/.ansiblerhsmcreds exists like so

>rhsm_username: "--REMOVED--"
>rhsm_password: "--REMOVED--"


## Usage
 - build using custom canvas and pre-built RHEL images 
 - set VM names according to standard #host
 - set VM hostnames according to standard 
 - make sure everything is virtio
 - US East 5, Performance
 - General > Advanced > preferPhysicalHost: true
 - System what you want and timeout of 2
 - Disks what you want
 - NIC: DHCP, Reserved IP, no auto-mac
 - Expose ports under services
 - set cloud-init to SSH key you uploaded and to 

```
#cloud-config

# This is an example, please update the key and password with your own!

ssh_pwauth: True
disable_root: False

users:
  - name: root
    lock_passwd: false
    ssh_authorized_keys:
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCvZvn+GL0wTOsAdh1ikIQoqj2Fw/RA6F14O347rgKdpkgOQpGQk1k2gM8wcla2Y1o0bPIzwlNy1oh5o9uNjZDMeDcEXWuXbu0cRBy4pVRhh8a8zAZfssnqoXHHLyPyHWpdTmgIhr0UIGYrzHrnySAnUcDp3gJuE46UEBtrlyv94cVvZf+EZUTaZ+2KjTRLoNryCn7vKoGHQBooYg1DeHLcLSRWEADUo+bP0y64+X/XTMZOAXbf8kTXocqAgfl/usbYdfLOgwU6zWuj8vxzAKuMEXS1AJSp5aeqRKlbbw40IkTmLoQIgJdb2Zt98BH/xHDe9xxhscUCfWeS37XLp75J root@gpteadmin
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDIYvn2Swnf02le9As9fqtURqUMKWJZVpWAHR25NeCtM5JYq7QYvYIBY1PzscXQmGtBxrW6xG08hFLVr1H7Anlt8YH+/ltniVT9TmeO7UVWPN44+02LyEOg/izBBUe6Pc5ytR47CHVqi/zYnh4XpkRbkYXZFhoeHRxQciD9b8XXobOKTNO+hOpNyu9tLCfVb59C1MD1OiPdVImi38CVMZ5MTTviZ/y7zD2QZLgn+a7INa1TKoEh6XEQhiGDKiKvGJi/7qwUT+qvovcZTHtRcxbK6TQWBVBH7BRs+4xiIV+bPVyce6OXEJAIEfq3+cOH71mSss77jmbMTP1ijDjceFl3 prutledg@kronus
  - name: ec2-user
    lock_passwd: false
    sudo: ALL=(ALL) NOPASSWD:ALL
    ssh_authorized_keys:
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCvZvn+GL0wTOsAdh1ikIQoqj2Fw/RA6F14O347rgKdpkgOQpGQk1k2gM8wcla2Y1o0bPIzwlNy1oh5o9uNjZDMeDcEXWuXbu0cRBy4pVRhh8a8zAZfssnqoXHHLyPyHWpdTmgIhr0UIGYrzHrnySAnUcDp3gJuE46UEBtrlyv94cVvZf+EZUTaZ+2KjTRLoNryCn7vKoGHQBooYg1DeHLcLSRWEADUo+bP0y64+X/XTMZOAXbf8kTXocqAgfl/usbYdfLOgwU6zWuj8vxzAKuMEXS1AJSp5aeqRKlbbw40IkTmLoQIgJdb2Zt98BH/xHDe9xxhscUCfWeS37XLp75J root@gpteadmin
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDIYvn2Swnf02le9As9fqtURqUMKWJZVpWAHR25NeCtM5JYq7QYvYIBY1PzscXQmGtBxrW6xG08hFLVr1H7Anlt8YH+/ltniVT9TmeO7UVWPN44+02LyEOg/izBBUe6Pc5ytR47CHVqi/zYnh4XpkRbkYXZFhoeHRxQciD9b8XXobOKTNO+hOpNyu9tLCfVb59C1MD1OiPdVImi38CVMZ5MTTviZ/y7zD2QZLgn+a7INa1TKoEh6XEQhiGDKiKvGJi/7qwUT+qvovcZTHtRcxbK6TQWBVBH7BRs+4xiIV+bPVyce6OXEJAIEfq3+cOH71mSss77jmbMTP1ijDjceFl3 prutledg@kronus      
chpasswd:
  list: |
    root:RHSummit2018IAM!
    ec2-user:RHSummit2018IAM!
  expire: False
```

- update
- Wait for VMs to come up. 
- Run just the "direct_build_tasks.yml" against the up VMs from the Ansible usage below. The rest won't work. 
- publish the blueprint (saves the images)
- Kill the instances through approved processes when done 




# Ansible Usage:
buildtype can either be "sso" to build an openshift+rhsso+saml+oidc instance or "ipa" to build an IDM instance. You will run each of the commands below once for each type you want. 
 
- Build the image: ansible-playbook build_template.yml --extra-vars "buildtype=ipa"
- Deploy several copies: ansible-playbook spawn_instances.yml --extra-vars "buildtype=ipa"
- Be sure to save the /tmp/ips IP list if you will be using them.  
- Remove them all when done: ansible-playbook kill_instances.yml --extra-vars "buildtype=ipa"
- Testing specific parts of the build_template
	- whole section
	     - ansible-playbook build_template.yml --tags=provision --extra-vars "buildtype=ipa"
	     - ansible-playbook build_template.yml --tags=inventory,install --extra-vars "buildtype=ipa"
	     - ansible-playbook build_template.yml --tags=save --extra-vars "buildtype=ipa"
     - tag from a section
	     - ansible-playbook build_template.yml --tags=sshkey --extra-vars "buildtype=ipa"
	     - ansible-playbook build_template.yml --tags=inventory,ipaversion --extra-vars "buildtype=ipa"
	     - ansible-playbook build_template.yml --tags=templateami --extra-vars "buildtype=ipa" 
- Testing specific section of spawn, kill. 
     - ansible-playbook spawn_instances.yml --extra-vars "buildtype=ipa" --tags=classinventory,ipfile
     - ansible-playbook -i inventory.build kill_instances.yml --extra-vars "buildtype=ipa" --tags=classinventory
- Testing specific instance configuration steps: 
     - copy IP from cloud vendor into inventory.build
     - ansible-playbook -i inventory.build direct_build_tasks.yml --extra-vars "buildtype=ipa" --tags=ipaversion
- Last minute stuff. No logic here on buildtype. Handle that how you see fit:  
     - copy IPs from /tmp/ips into inventory.oneoff
     - ansible-playbook -i inventory.oneoff oneoff.yml
     - ansible-playbook -i inventory.oneoff oneoff.yml --tags=oneoff-test
 

