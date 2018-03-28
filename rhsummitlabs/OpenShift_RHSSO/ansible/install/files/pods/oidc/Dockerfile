FROM registry.access.redhat.com/jboss-eap-7/eap71-openshift:1.2-7
MAINTAINER Dustin Minnich <dminnich@redhat.com>
USER root
RUN yum clean all && \
    yum -y update && \
    yum install -y net-tools bind-utils vim  lsof screen nmap-ncat nmap openssl wget curl rsync openssh-clients krb5-workstation openldap-clients bzip2 sos less iputils traceroute tcpdump telnet mtr strace unzip xz mysql git wireshark setroubleshoot info man-db mlocate findutils grep gawk pcre yum-utils readline which diffutils sudo iptables tar nss cronie net-tools bridge-utils gnupg2 ncurses nano sed rsyslog python file ethtool iperf iproute acl coreutils gzip logrotate tmpwatch procps-ng bc dmidecode emacs hdparm parted lvm2 make ntpdate patch policycoreutils util-linux expect python-docker-py && \    
    yum clean all 
ADD standalone-iam.xml oidc.jks /opt/eap/standalone/configuration/
ADD sample.war oidc-app.war /opt/eap/standalone/deployments/
RUN chown jboss:jboss -R /opt/eap/standalone/configuration/ && \
    chown jboss:jboss -R /opt/eap/standalone/deployments/
ADD run-jboss.sh /run-jboss.sh
RUN chmod -v +x /run-jboss.sh
CMD ["/run-jboss.sh"]
EXPOSE 8080 8443
USER 185
