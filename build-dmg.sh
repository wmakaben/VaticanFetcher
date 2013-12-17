#!/bin/sh

# This script creates a disk image for Mac OS X on Linux.
#
# Requirements:
# - mkfs.hfsplus must be installed
# - the script build.py must be run first
# - do not run this as root, see bug #412
#
# Output:
# - build/VaticanFetcher-{version-number}.dmg

user=$(whoami)
if [ $user = "root" ]
then
	echo "Do not run this script as root."
	exit 0
fi

version=`cat current-version.txt`

du_output=`du -sk build/VaticanFetcher.app 2>&1`
dir_size=`echo $du_output | cut -f1 -d" "`
dir_size=`expr $dir_size + 1000`
dmg_path=build/VaticanFetcher-$version.dmg

rm -f $dmg_path
dd if=/dev/zero of=$dmg_path bs=1024 count=$dir_size
mkfs.hfsplus -v "VaticanFetcher" $dmg_path

sudo mkdir /mnt/tmp-vaticanfetcher
sudo mount -o loop $dmg_path /mnt/tmp-vaticanfetcher

sudo cp -r build/VaticanFetcher.app /mnt/tmp-vaticanfetcher
sudo chown -R $user:$user /mnt/tmp-vaticanfetcher

sudo umount /mnt/tmp-vaticanfetcher
sudo rm -rf /mnt/tmp-vaticanfetcher
