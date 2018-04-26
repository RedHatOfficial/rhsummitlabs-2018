#!/usr/bin/env bash
# Simple script to check output from HTTP server
# Rhys Oxenham <roxenham@redhat.com>

if [ -z "$1" ]
then
	echo "ERROR: You must specify the floating IP (or LBaaS VIP) to use"
	exit
fi

echo -e "\nINFO: Using http://$1:80... (Ctrl-c to stop)\n\n"

while true; do
	date;
        curl -s http://$1;
        if [ $? -ne 0 ]; then
            echo "ERROR: Cannot communicate with server, or server is down."
        fi
	echo -e '\n';
	sleep 3;
done
