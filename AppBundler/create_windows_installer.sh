#!/bin/sh

# prepare
mkdir windows_installer
cd windows_installer
unzip ../dist/tmm_*_windows*.zip
cp ../AppBundler/installer.iss .
cp ../AppBundler/tmm.ico .

# build
iscc installer.iss

# sign
echo -n "$1" | base64 -d > "code-sign-cert.p12"
osslsigncode sign -pkcs12 "code-sign-cert.p12" -pass "$2" -n "tinyMediaManager" -i https://www.tinymediamanager.org/ -in Output/tinyMediaManagerSetup.exe -out ../dist/tinyMediaManagerSetup.exe

# cleanup
cd ..
rm -rf windows_installer
