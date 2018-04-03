FROM registry.access.redhat.com/rhel:7.4-164
MAINTAINER Dustin Minnich <dminnich@redhat.com>
RUN yum clean all && \
    yum -y update && \
    yum install -y net-tools bind-utils vim  lsof screen nmap-ncat nmap openssl wget curl rsync openssh-clients krb5-workstation openldap-clients bzip2 sos less iputils traceroute tcpdump telnet mtr strace unzip xz mysql git wireshark setroubleshoot info man-db mlocate findutils grep gawk pcre yum-utils readline which diffutils sudo iptables tar nss cronie net-tools bridge-utils gnupg2 ncurses nano sed rsyslog python file ethtool iperf iproute acl coreutils gzip logrotate tmpwatch procps-ng bc  dmidecode emacs hdparm parted lvm2 make ntpdate patch policycoreutils util-linux expect python-docker-py \    
    httpd mod_ssl mod_auth_mellon php && \
    yum clean all 
# Port change and log location changes
RUN sed -i 's/Listen 80/Listen 8080/g' /etc/httpd/conf/httpd.conf && \
    sed -i 's/443/8443/g' /etc/httpd/conf.d/ssl.conf && \
    sed -i 's^/etc/pki/tls/certs/localhost.crt^/etc/httpd/conf/saml-demo.paas.local.crt^g' /etc/httpd/conf.d/ssl.conf && \
    sed -i 's^/etc/pki/tls/private/localhost.key^/etc/httpd/conf/saml-demo.paas.local.key^g' /etc/httpd/conf.d/ssl.conf && \
    sed -i 's/.*ServerName.*/ServerName saml-demo.local:443/g' /etc/httpd/conf.d/ssl.conf && \
    mkdir -p /var/www/html/secret 
# Permissions change so apache can run as apache
RUN chown apache:apache -R /var/log/httpd && \
    chown apache:apache -R /etc/httpd/conf && \
    chown apache:apache -R /run
ADD index.php /var/www/html/secret/index.php
ADD mellon.conf /etc/httpd/conf.d/
ADD mellon-signing.crt mellon-signing.key saml-demo.paas.local.crt saml-demo.paas.local.key /etc/httpd/conf/
ADD run-httpd.sh /run-httpd.sh
RUN chmod -v +x /run-httpd.sh
CMD ["/run-httpd.sh"]
EXPOSE 8080 8443
USER 48
