#!/bin/sh
if [ ! -n "$1" -o ! -n "$2" -o ! -n "$3" -o ! -n "$4" -o ! -n "$5" -o ! -n "$6" ] ;then
    echo "Please input five parameters, for example '10 100 1024 2048 dfslb://192.168.37.6:10000 4567'."
else
    echo "The parameter you input is $1 $2 $3 $4 $5 $6"
fi
CLSPTH=.:conf:$CLASSPATH
for file in lib/*.jar;
do
    CLSPTH=$CLSPTH:$file;
done

java -server -Xmx2G -Xms2G -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=500 \
     -XX:+UnlockExperimentalVMOptions \
     -Dio.netty.leakDetectionLevel=SIMPLE \
     -cp $CLSPTH \
     com.jingoal.dfsclient.performance.test.DFSClientPerfTest $1 $2 $3 $4 $5 $6 > performance.log 2>&1 &
