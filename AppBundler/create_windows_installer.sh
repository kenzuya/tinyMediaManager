#!/bin/sh

(
mkdir windows_installer
chmod 777 windows_installer
su - xclient sh <<"EOF"
cd windows_installer
unzip ../dist/tmm_*_win.zip
cp ../AppBundler/installer.iss .
cp ../AppBundler/tmm.ico .
iscc installer.iss
cp Output/tinyMediaManagerSetup.exe ../dist/
EOF
)
