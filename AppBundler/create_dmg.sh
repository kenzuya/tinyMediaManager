#!/bin/sh

mkdir macos_dmg
cd macos_dmg
unzip -X ../target/tinyMediaManager-*macos-x86_64.zip -d tinyMediaManager
OUTSIZE=$(du -s tinyMediaManager | cut -f1)
dd if=/dev/zero of=temp.dmg bs=1124 count="${OUTSIZE}"
mkfs.hfsplus -v "tinyMediaManager" temp.dmg
hfsplus temp.dmg addall tinyMediaManager
hfsplus temp.dmg addall ../AppBundler/macos
hfsplus temp.dmg symlink "Applications" /Applications
hfsplus temp.dmg chmod 755 tinyMediaManager.app/Contents/MacOS/tinyMediaManager
hfsplus temp.dmg chmod 755 tinyMediaManager.app/Contents/MacOS/JavaApplicationStub
dmg dmg temp.dmg tinyMediaManager.dmg
cp tinyMediaManager.dmg ../target/tinyMediaManager.dmg
cd ..
rm -rf macos_dmg
