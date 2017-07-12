package kr.co.prq.prq_cdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

//import com.nostech.safen.SafeNo;

/**
 * prq_cdr 테이블 관련 객체
 * 2017-05-10 (수) 12:30
 * 
 * @author Taebu
 *  
 */
public class Prq_cmd_queue {
	
	static boolean is_release = true;
	/**
	 * prq_cdr 테이블의 데이터를 처리하기 위한 주요한 처리를 수행한다.
	 */
	public static void doMainProcess() {
		Connection con = DBConn.getConnection();
		
		String cd_date="",cd_id="",cd_port="",cd_callerid="",cd_calledid="",img_url="",result_msg="",mms_title="",cd_tel="",cd_hp="",message="",cd_name="",chk_limit_date="";				
		/* 상점 정보*/
		String st_no="",st_thumb_paper="",st_bottom_msg="",st_modoo_url="",black_list="";
		/* happycall */
		String hc_status="";
		int cd_state=0,cd_day_cnt=0,cd_day_limit=0,cd_device_day_cnt=0,day_cnt=0,mno_device_daily=0,mn_mms_limit=0,mn_dup_limit=0,chk_cd_date=0,daily_mms_cnt=0,mm_daily_cnt=0,my_device_cnt=0;
		String last_cdr="first_sent";		
		String gc_ipaddr="123.142.52.91";
		String is_blogon="off";		
		String[] mno_limit = new String[2];
		boolean chk_mms = true,is_test = true;		
		boolean is_hp = false,is_shophp=false,is_hpcall = false,is_set_limit = false;
		Long startTime=0L,endTime=0L,totalTime=0L;
	
		/* 상점 정보 */
		String[] store_info			= new String[10];
		/* happycall_info */
		String[] happycall_info	= new String[20];
		/* 콜로그 데이터 */
		String[] cdr_info	= new String[7];
		/* config 데이터 */
		String[] config	= new String[6];
		/* gcm_log 데이터 */
		String[] gcm_log	= new String[8];
		/* gcm_log 데이터 */
		String[] happy_log	= new String[3];
		
		if (con != null) {
			MyDataObject dao = new MyDataObject();
			StringBuilder sb = new StringBuilder();
			/* 1. `prq_cdr`이 cd_state=0 인 결과를 15개 불러 온다. */
			sb.append("select * from prq.prq_cdr  ");
			sb.append("WHERE cd_state=0 ");
			sb.append(" order by cd_date ");
			sb.append("limit 15;");
			
			try {
				startTime = System.currentTimeMillis();
				dao.openPstmt(sb.toString());
				dao.setRs(dao.pstmt().executeQuery());
				/* 2. 블랙 리스트 가져오기 */
				black_list=get_black();
				
				while(dao.rs().next()) 
				{
					PRQ_CDR.heart_beat = 1;
					String hist_table = DBConn.isExistTableYYYYMM();
					/* 소비자에게 걸려온 전화가 핸드폰인가? */
					is_hp=checkPattern("phone",dao.rs().getString("cd_callerid"));
					/*	String cd_callerid, 수신인, 매장에 전화건 손님 */
					cd_callerid=chkValue(dao.rs().getString("cd_callerid"));
					// cd_callerid="0166551377";
					/*	String cd_date 날짜정보, */
					cd_date=chkValue(dao.rs().getString("cd_date"));
					cd_date=chgDatetime(cd_date);
					System.out.print(cd_date+" : ");
					/*	String cd_id 아이디  */
					cd_id=chkValue(dao.rs().getString("cd_id"));
					/*	String cd_port 콜로그 포트 */
					cd_port=chkValue(dao.rs().getString("cd_port"));
					/*	String cd_calledid, 발신 */
					cd_calledid=chkValue(dao.rs().getString("cd_calledid"));
					/*	String cd_name, 매장 이름 */
					cd_name=chkValue(dao.rs().getString("cd_name"));
					/*	String cd_tel, 매장 일반 번호 */
					cd_tel=chkValue(dao.rs().getString("cd_tel"));
					/*	String cd_hp, 매장 사장님 발신 핸드폰 */
					cd_hp=chkValue(dao.rs().getString("cd_hp"));
					/*	Int cd_state 상태코드, */
					cd_state=dao.rs().getInt("cd_state");
					/*	Int cd_day_cnt 일별전송갯수, */
					cd_day_cnt=dao.rs().getInt("cd_day_cnt");
					/*	Int cd_day_limit 일별제한 갯수. */
					cd_day_limit=dao.rs().getInt("cd_day_limit");
					/*	Int cd_device_day_cnt 매장사장님 핸드폰에서 수동혹은 타 앱으로 전송한 갯수 */
					cd_device_day_cnt=dao.rs().getInt("cd_device_day_cnt");
			
					/* 3. 일반 번호 입니까? */
					if(!is_hp){
						result_msg= "일반번호";
						/* 발송 처리 핸드폰 일반 번호 */
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="2";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;
					}
					/* 4. 수신거부 번호 입니까?
					* 블랙리스트 번호 테스트 
					* 수신거부 목록에 있는 경우 while을 skip 하고 다음 로그를 조회 한다.
					* */
					if(black_list.contains(cd_callerid))
					{
						result_msg="수신거부";
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="3";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;						
					}
					
					/* 5. 상점 정보 불러오기  
					* - 기기 CID인 경우( * KT_CID 아닌 경우)
					* - 이메일과 포트 번호로 상점 정보 조회
					********************************************************************************/
					if(cd_port.equals("0")){
						/* kt CID 상점 정보 */
						config[0]=cd_id;
						config[1]=cd_calledid;
						store_info=get_store_kt(config);
						st_no=store_info[0];
						cd_hp=store_info[4];
						cd_tel=store_info[3];
						
						cdr_info[0]=store_info[1];
						cdr_info[1]=store_info[3];
						cdr_info[2]=store_info[4];
						cdr_info[3]=cd_date;
					
						/* kt cdr 정보 갱신 */
						set_cdr_kt(cdr_info);	
					}else if(!cd_port.equals("0")){
						/* 일반 CID 상점 정보 */
						config[0]=cd_id;
						config[1]=cd_port;
						store_info=get_store(config);
						st_no=store_info[0];
					}
					
					is_shophp=checkPattern("phone",store_info[4]);
					
					/*2017-06-30 (금) 18:01:07 
					 * 상점 사장 번호가 핸드폰 번호가 아님 */
					if(!is_shophp)
					{
						//
						result_msg="업체 사장번호가 핸드폰이 아님";
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="6";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;					
					}
					
					/* 6.  상점 정보 없음. */
					if(store_info[1].equals("150"))
					{
						result_msg="업소누락";
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="5";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;
					}
					
					/* 7. 전송할 핸드폰 번호가 없거나 정보 부족 */
					if(cd_hp.equals(""))
					{
						result_msg="전송할 핸드폰 번호가 없거나 정보 부족 ";						
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="6";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;
					}
					
					
					/*******************************************************************************
					* 8. get_last_cdr 
					* - 마지막 바로 전 cdr 정보 조회 
					* - 지금 들어온 데이터는 당연 예외 처리 값을 비교한 값만을 참조하고,
					* - 처음 보내는 것은 first_send로 명명한다.
					* - first_send가 아니면 간격만큼 숫자를 반환한다. string이므로 사용시 int로 변환
					*******************************************************************************/
					last_cdr=get_last_cdr(cd_date,cd_tel,cd_hp,cd_callerid);
					
					/*******************************************************************************
					* 9. get_mno_limit
					* - 중복 발송일 수 조회 기본값은 0인데  
					* - 값이 mn_dup_limit 만약 3이라면, 
					* - 마지막 콜로그와 대조해 보아서 
					* - 3일 동안 보내지 않습니다.  
					* - NEW] mn_limit_
					* return array[0]
					* mno_limit[0] =	mn_mms_limit
					* mno_limit[1] = mn_dup_limit
					******************************************************************************/						
					mno_limit=get_mno_limit(cd_hp);

					/********************************************************************************
					* 10. int get_send_cnt
					* - 오늘 mms 200(정상) 발송 수 조회 
					********************************************************************************/
					day_cnt=get_send_cnt(cd_hp);
					
					/********************************************************************************
					* 11. array get_mms_daily
					* - mms_daily 정보 가져 오기 시스템에서 보낸 정보 아닌 
					*********  ***********************************************************************/
					mno_device_daily=get_mms_daily(cd_hp);
					mm_daily_cnt=mno_device_daily;
					
					/********************************************************************************
					* 12. void set_cdr
					* - cdr 정보 세팅
					********************************************************************************/
					mn_mms_limit=Integer.parseInt(mno_limit[0]);
					mn_dup_limit=Integer.parseInt(mno_limit[1]);
					
					/* 모바일에서 건 조건(mn_mms_limit)이 0이면 무조건 전송 */
					cdr_info[0]=mno_limit[0];
					cdr_info[1]=Integer.toString(mm_daily_cnt);
					cdr_info[2]=Integer.toString(day_cnt);
					cdr_info[3]=cd_date;
					cdr_info[4]=cd_tel;
					cdr_info[5]=cd_hp;
					set_cdr(cdr_info);
					cd_day_cnt=day_cnt;
					
					/* 오늘 기기에서 보낸 총 갯수 = 기기에서 보낸 mms + prq에서 보낸 mms 갯수*/
					my_device_cnt=mm_daily_cnt+cd_day_cnt+cd_device_day_cnt; 
					is_set_limit=my_device_cnt>=mn_mms_limit;
					
					/* 8. 전송제한초과 입니까? */
					if(mn_mms_limit==0){
						
						
					}else if(is_set_limit){
						/*gcm 로그 발생*/
						result_msg="150건초과";
						cdr_info[0]=cd_date;
						cdr_info[1]=cd_id;
						cdr_info[2]=cd_port;
						cdr_info[3]=cd_callerid;
						cdr_info[4]="4";
						set_sendcdr(cdr_info);
						System.out.println(result_msg);
						continue;						
					}
					
					if(last_cdr.equals("first_sent")){
						chk_limit_date="처음 보냄";
					}else{
						chk_cd_date=Integer.parseInt(last_cdr);
						chk_limit_date=mn_dup_limit>chk_cd_date?"보내면 안됨":"보냄";
					}
					/*mms 발송 여부*/
					chk_mms=true;
					//message=chg_mms_msg(st_top_msg,st_mno,st_middle_msg,st_no,is_hp,st_bottom_msg,st_modoo_url,store_info,cd_callerid);
					message=chg_mms_msg(st_bottom_msg,st_modoo_url,store_info);
					//System.out.println(message);
					/********************************************************************************
					* 9. void set_gcm_log
					* - gcm 로그에 따라 prq DB에 gcm_log 발생
					*
					********************************************************************************/
					img_url="http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
					/* 일간 mms 발송건 초기값 */
					daily_mms_cnt=0;
					/* 일간 mms 발송건 디바이스 값 +  일간 mms 발송건 prq 값  */
					daily_mms_cnt=mm_daily_cnt+cd_day_cnt;;

					/********************************************************************************
					* 9-1. if($cd_date=="first_send"){...}
					* - 처음 보낼 때 안보내지던 버그 수정
					* - $chk_mms = true;
					*********************************************************************************/
					result_msg= last_cdr+"/"+mn_dup_limit+"일 발송";
					System.out.println(result_msg);
							
					if(last_cdr.equals("first_sent")){
						/*gcm 로그 발생*/
						result_msg= "처음 발송 / "+mn_dup_limit;
						chk_mms=true;
					/********************************************************************************
					* 9-2. void set_gcm_log
					* 중복 제한 보내면 안됨 
					* prq_gcm_log 중복제한 로그 발생
					********************************************************************************/
					}

					/********************************************************************************
					* 9-4. curl->simple_post('http://prq.co.kr/prq/set_gcm.php')
					* - 수신거부 중복, 150건 제한 혹은 설정한 일수 제한 아닌 경우만
					* - $chk_mms = true;
					*********************************************************************************/
					if(chk_mms&&is_hp&&is_release)
					{
						//set_gcmurl(message,st_no,mms_title,cd_callerid,cd_hp,st_thumb_paper,false);
						set_gcmurl(message,st_no,store_info[6],cd_callerid,cd_hp,store_info[5],false);
					}
	
					/* 발송 처리 핸드폰 일반 번호 */
					//Utils.getLogger().info(cd_date+" : cnt "+ my_device_cnt+" / limit "+mn_mms_limit+" cd_callerid : "+cd_callerid+" cd_hp : "+cd_hp);
					cdr_info[0]=cd_date;
					cdr_info[1]=cd_id;
					cdr_info[2]=cd_port;
					cdr_info[3]=cd_callerid;
					cdr_info[4]="1";
					set_sendcdr(cdr_info);
					System.out.println("전송");
					
					happy_log=get_happycall();
					/* [01086033821(hc_hp), 331(st_no), (hc_no)] */
					//store_info=get_storeno(happycall_info[1]);
					
					
					//is_set_limit=is_limit(store_info[4]);
					//hc_status=is_set_limit?"2":"1";
					
					//set_happycall_log(hc_status,happy_log[2]);
					//is_hpcall=!happy_log[0].equals("-1");
					
					
					
					//result_msg= my_device_cnt+"/"+mn_mms_limit+"건 제한";
					//System.out.println(result_msg);
					//if(get_blog_yn(st_no).equals("on")&&is_hp&&is_release)
					//	set_happycall(cd_callerid,st_no);
					//if(is_hpcall&&!is_set_limit)
					//{
					//	happycall_info[0]=happy_log[0];
					//	happycall_info[1]=happy_log[1];
					//	happycall_info[2]=Integer.toString(my_device_cnt);
					//	happycall_info[3]=Integer.toString(mn_mms_limit);
					//	set_happycall_mms(is_set_limit,is_hp,happycall_info);
					//	System.out.println("happycall 전송");
					//}/* if(is_hpcall){...}*/
				}/* while(dao.rs().next()){...} */	
			}catch (SQLException e){
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS001";
				e.printStackTrace();
			}catch (Exception e){
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS002";
				Utils.getLogger().warning(Utils.stack(e));
			}finally{
				dao.closePstmt();
		        endTime = System.currentTimeMillis();
		        /* 처리 완료 건 이동 */
		        set_hist();
		        // 시간 출력
		        System.out.println("##  소요시간(초.0f) : " + ( endTime - startTime )/1000.0f +"초");
			}
		}
	}

    /**
     * chkValue
	 *  데이터 유효성 null 체크에 대한 값을 "" 로 리턴한다.
     * @param str
     * @return String
     */
	public static String chkValue(String str)
	{
		String retVal="";

		try{
				retVal=str==null?"":str;
		}catch(NullPointerException e){
			
		}
		return retVal;
	}

	/**
	* get_last_cdr
	* @param String cd_date 
	* @param String cd_tel
	* @param String cd_hp
	* @param String cd_callerid
	* @return String
	*/
	private static String get_last_cdr(String cd_date, String cd_tel,String cd_hp,String cd_callerid) {
		String retVal = "first_sent";
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		MyDataObject dao2 = new MyDataObject();

		sb.append("SELECT  ");
		sb.append(" cd_date ");
		sb.append(" FROM ");
		sb.append(" prq_cdr_tmp ");
		sb.append("WHERE cd_tel=? ");
		sb.append("AND cd_hp=? ");
		sb.append("AND cd_callerid=? ");		
		sb.append("ORDER BY cd_date desc limit 1,1;");

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, cd_tel);
			dao.pstmt().setString(2, cd_hp);
			dao.pstmt().setString(3, cd_callerid);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				retVal = dao.rs().getString("cd_date");
				//맞는 데이터가 있다면 해당 내용 반환
				
				
				sb2.append("SELECT TIMESTAMPDIFF(DAY,?,?) as cd_date;");
				dao2.openPstmt(sb2.toString());
				dao2.pstmt().setString(1, retVal);
				dao2.pstmt().setString(2, cd_date);
				dao2.setRs(dao2.pstmt().executeQuery());
				
				if (dao2.rs().next()) {
					retVal = dao2.rs().getString("cd_date");
					/* 0 이상의 숫자로 return */ 
				}
								
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS003";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS004";
		}
		finally {
			dao.closePstmt();
			dao2.closePstmt();
		}

		return retVal;
	}


	/**
	* get_mno_limit(cd_hp)
	* @param String cd_hp
	* @return array
	*/
	private static String[] get_mno_limit(String cd_hp) {
		String[] s = new String[2]; 
		s[0]="150";
		s[1]="7";
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT  ");
		sb.append(" mn_mms_limit,  ");
		sb.append(" mn_dup_limit  ");
		sb.append(" FROM ");
		sb.append(" prq_mno ");
		sb.append("WHERE mn_hp=?");
		

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, cd_hp);
			
			dao.setRs (dao.pstmt().executeQuery());
			if (dao.rs().next()) 
			{
				s[0]=dao.rs().getString("mn_mms_limit");
				s[1]=dao.rs().getString("mn_dup_limit");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS005";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS006";
		}
		finally {
			dao.closePstmt();
		}

		return s;
	}

	/**
	 * get_send_cnt
	 * prq_mms_log 에서 핸드폰으로 정상 200 전송 된 결과만 가져오도록 한다.  
	 * @param mb_hp
	 * @return int
	 */
	private static int get_send_cnt(String mb_hp){
		int retVal = 0;
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		
//		sb.append("SELECT count(*) cnt FROM `prq_mms_log` ");
//		sb.append("where mm_sender=? ");
//		sb.append("and left(mm_result,3)='200' ");
//		sb.append("and date(now())=date(mm_datetime);");
//		
		sb.append("select ms_success cnt from prq_sf_log ");
		sb.append("where ms_hp=? ");
		sb.append("and ms_date=date(now());");
		/*
		sb.append("SELECT count(*) cnt ");
		sb.append("FROM `prq_gcm_log` ");
		sb.append("WHERE date(now())=date(gc_datetime) ");
		sb.append("AND gc_sender=? ");
		 */
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, mb_hp);
			
			dao.setRs (dao.pstmt().executeQuery());
			
			if (dao.rs().next()) {
				retVal = dao.rs().getInt("cnt");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS007";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS008";
		}
		finally {
			dao.closePstmt();
		}
		return retVal;
	}
	
	/**************************************
	 * get_mms_daily
	 * mms 디바이스 발송 갯수 가져오기
	 * @author Taebu  Moon <mtaebu@gmail.com>
	 * @param st_hp 상점 핸드폰 번호
	 * @return int
	 **************************************/
	private static int get_mms_daily(String st_hp)
    {
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT ");
		sb.append(" mm_daily_cnt ");
		sb.append("FROM ");
		sb.append(" prq_mms_log ");
		sb.append(" WHERE ");
		sb.append(" mm_sender=? ");
		sb.append(" and date(mm_datetime)=date(now()) ");
		sb.append(" ORDER BY ");
		sb.append(" mm_datetime DESC ");
		sb.append(" LIMIT 1;");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, st_hp);
			
			dao.setRs (dao.pstmt().executeQuery());

			if(dao.rs().wasNull()){
				retVal = 0;
			}else if (dao.rs().next()) {
				retVal = dao.rs().getInt("mm_daily_cnt");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS009";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS010";
		}
		finally {
			dao.closePstmt();
		}

		return retVal;
    }	
	
	/**
	 * 금일 보낸 발송 갯수 갱신
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $table 게시판 테이블
	 * @param string $id 게시물번호
	 * @return array
	 */
	private static void set_cdr(String[] str)
    {
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		
		sb.append("UPDATE prq_cdr SET ");
		sb.append("cd_day_limit=?,");
		sb.append("cd_device_day_cnt=?,");
		sb.append("cd_day_cnt=? ");
		sb.append(" WHERE cd_date=? ");
		sb.append(" and cd_tel=? ");
		sb.append(" and cd_hp=? ;");		
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			dao.pstmt().setString(3, str[2]);
			dao.pstmt().setString(4, str[3]);
			dao.pstmt().setString(5, str[4]);
			dao.pstmt().setString(6, str[5]);
			/* 조회한 콜로그의 일 발송량 갱신 */
			dao.pstmt().executeUpdate();

						
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS011";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS012";
		}
		finally {
			dao.closePstmt();
		}
    }
	
	/**
	 * STORE 리스트 가져오기
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $cd_id  콜로그 아이디
	 * @param string $cd_port 콜로그 포트
	 * @return array
	 */
 	private static String[] get_store(String[] str)
    {
		String[] s = new String[10]; 
		StringBuilder sb = new StringBuilder();
		s[0]="0";
		s[1]="150";
		s[2]="150";
		s[3]="150";
		s[4]="150";
		s[5]="150";
		s[6]="150";
		s[7]="150";
		s[8]="150";
		s[9]="150";
		MyDataObject dao = new MyDataObject();
				
		sb.append("SELECT ");
		sb.append(" st_no,");
		sb.append(" st_name,");
		sb.append(" st_mno,");
		sb.append(" st_tel_1,");
		sb.append(" st_hp_1,");
		sb.append(" st_thumb_paper,");
		sb.append(" st_top_msg, ");
		sb.append(" st_middle_msg,");
		sb.append(" st_bottom_msg, ");
		sb.append(" st_modoo_url ");
		sb.append(" from ");
		sb.append("prq_store ");
		sb.append("where ");
		sb.append("mb_id=? ");
		sb.append("and st_port=? ");	

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			
			dao.setRs (dao.pstmt().executeQuery());
			if (dao.rs().next()) {
				s[0]=dao.rs().getString("st_no");
				s[1]=dao.rs().getString("st_name");
				s[2]=dao.rs().getString("st_mno");
				s[3]=dao.rs().getString("st_tel_1");
				s[4]=dao.rs().getString("st_hp_1");
				s[5]=dao.rs().getString("st_thumb_paper");
				s[6]=dao.rs().getString("st_top_msg");
				s[7]=dao.rs().getString("st_middle_msg");
				s[8]=dao.rs().getString("st_bottom_msg");
				s[9]=dao.rs().getString("st_modoo_url");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS013";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS014";
		}
		finally {
			dao.closePstmt();
		}

		return s;
 	}

	/**
	 * kt STORE 리스트 가져오기
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $cd_id  콜로그 아이디
	 * @param string $cd_port 콜로그 포트
	 * @return array
	 */
 	
 	private static String[] get_store_kt(String[] str)
    {
		String[] s = new String[10]; 
		s[0]="0";
		s[1]="150";				
		s[2]="150";				
		s[3]="150";				
		s[4]="150";				
		s[5]="150";				
		s[6]="150";				
		s[7]="150";				
		s[8]="150";				
		s[9]="150";		
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
	
    	sb.append("select ");
		sb.append(" st_no,");
		sb.append(" st_name,");
		sb.append(" st_mno,");
		sb.append(" st_tel_1,");
		sb.append(" st_hp_1,");
		sb.append(" st_thumb_paper,");
		sb.append(" st_top_msg, ");
		sb.append(" st_middle_msg,");
		sb.append(" st_bottom_msg, ");
		sb.append(" st_modoo_url ");
		sb.append(" from ");
		sb.append("prq_store ");
		sb.append("where ");
		sb.append("mb_id=? ");
		sb.append("and st_tel_1=?");
		
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			
			dao.setRs (dao.pstmt().executeQuery());
			if (dao.rs().next()) 
			{
				s[0]=dao.rs().getString("st_no");
				s[1]=dao.rs().getString("st_name");
				s[2]=dao.rs().getString("st_mno");
				s[3]=dao.rs().getString("st_tel_1");
				s[4]=dao.rs().getString("st_hp_1");
				s[5]=dao.rs().getString("st_thumb_paper");
				s[6]=dao.rs().getString("st_top_msg");
				s[7]=dao.rs().getString("st_middle_msg");
				s[8]=dao.rs().getString("st_bottom_msg");
				s[9]=dao.rs().getString("st_modoo_url");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS015";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS016";
		}
		finally {
			dao.closePstmt();
		}
		return s;
 	}
 	

	/**
	 * set_cdr_kt 
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $cd_id  콜로그 아이디
	 * @param string $cd_port 콜로그 포트
	 * @return void
	 */
 	
 	private static void set_cdr_kt(String[] str)
    {
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		
		sb.append("UPDATE prq_cdr SET ");
		sb.append("cd_name=?,");
		sb.append("cd_tel=?,");
		sb.append("cd_hp=? ");
		sb.append(" WHERE cd_date=? ");
		sb.append(" and cd_port=0; ");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			dao.pstmt().setString(3, str[2]);
			dao.pstmt().setString(4, str[3]);
			/* 조회한 콜로그의 일 발송량 갱신 */
			dao.pstmt().executeUpdate();

						
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS017";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS018";
		}
		finally {
			dao.closePstmt();
		}
 	}
 	
	/**
	 * 블랙 리스트 가져오기
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * 080-130-8119
	 * @param string $cd_port 콜로그 포트
	 * @return array
	 */
 	private static String get_black()
    {
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		List<String> msg = new ArrayList<String>();
		sb.append("select ");
		sb.append("bl_hp ");
		sb.append("from ");
		sb.append("`callerid`.black_hp ");
		sb.append("where ");
		sb.append("bl_dnis='0801308119';");
		try {
			dao.openPstmt(sb.toString());
			dao.setRs(dao.pstmt().executeQuery());
			while(dao.rs().next()) 
			{
				msg.add(dao.rs().getString("bl_hp"));
			}
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS019";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS020";
		}
		finally {
			dao.closePstmt();
		}
		return String.join(",", msg);
	}

	/**
	 * 블로그 url on/off 사용 여부
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $st_no  상점아이디
	 * @return array
	 */
 	private static String get_blog_yn(String st_no)
    {

		String sql="";
		String retVal="";
		MyDataObject dao = new MyDataObject();		
		sql="select ";
		sql+="pv_value ";
		sql+="from ";
		sql+="prq_values ";
		sql+="where ";
		sql+="pv_code='5002' ";
		sql+="and pv_no='"+st_no+"';";
 
		try {
			dao.openPstmt(sql);
			dao.setRs(dao.pstmt().executeQuery());
			if(dao.rs().wasNull()){
				retVal="off";
			}else if(dao.rs().next()){
				retVal=dao.rs().getString("pv_value");
			}
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS021";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS022";
		}
		finally {
			dao.closePstmt();
		}
		return retVal;
    }

	// yyyy-MM-dd HH:mm:ss.0 을 yyyy-MM-dd HH:mm:ss날짜로 변경
	public static String chgDatetime(String str)
	{
		String retVal="";

		try{
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date historyDate = simpleDate.parse(str);
		retVal=simpleDate.format(historyDate);
		}catch(ParseException e){
		}
		return retVal;
	}

	
	/**
	 * 금일 보낸 발송 갯수 갱신
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $table 게시판 테이블
	 * @param string $id 게시물번호
	 * @return array
	 */
	private static void set_gcm_log(String[] str)
    {
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();

		sb.append("INSERT INTO `prq_gcm_log` SET ");
		sb.append("gc_subject=?,");
		sb.append("gc_content=?,");
		sb.append("gc_ismms='true',");
		sb.append("gc_receiver=?,");
		sb.append("gc_sender=?,");
		sb.append("gc_imgurl=?,");
		sb.append("gc_result=?,");
		sb.append("gc_ipaddr=?,");
		sb.append("gc_stno=?,");
		sb.append("gc_datetime=now();");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			dao.pstmt().setString(3, str[2]);
			dao.pstmt().setString(4, str[3]);
			dao.pstmt().setString(5, str[4]);
			dao.pstmt().setString(6, str[5]);
			dao.pstmt().setString(7, str[6]);
			dao.pstmt().setString(8, str[7]);
			/* 조회한 콜로그의 일 발송량 갱신 */
			dao.pstmt().executeUpdate();

						
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS023";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS024";
		}
		finally {
			dao.closePstmt();
		}
    }	

	/**
	 *  조회한 cdr 정보 발신으로 갱신
	 *
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string[0] cd_date
	 * @param string[1] cd_id
	 * @param string[2] cd_port
	 * @param string[3] cd_callerid
	 * @param string[4] cd_state
	 * @return void
	 */
	private static void set_sendcdr(String[] str)
    {
		MyDataObject dao = new MyDataObject();
		StringBuilder sb = new StringBuilder();
		
		sb.append("UPDATE prq_cdr SET cd_state=? ");		
		sb.append("WHERE cd_state=0 ");
		sb.append("and cd_date=? ");
		sb.append("and cd_id=? ");
		sb.append("and cd_port=? ");
		sb.append("and cd_callerid=?; ");
		
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[4]);
			dao.pstmt().setString(2, str[0]);
			dao.pstmt().setString(3, str[1]);
			dao.pstmt().setString(4, str[2]);
			dao.pstmt().setString(5, str[3]);
			/* 조회한 콜로그의 일 발송량 갱신 */
			dao.pstmt().executeUpdate();
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS025";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS026";
		}
		finally {
			dao.closePstmt();
		}
    }


	  /**
	  * 정규식 패턴 검증
	  * @param pattern
	  * @param str
	  * @return
	  */
	
	 public static boolean checkPattern(String pattern, String str){
	  boolean okPattern = false;
	  String regex = null;
	  
	  pattern = pattern.trim();
	  
	  //숫자 체크
	  if("num".equals(pattern)){
	   regex = "^[0-9]*$";
	  }
	  
	  //영문 체크
	  
	  //이메일 체크
	  if("email".equals(pattern)){
	   regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
	  }
	  
	  //전화번호 체크
	  if("tel".equals(pattern)){
	   regex = "^\\d{2,3}-\\d{3,4}-\\d{4}$";
	  }
	  
	  //휴대폰번호 체크
	  if("phone".equals(pattern)){
	   regex = "^01[016789]-?(\\d{3}|\\d{4})-?\\d{4}$";
	  }
	  //System.out.println(regex);
	  okPattern = Pattern.matches(regex, str);
	  return okPattern;
	 }

	 
	 
	/**
	 * set_hist
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @return void
	 */
	private static void set_hist()
    {
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();

		sb.append("insert into prq_cdr_tmp ");
		sb.append("select * from prq_cdr ");
		sb.append("where cd_state>0;");// 처리가
		
		try {
			dao.openPstmt(sb.toString());
			//dao.pstmt().setString(1, historytable);
			/* 조회한 콜로그의 일 발송량 갱신 */
			dao.pstmt().executeUpdate();
						
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS027";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS028";
		}
		finally {
			/* 처리된 건 지우기 */
			del_hist();
			dao.closePstmt();
		}
    }	

		
		/**
		 * set_hist
		 *
		 * @author Taebu Moon <mtaebu@gmail.com>
		 * @param string $table 게시판 테이블
		 * @param string $id 게시물번호
		 * @return array
		 */
		private static void del_hist()
	    {
			StringBuilder sb = new StringBuilder();

			MyDataObject dao = new MyDataObject();
					
			sb.append("delete from prq_cdr where cd_state>0;");// 처리가 진행중인 것은 지우지 않는다. 
			try {
				dao.openPstmt(sb.toString());
				/* 조회한 콜로그의 일 발송량 갱신 */
				dao.pstmt().executeUpdate();

							
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS029";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS030";
			}
			finally {
				dao.closePstmt();
			}
	    }	

		
		/**
		 * 
		 * @param mb_hp
		 * @param st_no
		 * @return
		 */
		public static void set_happycall(String mb_hp,String st_no) throws MySQLIntegrityConstraintViolationException
	    {
			StringBuilder sb = new StringBuilder();
			//StringBuilder sb2 = new StringBuilder();
			
			MyDataObject dao = new MyDataObject();
			//MyDataObject dao2 = new MyDataObject();
			long unixtime_40m=0L;
			//int last_id=0;
			
			unixtime_40m=System.currentTimeMillis() / 1000;
			
			/* 40 분을 더한 다. */
			unixtime_40m=unixtime_40m+40*60;
			
			sb.append("INSERT INTO prq_happycall_log SET ");
			sb.append("hc_unixtime=?,");
			sb.append("hc_hp=?, ");
			sb.append("st_no=?, ");
			sb.append("hc_status=?, ");
			sb.append("hc_date=? ");
			
			try {
				dao.openPstmt(sb.toString());
				dao.pstmt().setLong(1, unixtime_40m);
				dao.pstmt().setString(2, mb_hp);
				dao.pstmt().setString(3, st_no);
				dao.pstmt().setString(4, "0");
				dao.pstmt().setString(5, Utils.getyyyymmdd());
				/* 조회한 콜로그의 일 발송량 갱신 */
				dao.pstmt().executeUpdate();
				/*
				sb2.append("select LAST_INSERT_ID() last_id;");
				dao2.openPstmt(sb2.toString());
				dao2.setRs(dao2.pstmt().executeQuery());
				
				if (dao2.rs().next()) {
					last_id = dao2.rs().getInt("last_id");
				}
				*/		
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS031";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS032";
				//com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
			}finally {
				dao.closePstmt();
			}
			//return last_id; 
	    }

		/**************************************
		 * get_mms_daily
		 * mms 디바이스 발송 갯수 가져오기
		 * @author Taebu  Moon <mtaebu@gmail.com>
		 * @return int
		 **************************************/
		private static String[] get_happycall()
	    {
			String[] s = new String[3]; 
			s[0]="-1";
			s[1]="-1";
			s[2]="-1";
			
			StringBuilder sb = new StringBuilder();
			MyDataObject dao = new MyDataObject();
			long unixtime=0L;
			unixtime=System.currentTimeMillis() / 1000;
			
			sb.append("SELECT ");
			sb.append(" hc_hp,st_no,hc_no ");
			sb.append("FROM ");
			sb.append(" prq_happycall_log ");
			sb.append(" WHERE ");
			sb.append(" hc_status=? ");
			sb.append(" and hc_unixtime<? ");
			sb.append(" LIMIT 1");
			try {
				dao.openPstmt(sb.toString());
				dao.pstmt().setString(1, "0");
				dao.pstmt().setLong(2, unixtime);
				dao.setRs (dao.pstmt().executeQuery());

				if(dao.rs().wasNull()){
					
				}else if (dao.rs().next()) {
					s[0]=dao.rs().getString("hc_hp");
					s[1]=dao.rs().getString("st_no");
					s[2]=dao.rs().getString("hc_no");				
				}						
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS033";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS034";
			}
			finally {
				dao.closePstmt();
			}
			return s;
	    }
		
		/**
		 * 
		 * @param message
		 * @param st_no
		 * @param mms_title
		 * @param cd_callerid
		 * @param cd_hp
		 * @param st_thumb_paper
		 * @param ishappycall
		 */
		private static void set_gcmurl(String message,String st_no,String mms_title,String cd_callerid,String cd_hp,String st_thumb_paper,boolean ishappycall)
		{
			
			String query="";
			URL targetURL;
			URLConnection urlConn;
			query="is_mms=true";
			query+="&message="+message;
			query+="&st_no="+st_no;
			query+="&title="+mms_title;
			query+="&receiver_num="+cd_callerid;
			query+="&phone="+cd_hp;
			query+="&mode=crontab";
		
			if(ishappycall)
			{
				query+="&happycall=true";
				query+="&img_url=http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
			}else{
				query+="&happycall=false";
				query+="&img_url=http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
			}

			try {
				
				targetURL = new URL("http://prq.co.kr/prq/set_gcm.php");
				urlConn = targetURL.openConnection();
				HttpURLConnection cons = (HttpURLConnection) urlConn;
				// 헤더값을 설정한다.
				cons.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

				cons.setRequestMethod("POST");
				//cons.getOutputStream().write("LOGIN".getBytes("UTF-8"));
				cons.setDoOutput(true);
				cons.setDoInput(true);
				cons.setUseCaches(false);
				cons.setDefaultUseCaches(false);
				
				/*
				PrintWriter out = new PrintWriter(cons.getOutputStream());
				out.close();*/
				//System.out.println(query);
				OutputStream opstrm=cons.getOutputStream();
				opstrm.write(query.getBytes());
				opstrm.flush();
				opstrm.close();

				String buffer = null;
				String bufferHtml="empty";
				BufferedReader in = new BufferedReader(new InputStreamReader(cons.getInputStream()));

				 while ((buffer = in.readLine()) != null) {
					 bufferHtml += buffer;
				}
				//Utils.getLogger().info(bufferHtml);
				in.close();				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS035";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS036";
			}
		}

		/**
		 * kt STORE 리스트 가져오기
		 *
		 * @author Taebu Moon <mtaebu@gmail.com>
		 * @param string $cd_id  콜로그 아이디
		 * @param string $cd_port 콜로그 포트
		 * @return array
		 */
	 	
	 	private static String[] get_storeno(String st_no)
	    {
			String[] s = new String[10]; 
			s[0]="0";
			s[1]="150";				
			s[2]="150";				
			s[3]="150";				
			s[4]="150";				
			s[5]="150";				
			s[6]="150";				
			s[7]="150";				
			s[8]="150";				
			s[9]="150";		
			StringBuilder sb = new StringBuilder();

			MyDataObject dao = new MyDataObject();
		
	    	sb.append("select ");
			sb.append(" st_no,");
			sb.append(" st_name,");
			sb.append(" st_mno,");
			sb.append(" st_tel_1,");
			sb.append(" st_hp_1,");
			sb.append(" st_thumb_paper,");
			sb.append(" st_top_msg, ");
			sb.append(" st_middle_msg,");
			sb.append(" st_bottom_msg, ");
			sb.append(" st_modoo_url ");
			sb.append(" from ");
			sb.append("prq_store ");
			sb.append("where ");
			sb.append("st_no=? ");
			
			try {
				dao.openPstmt(sb.toString());
				dao.pstmt().setString(1, st_no);
				
				dao.setRs (dao.pstmt().executeQuery());
				if (dao.rs().next()) 
				{
					s[0]=dao.rs().getString("st_no");
					s[1]=dao.rs().getString("st_name");
					s[2]=dao.rs().getString("st_mno");
					s[3]=dao.rs().getString("st_tel_1");
					s[4]=dao.rs().getString("st_hp_1");
					s[5]=dao.rs().getString("st_thumb_paper");
					s[6]=dao.rs().getString("st_top_msg");
					s[7]=dao.rs().getString("st_middle_msg");
					s[8]=dao.rs().getString("st_bottom_msg");
					s[9]=dao.rs().getString("st_modoo_url");
				}			
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS037";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS038";
			}
			finally {
				dao.closePstmt();
			}
		return s;
	}


		/**************************************
		 * set_happycall_mms
		 * 블로그 홍보를 사용하는 매장에 한에서 mms 홍보 문구 gcm로 등록 
		 * @author Taebu  Moon <mtaebu@gmail.com>
		 * @return boolean
		 **************************************/
	 	private static boolean set_happycall_mms(Boolean is_set_limit,Boolean is_hp,String[] happycall_info)
	    {
			List<String> msgs = new ArrayList<String>();
			String message="";
			
			boolean chk_mms =false;
			String[] store_info=get_storeno(happycall_info[1]);
			try {
				if(!is_set_limit)
				{
				msgs.add("[광고]["+store_info[1]+"]");
				msgs.add("사진찍고 리뷰 쓰면");
				msgs.add("2,000 포인트 드려요!");
				msgs.add("http://prq.co.kr/prq/blog/write/"+happycall_info[1]);
				msgs.add("");
				msgs.add("수신거부");
				msgs.add("080-130-8119");
				
				if(store_info[2].equals("LG")||store_info[2].equals("SK")){
					message=String.join("\n", msgs);
				}else if(store_info[2].equals("KT")){
					message=String.join("<br>", msgs);
				}else{
					message=String.join("<br>", msgs);
				}
				set_gcmurl(message,store_info[0],"블로그 리뷰", happycall_info[0],store_info[4],store_info[5],true);
				}
			}catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS039";
			}
			return chk_mms; 
	    }


		private static String chg_mms_msg(String st_bottom_msg,String st_modoo_url,String[] store_info)
	    {
			String message="";
			List<String> msg = new ArrayList<String>();
			msg.add(store_info[6]);

			// "foo and bar and baz"
			if(store_info[2].equals("LG")){
				//$msg[]=str_replace(array("\r\n", "\r",'<br />','<br>'), '\n', $st->st_middle_msg);
				msg.add(store_info[7]);
			}else if(store_info[2].equals("KT")){
				msg.add(store_info[7].replaceAll("(\\r|\\n)","<br>"));		
			}else if(store_info[2].equals("SK")){
				// SK 는 아무것도 하지 않는다
				msg.add(store_info[7]);
			}else{
				msg.add(store_info[7].replaceAll("(\\r|\\n)","<br>"));
			}
			
			if(get_blog_yn(store_info[0]).equals("on"))
			{
				msg.add("");
				msg.add("리뷰 이벤트 (2,000원 지급)");
				msg.add("http://prq.co.kr/prq/blog/write/"+store_info[0]);
			}
			msg.add(st_bottom_msg);
			msg.add(st_modoo_url);

			if(store_info[2].equals("LG")||store_info[2].equals("SK")){
				message=String.join("\n", msg);
			}else if(store_info[2].equals("KT")){
			//msg=join("<br>",$msg);
				message=String.join("<br>", msg);
			}else{
				message=String.join("\n", msg);
			}
			
			message=message.replaceAll("\\#\\{homepage\\}","http://prq.co.kr/prq/page/"+store_info[0]);
			message=message.replaceAll("\\#\\{st\\_tel\\}",store_info[3]);
			
			return message;
	    }	
		
		public static boolean is_limit(String cd_hp)
		{
			boolean is_set_limit=false;
			
			String mno_limit[]=get_mno_limit(cd_hp);
			int mn_mms_limit=Integer.parseInt(mno_limit[0]);
			//int mn_dup_limit=Integer.parseInt(mno_limit[1]);
			
			
			/* 오늘 기기에서 보낸 총 갯수 = 기기에서 보낸 mms + prq에서 보낸 mms 갯수*/
			int my_device_cnt=get_send_cnt(cd_hp)+get_device_cnt(cd_hp);
			if(mn_mms_limit==0){
				is_set_limit=false;
			}else{
				is_set_limit=my_device_cnt>=mn_mms_limit;	
			}
			
			return is_set_limit; 
		}

		/**
		 * get_device_cnt
		 * prq_cdr_tmp 에서 당일 핸드폰에서 수동으로 전송한 갯수를 가져온다.   
		 * @param mb_hp
		 * @return int
		 */
		private static int get_device_cnt(String mb_hp){
			int retVal = 0;
			StringBuilder sb = new StringBuilder();
			MyDataObject dao = new MyDataObject();
			
			sb.append("select cd_device_day_cnt cnt FROM `prq_cdr_tmp`");
			sb.append(" where cd_hp=? ");
			sb.append("and date(cd_date)=date(now()) ");
			sb.append("order by cd_device_day_cnt desc ");
			sb.append("limit 1;");
			try {
				dao.openPstmt(sb.toString());
				dao.pstmt().setString(1, mb_hp);
				
				dao.setRs (dao.pstmt().executeQuery());
				
				if (dao.rs().next()) {
					retVal = dao.rs().getInt("cnt");
				}			
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS040";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS041";
			}
			finally {
				dao.closePstmt();
			}
			return retVal;
		}


		/**
		 * 금일 보낸 발송 갯수 갱신
		 *
		 * @author Taebu Moon <mtaebu@gmail.com>
		 * @param string $table 게시판 테이블
		 * @param string $id 게시물번호
		 * @return array
		 */
		private static void set_happycall_log(String hc_status,String hc_no)
	    {
			StringBuilder sb = new StringBuilder();
			MyDataObject dao = new MyDataObject();
			
			sb.append("update prq_happycall_log set ");
			sb.append("hc_status=? ");
			sb.append("where hc_no=?;");
			
			try {
				dao.openPstmt(sb.toString());
				
				dao.pstmt().setString(1, hc_status);
				dao.pstmt().setString(2, hc_no);
				dao.pstmt().executeUpdate();
							
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS042";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS043";
			}
			finally {
				dao.closePstmt();
			}
	    }
}
