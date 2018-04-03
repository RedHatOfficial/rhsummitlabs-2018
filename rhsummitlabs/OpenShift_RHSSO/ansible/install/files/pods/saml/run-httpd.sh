#!/usr/bin/bash
rm -rf /run/httpd/* /tmp/httpd*
sleep 3
cp /etc/config/metadata.xml /etc/httpd/conf/metadata.xml
chmod 644 /etc/httpd/conf/metadata.xml
exec /usr/sbin/apachectl -DFOREGROUND
