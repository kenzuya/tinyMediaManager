#!/bin/sh


mkdir windows_installer
cd windows_installer

unzip ../dist/tinyMediaManager*-windows-*.zip
cd tinyMediaManager
cp ../../AppBundler/installer.iss .

VERSION=$(grep 'human.version' version | cut -d'=' -f2)
touch .userdir

iscc installer.iss "/DMyAppVersion=$VERSION"

cp Output/tinyMediaManager*.exe ../../dist/
cd ..
cd ..
rm -rf windows_installer
