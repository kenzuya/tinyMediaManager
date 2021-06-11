#!/bin/sh

mkdir windows_installer
cd windows_installer
unzip ../dist/tmm_*_win.zip
cp ../AppBundler/installer.iss .
cp ../AppBundler/tmm.ico .
iscc installer.iss
cp Output/tinyMediaManagerSetup.exe ../dist/
cd ..
rm -rf windows_installer
