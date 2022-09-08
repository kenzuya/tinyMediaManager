#!/bin/sh


mkdir windows_installer
cd windows_installer

unzip ../target/tinyMediaManager*-windows-*.zip
cd tinyMediaManager
cp ../../AppBundler/installer.iss .

ls -l 
ls -l ../target/

VERSION=$(grep 'version' version | cut -d'=' -f2)
TIMESTAMP=$(grep 'timestamp' version | cut -d'=' -f2)
REALVERSION=$(echo $VERSION | sed "s/SNAPSHOT/$TIMESTAMP/g")

iscc installer.iss "/DMyAppVersion=$REALVERSION"

cp Output/tinyMediaManagerSetup.exe ../../target/
cd ..
cd ..
rm -rf windows_installer
