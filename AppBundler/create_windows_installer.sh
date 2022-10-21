#!/bin/sh

# prepare
mkdir windows_installer
cd windows_installer

unzip ../dist/tinyMediaManager*-windows-*.zip
cd tinyMediaManager
cp ../../AppBundler/installer.iss .

VERSION_T=$(grep 'human.version' version | cut -d'=' -f2)
VERSION_N=$(echo "$VERSION_T" | sed "s/[-].*//")
touch .userdir

# build
iscc installer.iss "/DMyAppVersionText=$VERSION_T" "/DMyAppVersionNum=$VERSION_N"

# sign
echo -n "$1" | base64 -d > "code-sign-cert.p12"
osslsigncode sign -pkcs12 "code-sign-cert.p12" -pass "$2" -n "tinyMediaManager" -i https://www.tinymediamanager.org/ -in Output/tinyMediaManager-$VERSION_T-Setup.exe -out ../../dist/tinyMediaManager-$VERSION_T-Setup.exe

# cleanup
cd ..
cd ..
rm -rf windows_installer
