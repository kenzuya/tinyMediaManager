#!/bin/sh


mkdir windows_installer
cd windows_installer

unzip ../target/tinyMediaManager*-windows-*.zip
cd tinyMediaManager
cp ../../AppBundler/installer.iss .

VERSION=$(grep 'human.version' version | cut -d'=' -f2)
iscc installer.iss "/DMyAppVersion=$VERSION"

cp Output/tinyMediaManagerSetup*.exe ../../dist/
cd ..
cd ..
rm -rf windows_installer
