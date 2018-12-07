#!/bin/sh

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
     -Ddfs.seeds=dfslb://127.0.0.1:10000 \
     -Ddfs.file.count=1 \
     com.jingoal.dfsclient.test.Stress > stress.log 2>&1 &
