#!/bin/sh
baseDir=`dirname $0`/../../
remoteDir=/root/proj/java/
proj=CleanBT
mod=$1

rsync -vlcr $baseDir/$proj  root@aliyun:$remoteDir --exclude-from "$baseDir/$proj/sh/exclude.list" --progress
ssh -o StrictHostKeyChecking=no -tt -p 22 root@aliyun "cd $remoteDir/$proj/$mod &&mvn clean &&mvn package"
