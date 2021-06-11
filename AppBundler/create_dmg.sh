#!/bin/sh

(
mkdir macos_dmg
cd macos_dmg
unzip ../dist/tmm_*_mac.zip
OUTSIZE=$(du -s tinyMediaManager.app | cut -f1)
dd if=/dev/zero of=temp.dmg bs=1124 count="${OUTSIZE}"
mkfs.hfsplus -v "tinyMediaManager" temp.dmg
hfsplus temp.dmg addall tinyMediaManager.app tinyMediaManager.app
dmg dmg temp.dmg tinyMediaManager.dmg
cp tinyMediaManager.dmg ../dist/tinyMediaManager.dmg
)
