#!/bin/sh


mkdir windows_installer
cd windows_installer
unzip ../target/tinyMediaManager*-windows-*.zip tinyMediaManager/*
cp ../AppBundler/installer.iss .

ls -l windows_installer

VERSION=$(grep 'version' tinyMediaManager/version | cut -d'=' -f2)
TIMESTAMP=$(grep 'timestamp' tinyMediaManager/version | cut -d'=' -f2)
REALVERSION=$(echo $VERSION | sed "s/SNAPSHOT/$TIMESTAMP/g")

iscc installer.iss "/DMyAppVersion=$REALVERSION"

cp Output/tinyMediaManagerSetup.exe ../target/
cd ..
rm -rf windows_installer
