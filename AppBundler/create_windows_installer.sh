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
echo -n "${CODE_SIGN_CERT}" | base64 -d > "code-sign-cert.p12"
shasum code-sign-cert.p12
ls -Flah code-sign-cert.p12
osslsigncode sign -pkcs12 "code-sign-cert.p12" -pass "${CODE_SIGN_PASS}" -n "tinyMediaManager" -i https://www.tinymediamanager.org/ -in Output/tinyMediaManagerSetup.exe -out ../dist/tinyMediaManagerSetup.exe

# cleanup
cd ..
rm -rf windows_installer
