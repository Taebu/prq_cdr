ps -ef | grep prq_cdr/bin | grep java | grep -v grep | awk '{print $2}' | xargs kill -9
