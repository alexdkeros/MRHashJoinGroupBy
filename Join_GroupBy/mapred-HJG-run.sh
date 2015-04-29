#!/bin/bash

reducers="2 32 64"
joinAttrs="salary"
groupAttrs="salary,R.id,R.rank,R.fname,R.sname,R.married,S.married,S.sname,S.rank,S.fname,S.id,S.foobar"

for r in $reducers
do

for jA in $joinAttrs
do

for gA in $groupAttrs
do

echo "$r $jA $gA" >> ~/workspace/timings.txt

/usr/bin/time -a -o ~/workspace/timings.txt -p /usr/local/hadoop/bin/hadoop jar ~/workspace/MRHashJoinGroup.jar MRHashJoinGroup /user/ak/HJG/R.txt /user/ak/HJG/S.txt $jA $gA $r

/usr/local/hadoop/bin/hadoop fs -rmr outHJ

/usr/local/hadoop/bin/hadoop fs -rmr outGB

/usr/local/hadoop/bin/hadoop fs -rmr outHJG

done
done
done
