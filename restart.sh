echo "=> restart prq_cdr Agent..."
while :
do
pid=`/bin/ps -ef | grep prq_cdr/bin | grep java | grep -v grep | awk '{print $2}'`
if test "$pid" ; then
    kill -9  $pid
    echo $pid "is killed"
    echo "=> Starting prq_cdr Agent..."
    nohup /home/t3point/prq_cdr/startw.sh
else
    echo "=> Shutted Down prq_cdr Agent..."
    echo "=> Start prq_cdr Agent..."
    nohup /home/t3point/prq_cdr/startw.sh
    break
fi
