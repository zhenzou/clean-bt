#!/bin/sh
baseDir=/media/Media/Projects/JavaFX
remoteDir=/root/proj/java/
proj=CleanBT
mod=$1

rsync -vlcr $baseDir/CleanBT  root@mos:$remoteDir --exclude-from "$baseDir/$proj/sh/exclude.list" --progress
ssh -o StrictHostKeyChecking=no -tt -p 22 root@mos "cd $remoteDir/$proj/$mod &&mvn package "
