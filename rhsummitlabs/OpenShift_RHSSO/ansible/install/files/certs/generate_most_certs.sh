#!/bin/bash
set -ex
#ca
touch index.txt
echo 1000 > serial
mkdir certs private csr
openssl req -newkey rsa:4096 -new -days 3650 -nodes -sha256 -keyout myca.key -extensions v3_ca -subj "/C=US/ST=North Carolina/O=Red Hat Summit/OU=Lab Demos/CN=MyCA" -out myca.csr -extensions v3_ca -config openssl.cnf
chmod 600 myca.key
openssl ca -config openssl.cnf -extensions v3_ca -days 3650 -notext -md sha256 -in myca.csr -out myca.crt -keyfile myca.key -batch -selfsign

#pod certs that don't need sans.  mellon is used for signing.  novnc we won't know the hostname ahead of time so will alert.  
for x in db-demo.paas.local saml-demo.paas.local oidc-demo.paas.local mellon-signing novnc openshift.local
do
   openssl req -newkey rsa:4096 -keyout private/${x}.key -new -sha256 -nodes -days 3650 -subj "/C=US/ST=Any/O=Any/CN=${x}" -reqexts SAN -config <(cat /etc/pki/tls/openssl.cnf <(printf "[SAN]\nsubjectAltName=DNS:${x}")) -out csr/${x}.csr
   chmod 600 private/${x}.key
   openssl ca -config openssl.cnf -extensions v3_server -days 3650 -notext -md sha256 -in csr/${x}.csr -out certs/${x}.crt -keyfile myca.key -cert myca.crt -batch
done


#sso could have several hostnames lets set the sans
p="sso-demo.paas.local"
s="DNS:sso-demo.paas.local,DNS:secure-sso-demo.paas.local,DNS:secure-tmpsso-demo.paas.local,DNS:tmpsso-demo.paas.local"
openssl req -newkey rsa:4096 -keyout private/${p}.key -new -sha256 -nodes -days 3650 -subj "/C=US/ST=Any/O=Any/CN=${p}" -reqexts SAN -config <(cat /etc/pki/tls/openssl.cnf <(printf "[SAN]\nsubjectAltName=${s}")) -out csr/${p}.csr
chmod 600 private/${p}.key
openssl ca -config openssl.cnf -extensions v3_server -days 3650 -notext -md sha256 -in csr/${p}.csr -out certs/${p}.crt -keyfile myca.key -cert myca.crt -batch
#convert this to a pkcs
cat myca.crt certs/${p}.crt > certs/${p}-with-chain.crt
openssl pkcs12 -export -inkey private/${p}.key -in certs/${p}-with-chain.crt -out certs/${p}.pkcs12 -name "sso-https-key" -password pass:test1234
keytool -importkeystore -deststorepass 'test1234' -destkeypass 'test1234' -destkeystore certs/${p}.jks -srckeystore certs/${p}.pkcs12 -srcstoretype PKCS12 -srcstorepass 'test1234' -srcalias 'sso-https-key' -destalias 'sso-https-key' -noprompt
keytool -import -file myca.crt -alias myca -keystore certs/${p}.jks -storepass 'test1234' -keypass 'test1234' -noprompt
keytool -import -file myca.crt -alias myca -keystore certs/truststore.jks -storepass 'test1234' -keypass 'test1234' -noprompt
cp certs/${p}.jks certs/sso-https.jks
cp certs/truststore.jks certs/sso-truststore.jks 
#jgroups
keytool -genseckey -alias jgroups -storetype JCEKS -keystore certs/jgroups.jceks -storepass 'test1234' -keypass 'test1234' -noprompt

#oidc needs to be a jks as well
p="oidc-demo.paas.local"
cat myca.crt certs/${p}.crt > certs/${p}-with-chain.crt
openssl pkcs12 -export -inkey private/${p}.key -in certs/${p}-with-chain.crt -out certs/${p}.pkcs12 -name "oidc" -password pass:test1234
keytool -importkeystore -deststorepass 'test1234' -destkeypass 'test1234' -destkeystore certs/${p}.jks -srckeystore certs/${p}.pkcs12 -srcstoretype PKCS12 -srcstorepass 'test1234' -srcalias 'oidc' -destalias 'oidc' -noprompt
keytool -import -file myca.crt -alias myca -keystore certs/${p}.jks -storepass 'test1234' -keypass 'test1234' -noprompt
