#!/bin/sh

VER=""
if [ ! -z "$1" ] then
    VER="/DMyAppVersion=$1"
fi

mkdir windows_installer
cd windows_installer
unzip ../target/tinyMediaManager*-windows-*.zip
cp ../AppBundler/installer.iss .
iscc installer.iss $VER
cp Output/tinyMediaManagerSetup.exe ../dist/
cd ..
rm -rf windows_installer
