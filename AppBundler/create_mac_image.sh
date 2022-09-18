#!/bin/sh

###############################
# x64
###############################
mkdir macos_dmg_x64
cd macos_dmg_x64
unzip -X ../target/tinyMediaManager-*macos-x86_64.zip -d tinyMediaManager
VERSION=$(grep 'human.version' tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/version | cut -d'=' -f2)

touch tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/.userdir

# sign
codesign --force --options runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app

# prepare dmg
cp ../AppBundler/macos/DS_Store tinyMediaManager/.DS_Store
../AppBundler/macos/bin/create-dmg \
--background ../AppBundler/macos/background.png \
--volname "tinyMediaManager" \
--window-pos 200 120 \
--window-size 660 400 \
--icon-size 100 \
--icon "tinyMediaManager.app" 160 180 \
--hide-extension "tinyMediaManager.app" \
--app-drop-link 500 175 \
--skip-jenkins \
--format UDBZ \
tinyMediaManager.dmg \
tinyMediaManager

# sign dmg
codesign --force --options runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager.dmg

# notarize dmg
#xcrun notarytool submit tinyMediaManager.dmg --keychain-profile "tmm"

# copy to dist
cp tinyMediaManager.dmg ../dist/tinyMediaManager-$VERSION-macos-x86_64.dmg

# cleanup
cd ..
rm -rf macos_dmg_x64


###############################
# arm64
###############################
mkdir macos_dmg_aarch64
cd macos_dmg_aarch64
unzip -X ../target/tinyMediaManager-*macos-aarch64.zip -d tinyMediaManager
VERSION=$(grep 'human.version' tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/version | cut -d'=' -f2)

touch tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/.userdir

# sign
codesign --force --options runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager/tinyMediaManager.app

# prepare dmg
cp ../AppBundler/macos/DS_Store tinyMediaManager/.DS_Store
../AppBundler/macos/bin/create-dmg \
--background ../AppBundler/macos/background.png \
--volname "tinyMediaManager" \
--window-pos 200 120 \
--window-size 660 400 \
--icon-size 100 \
--icon "tinyMediaManager.app" 160 180 \
--hide-extension "tinyMediaManager.app" \
--app-drop-link 500 175 \
--skip-jenkins \
--format ULMO \
tinyMediaManager.dmg \
tinyMediaManager

# sign dmg
codesign --force --options runtime --deep --timestamp --sign "${MAC_SIGN_CERT}" tinyMediaManager.dmg

# notarize dmg
#xcrun notarytool submit tinyMediaManager.dmg --keychain-profile "tmm"

# copy to dist
cp tinyMediaManager.dmg ../dist/tinyMediaManager-$VERSION-macos-aarch64.dmg

# cleanup
cd ..
rm -rf macos_dmg_aarch64