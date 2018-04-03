FROM jboss/keycloak-adapter-wildfly

USER root

RUN yum clean all && \
    yum -y update && \
    yum install -y wget curl vim less && \
    yum clean all

