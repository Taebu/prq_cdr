echo "=> Shutted Down prq_cdr Agent..."
while :
do
pid=`/bin/ps -ef | grep prq_cdr/bin | grep java | grep -v grep | awk '{print $2}'`
if test "$pid" ; then
    kill -9  $pid
    echo $pid "is killed"
else
    echo "=> Shutted Down prq_cdr Agent..."
    break
fi
done