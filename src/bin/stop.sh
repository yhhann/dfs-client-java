#!/bin/sh

APP_MAINCLASS=com.jingoal.dfsclient.test.DfsLoader
checkpid() {
    javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAINCLASS`

    if [ -n "$javaps" ]; then
        psid=`echo $javaps | awk '{print $1}'`
    else
        psid=0
    fi
}

stop(){
    if [ $psid -ne 0 ]; then
        echo -n "Stopping $APP_MAINCLASS(pid=$psid) "
        kill -TERM $psid >/dev/null 2>&1
        while [ -x /proc/${psid} ]; do
            echo -n "."
            sleep 1s
        done
        echo "[OK]"
    else
        echo "================================"
        echo "warn: $APP_MAINCLASS is not running"
        echo "================================"
    fi
}

checkpid
stop