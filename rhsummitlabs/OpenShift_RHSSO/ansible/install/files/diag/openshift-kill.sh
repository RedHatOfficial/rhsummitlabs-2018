#!/bin/bash
oc cluster down
mount | grep openshift.local | awk '{ print $3 }' | while read line; do umount $line; done
docker ps -a | tail -n +2 | awk '{print $1}' | while read line; do docker rm $line; done
docker images -a | awk '{ print $1 ":" $2 }' | grep -v REPOSITORY | while read line; do docker rmi $line; done
docker images -a | awk '{ print $3 }' | grep -v IMAGE | while read line; do docker rmi $line; done
rm -Rf /opt/openshift_data/ /var/lib/origin/openshift.local.*

