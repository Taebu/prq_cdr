# prq_cdr
prq_cdr을 종전 crontab -e 방식의 
curl -v http://prq.co.kr/prq/cronta/view를
자바 프로그래밍 방식으로 변경 합니다.
문서 작성일 : 2016-12-28 (수) 14:59:34 
문서 관리자 : 문태부 

## Java 수정 후 구동 되게 만들기
- 해당 경로 이동
> $cd /home/t3point/prq_cdr

- 구동 확인 숫자가 나오면 멈추고 빌드 해야 한다.
> $sh ./status.sh

- 멈추는 명령 
> $sh ./stop.sh

- java to class
> $sh ./make.sh

- prq_cdr 재구동
> $sh ./startw.sh

- prq_cdr 재구동 확인
> $sh ./status.sh


