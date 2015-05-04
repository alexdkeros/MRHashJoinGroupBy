#!/bin/bash

reducers=(60 32 2);
joinAttrs=(salary foobar);
groupAttrs=(salary,R.id,R.rank,R.fname,R.sname,R.married,R.foobar,S.married,S.sname,S.rank,S.fname,S.id,S.foobar foobar,R.id,R.rank,R.salary,R.fname,R.sname,R.married,S.married,S.sname,S.rank,S.fname,S.salary,S.id);
hadoopPath="usr/local/hadoop103"
userPath="/home/advdb05"
resultsPath="/home/advdb05/timings.txt"
inPath0="/user/vagvaz/advdb/R.txt"
inPath1="/user/vagvaz/advdb/S.txt"

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
