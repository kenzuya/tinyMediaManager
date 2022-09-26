#!/bin/sh


mkdir windows_installer
cd windows_installer

unzip ../dist/tinyMediaManager*-windows-*.zip
cd tinyMediaManager
cp ../../AppBundler/installer.iss .

VERSION_T=$(grep 'human.version' version | cut -d'=' -f2)
VERSION_N=$(echo "$VERSION_T" | sed "s/[-].*//")
touch .userdir

iscc installer.iss "/DMyAppVersionText=$VERSION_T" "/DMyAppVersionNum=$VERSION_N"

cp Output/tinyMediaManager*.exe ../../dist/
cd ..
cd ..
rm -rf windows_installer
