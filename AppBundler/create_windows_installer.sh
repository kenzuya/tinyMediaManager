#!/bin/sh


mkdir windows_installer
cd windows_installer
unzip ../target/tinyMediaManager*-windows-*.zip
cp ../AppBundler/installer.iss .

VERSION=$(grep 'version' version | cut -d'=' -f2)
TIMESTAMP=$(grep 'timestamp' version | cut -d'=' -f2)
REALVERSION=$(echo $VERSION | sed "s/SNAPSHOT/$TIMESTAMP/g")

iscc installer.iss "$REALVERSION"

cp Output/tinyMediaManagerSetup.exe ../target/
cd ..
rm -rf windows_installer
