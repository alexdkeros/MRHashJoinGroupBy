#!/bin/bash

reducers=(60 32 2);
joinAttrs=(name id);
groupAttrs=(name,in1.id,in2.id id,in1.name,in2.name);
hadoopPath="/usr/local/hadoop"
userPath="/home/ak/workspace"
resultsPath="/home/ak/workspace/timings.txt"
inPath0="/user/ak/HJG/in1.txt"
inPath1="/user/ak/HJG/in2.txt"

for r in ${reducers[@]}
do

	for (( i=0; i<2; i++ ))
	do

		echo "$r ${joinAttrs[$i]} ${groupAttrs[$i]}" >> $resultsPath

		/usr/bin/time -a -o $resultsPath -p $hadoopPath/bin/hadoop jar $userPath/MRHashJoinGroup.jar MRHashJoinGroup $inPath0 $inPath1 ${joinAttrs[$i]} ${groupAttrs[$i]} $r

		$hadoopPath/bin/hadoop fs -rmr outHJ

		$hadoopPath/bin/hadoop fs -rmr outGB

		$hadoopPath/bin/hadoop fs -rmr outHJG

	done
done

