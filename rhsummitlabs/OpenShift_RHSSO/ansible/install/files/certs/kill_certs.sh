#!/bin/bash
find . -mindepth 1 ! -name "openssl.cnf" ! -name "*.sh" -exec rm -Rf {} \;
