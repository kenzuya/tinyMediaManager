#!/bin/sh

mkdir macos_dmg
cd macos_dmg
unzip -X ../dist/tinyMediaManager-*macos-x86_64.zip -d tinyMediaManager
VERSION=$(grep 'human.version' tinyMediaManager/tinyMediaManager.app/Contents/Resources/Java/version | cut -d'=' -f2)
OUTSIZE=$(du -s tinyMediaManager | cut -f1)
dd if=/dev/zero of=temp.dmg bs=1124 count="${OUTSIZE}"
mkfs.hfsplus -v "tinyMediaManager" temp.dmg
hfsplus temp.dmg addall tinyMediaManager
hfsplus temp.dmg addall ../AppBundler/macos
hfsplus temp.dmg symlink "Applications" /Applications
hfsplus temp.dmg chmod 755 tinyMediaManager.app/Contents/MacOS/tinyMediaManager
hfsplus temp.dmg chmod 755 tinyMediaManager.app/Contents/MacOS/JavaApplicationStub
dmg dmg temp.dmg tinyMediaManager.dmg
cp tinyMediaManager.dmg ../dist/tinyMediaManager-$VERSION-macos-x86_64.dmg
cd ..
rm -rf macos_dmg
