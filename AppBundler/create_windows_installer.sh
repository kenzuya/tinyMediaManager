#!/bin/sh

# prepare
mkdir windows_installer
cd windows_installer
unzip ../dist/tmm_*_windows*.zip
cp ../AppBundler/installer.iss .
cp ../AppBundler/tmm.ico .

# build
iscc installer.iss

# sign
curl --silent "https://gitlab.com/gitlab-org/incubation-engineering/mobile-devops/load-secure-files/-/raw/main/installer" | bash
osslsigncode sign -pkcs12 "../.secure_files/code-sign-cert.p12" -pass "${CODE_SIGN_PASS}" -n "tinyMediaManager" -i https://www.tinymediamanager.org/ -in Output/tinyMediaManagerSetup.exe -out ../dist/tinyMediaManagerSetup.exe

# cleanup
cd ..
rm -rf windows_installer
