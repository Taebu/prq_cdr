ps -ef|grep prq_cdr/bin|grep java|grep prq_cdr|awk 'BEGIN {FS=" "} {print $2}'

