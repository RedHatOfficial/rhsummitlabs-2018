#!/bin/bash
if [ "${1}" = "server" ]; then
openssl ca -config openssl.cnf -extensions v3_server -days 3650 -notext -md sha256 -in csr/${2}.csr -out certs/${2}.crt -keyfile myca.key -cert myca.crt -batch
elif [ "${1}" = "ca" ]; then
openssl ca -config openssl.cnf -extensions v3_ca -days 3650 -notext -md sha256 -in csr/${2}.csr -out certs/${2}.crt -keyfile myca.key -cert myca.crt -batch
else
echo "usage: server|ca fqdn_file_in_csr_dir"
fi
