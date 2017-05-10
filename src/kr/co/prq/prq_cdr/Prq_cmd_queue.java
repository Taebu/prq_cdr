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
 * 2017-01-02 (목) 오전 10:42
 * @author Taebu
 *  
 */
public class Prq_cmd_queue {
	
	/**
	 * prq_cdr 테이블의 데이터를 처리하기 위한 주요한 처리를 수행한다.
	 */
	public static void doMainProcess() {
		Connection con = DBConn.getConnection();
		
		String cd_date	="";				
		String cd_id="";					
		String cd_port="";				
		String cd_callerid="";			
		String cd_calledid="";			
		String cd_name="";				
		String cd_tel="";						
		String cd_hp="";					
		String last_cdr="first_sent";
		String chk_limit_date="";
		String message="";
		
		String result_msg="";
		String mms_title="";
		String img_url="";
		String gc_ipaddr="123.142.52.91";
		
		/* 상점 정보*/
		String st_no="";
		String st_name="";
		String st_mno="";
		String st_tel_1="";
		String st_hp_1="";
		String st_tel="";
		String st_hp="";
		String st_thumb_paper="";
		String st_top_msg="";
		String st_middle_msg="";
		String st_bottom_msg="";
		String st_modoo_url="";
		String is_blogon="off";

		int cd_state=0;					
		int cd_day_cnt=0;				
		int cd_day_limit=0;				
		int cd_device_day_cnt=0;		
		int day_cnt=0;
		int mno_device_daily=0;
		int mn_mms_limit=0;
		int mn_dup_limit=0;
		int chk_cd_date=0;
		int daily_mms_cnt=0;
		int mm_daily_cnt=0;
		String[] mno_limit = new String[2];
		
		boolean chk_mms = true;
		boolean is_hp = false;
		boolean is_hpcall = false;
		
		String black_list="";
		Long startTime =0L;
		Long endTime=0L;
		Long totalTime=0L;
        
		
		/* 상점 정보 */
		String[] store_info			= new String[10];
		/* 콜로그 데이터 */
		String[] cdr_info	= new String[6];
		/* config 데이터 */
		String[] config	= new String[6];
		/* gcm_log 데이터 */
		String[] gcm_log	= new String[8];
		/* gcm_log 데이터 */
		String[] happy_log	= new String[3];
		
		if (con != null) {
			MyDataObject dao = new MyDataObject();
			StringBuilder sb = new StringBuilder();

			sb.append("select * from prq_cdr  ");
			sb.append("WHERE cd_state=0 ");
			sb.append("limit 15;");
			
			try {

				startTime = System.currentTimeMillis();


				dao.openPstmt(sb.toString());

				dao.setRs(dao.pstmt().executeQuery());
				/*****************************************************
				* 2-2. 리스트 출력 
				* SELECT TIMESTAMPDIFF(DAY,'2009-05-18','2009-07-29');
				***************************************************/
				while(dao.rs().next()) 
				{
					/****************************************************************************** 
					* 1. 블랙 리스트 가져오기 
					* 
					******************************************************************************/
					black_list=get_black();
					
					PRQ_CDR.heart_beat = 1;
					
					String hist_table = DBConn.isExistTableYYYYMM();
					//set_hist(hist_table);
					//del_hist();
					
					is_hp=checkPattern("phone",dao.rs().getString("cd_callerid"));
					/*	String cd_date 날짜정보, */
					cd_date=chkValue(dao.rs().getString("cd_date"));
					/*	String cd_id 아이디  */
					cd_id=chkValue(dao.rs().getString("cd_id"));
					/*	String cd_port 콜로그 포트 */
					cd_port=chkValue(dao.rs().getString("cd_port"));
					
					/*	String cd_callerid, 수신인 */
					cd_callerid=chkValue(dao.rs().getString("cd_callerid"));
					
					/*	String cd_calledid, 발신인 */
					cd_calledid=chkValue(dao.rs().getString("cd_calledid")); 
					
					/*	String cd_name, 발신인 */
					cd_name=chkValue(dao.rs().getString("cd_name"));
					
					/*	String cd_tel, 발신인 */
					cd_tel=chkValue(dao.rs().getString("cd_tel"));
					
					/*	String cd_hp, 발신인 */
					cd_hp=chkValue(dao.rs().getString("cd_hp"));
					
					/*	Int cd_state 상태코드, */
					cd_state=dao.rs().getInt("cd_state");
					
					/*	Int cd_day_cnt 일별전송, */
					cd_day_cnt=dao.rs().getInt("cd_day_cnt");
					
					/*	Int cd_day_limit 일변제한, */
					cd_day_limit=dao.rs().getInt("cd_day_limit");
					
					/*	Int cd_device_day_cnt 기기 일별제한, */
					cd_device_day_cnt=dao.rs().getInt("cd_device_day_cnt");

					/*******************************************************************************
					* 3. get_last_cdr 
					* - 마지막 바로 전 cdr 정보 조회 
					* - 지금 들어온 데이터는 당연 예외 처리 값을 비교한 값만을 참조하고,
					* - 처음 보내는 것은 first_send로 명명한다.
					*******************************************************************************/
					/*0000-00-00 00:00:00.0 to 0000-00-00 00:00:00 */
					cd_date=chgDatetime(cd_date);
					last_cdr=get_last_cdr(cd_date,cd_tel,cd_hp,cd_callerid);
					
					/*******************************************************************************
					* 4. get_mno_limit
					* - 중복 발송일 수 조회 기본값은 0인데  
					* - 값이 mn_dup_limit 만약 3이라면, 
					* - 마지막 콜로그와 대조해 보아서 
					* - 3일 동안 보내지 않습니다.  
					* - NEW] mn_limit_
					* return array[0]
					* mno_limit[0] =	mn_mms_limit
					* mno_limit[1] = mn_dup_limit
					******************************************************************************/						
					mno_limit=get_mno_limit(cd_id);
						

					/********************************************************************************
					* 5. array get_send_cnt
					* - 이번달 발송 수 조회 
					********************************************************************************/
					day_cnt=get_send_cnt(cd_hp);
					
					/********************************************************************************
					* 6-1. array get_mms_daily
					* - mms_daily 정보 가져 오기
					*********  ***********************************************************************/
					mno_device_daily=get_mms_daily(cd_hp);
					mm_daily_cnt=mno_device_daily;

					/********************************************************************************
					* 6-2. void set_cdr
					* - cdr 정보 세팅
					********************************************************************************/
					mn_mms_limit=Integer.parseInt(mno_limit[0]);
					mn_dup_limit=Integer.parseInt(mno_limit[1]);
					
					mn_mms_limit=mn_mms_limit>0?mn_mms_limit:150;
					/**
					 * cdr_info[0] 'cd_date'
					 * cdr_info[1] 'cd_tel'
					 * cdr_info[2] 'cd_hp'
					 * cdr_info[3] 'cd_device_day_cnt'
					 * cdr_info[4] 'cd_day_limit'
					 * cdr_info[5] 'get_day_cnt'
					 * 
					 * 	sb.append("cd_device_day_cnt=?,");
					 *	sb.append("cd_day_cnt=? ");
					 * 	sb.append(" WHERE cd_date=? ");
					 *	sb.append(" and cd_tel=? ");
					 * 	sb.append(" and cd_hp=? ;");
					 */
					//cdr_info[0]=Integer.toString(day_cnt);
					cdr_info[0]=Integer.toString(mm_daily_cnt);
					cdr_info[1]=Integer.toString(day_cnt);
					cdr_info[2]=cd_date;
					cdr_info[3]=cd_tel;
					cdr_info[4]=cd_hp;
					     
					set_cdr(cdr_info);
					
					
					
					
					if(last_cdr.equals("first_sent")){
						chk_limit_date="처음 보냄";
					}else{
						chk_cd_date=Integer.parseInt(last_cdr);
						chk_limit_date=mn_dup_limit>chk_cd_date?"보내면 안됨":"보냄";
					}

					/********************************************************************************
					* 
					* 7.array get_store 
					* - 기기 CID인 경우( * KT_CID 아닌 경우)
					* - 이메일과 포트 번호로 상점 정보 조회
					*
					********************************************************************************/
					if(cd_port.equals("0")){
						/* kt CID 상점 정보 */
						config[0]=cd_id;
						config[1]=cd_calledid;
						store_info=get_store_kt(config);
						
					}else if(!cd_port.equals("0")){
						/* 일반 CID 상점 정보 */
						config[0]=cd_id;
						config[1]=cd_port;
						store_info=get_store(config);
						//store_info[5];
					}
				
					st_no=store_info[0];
					st_name=store_info[1];
					st_mno=store_info[2];
					st_tel_1=store_info[3];
					st_hp_1=store_info[4];
					st_thumb_paper=store_info[5];
					st_top_msg=store_info[6];
					st_middle_msg=store_info[7];
					st_bottom_msg=store_info[8];
					st_modoo_url=store_info[9];				
				
				
				/* 콜로그가 KT 장비 인 경우*/
				if(!store_info[0].equals(""))
				{
					if(cd_port.equals("0"))
					{
						/*
							sb.append("cd_name=?,");
							sb.append("cd_tel=?,");
							sb.append("cd_hp=? ");
							sb.append(" WHERE cd_date=? ");
							sb.append(" and cd_port=0; ");
						/********************************************************************************
						* 8. void set_cdr_kt 
						* - KT_CID 포트 구분이 없다.
						* - PRQ_CID 역시 포트 구분이 없다.
						* - 개발 당시 한번의 핸드폰 건만 하루 전송하고 이외에 콜은 인정하지 않는다.
						* - cdr kt 세팅
						********************************************************************************/
						cdr_info[0]=st_name;
						cdr_info[1]=st_tel_1;
						cdr_info[2]=st_hp_1;
						cdr_info[3]=cd_date;
						//페이지네이션 기본 설정
						
						set_cdr_kt(cdr_info);
						//$li->cd_hp=$st->st_hp_1;
						cd_hp=store_info[4];
					}/* if($li->cd_port=="0"){...} */
					

					/*mms 발송 여부*/
					chk_mms=true;
					
					//StringBuilder msg= new StringBuilder();
					
					/* 1.8 버전 가능 */
					List<String> msg = new ArrayList<String>();

					msg.add(st_top_msg);
					mms_title=st_top_msg;
					// "foo and bar and baz"
					if(st_mno.equals("LG")){
						//$msg[]=str_replace(array("\r\n", "\r",'<br />','<br>'), '\n', $st->st_middle_msg);
						msg.add(st_middle_msg);
					}else if(st_mno.equals("KT")){
						msg.add(st_middle_msg.replaceAll("(\\r|\\n)","<br>"));		
					}else if(st_mno.equals("SK")){
						// SK 는 아무것도 하지 않는다
						msg.add(st_middle_msg);
					}
					//$mms_title=strlen($st->st_top_msg)>3?$st->st_top_msg:"web";
					// mms_title=store_info[6];
					
					is_blogon=get_blog_yn(st_no);
					
					if(is_blogon.equals("on"))
					{
						msg.add("");
						msg.add("리뷰 이벤트 (2,000원 지급)");
						msg.add("http://prq.co.kr/prq/blog/write/"+st_no);
						
						//set_happycall(String mb_hp,String st_no)
						if(is_hp)
						set_happycall(cd_callerid,st_no);
					}
					msg.add(st_bottom_msg);
					msg.add(st_modoo_url);
					/*
					$param=ARRAY();
					$param['url']="http://prq.co.kr/prq/set_gcm.php";
					$param['return_type']='';
					*/
					if(st_mno.equals("LG")){
						message=String.join("\n", msg);
					}else if(st_mno.equals("KT")){
					//msg=join("<br>",$msg);
						message=String.join("<br>", msg);
					}else if(st_mno.equals("SK")){
					//msg=join("\r\n",$msg);
						message=String.join("\n", msg);
					}

					//msg=str_replace("#{homepage}","http://prq.co.kr/prq/page/".st_no,$msg);
					message=message.replaceAll("\\#\\{homepage\\}","http://prq.co.kr/prq/page/"+store_info[0]);
					//msg=str_replace("#{st_tel}",phone_format(st_tel_1),$msg);
					message=message.replaceAll("\\#\\{st\\_tel\\}",store_info[3]);
					//Utils.getLogger().info("message : "+message);
					//echo $msg;
					

					/********************************************************************************
					* 9. void set_gcm_log
					* - gcm 로그에 따라 prq DB에 gcm_log 발생
					*
					********************************************************************************/
					img_url="http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
					//수신거부 여부 체크
//					if(in_array(cd_callerid,black_arr))
					
					if(black_list.contains(cd_callerid))
					{
						/*gcm 로그 발생*/
						result_msg= "수신거부";

						if(cd_port.equals("0"))
						{
							//$li->cd_hp=$st->st_hp_1;
							cd_hp= st_hp_1;
							cd_tel= st_tel_1;
						}
						
						if(is_hp)
						{
							gcm_log[0]=mms_title;
							gcm_log[1]=message;
							gcm_log[2]=cd_callerid;
							gcm_log[3]=cd_hp;
							gcm_log[4]=img_url;
							gcm_log[5]=result_msg;
							gcm_log[6]=gc_ipaddr;
							gcm_log[7]=st_no;
							set_gcm_log(gcm_log);
							chk_mms=true;
						}
						chk_mms=false;
						
					}


					
					/* 일간 mms 발송건 초기값 */
					daily_mms_cnt=0;
					/* 일간 mms 발송건 디바이스 값 */
					daily_mms_cnt+=mm_daily_cnt;
					/* 일간 mms 발송건 prq 값 */
					daily_mms_cnt+=cd_day_cnt;
					
					/********************************************************************************
					*
					* 9-1. if($cd_date=="first_send"){...}
					* - 처음 보낼 때 안보내지던 버그 수정
					* - $chk_mms = true;
					*********************************************************************************/
					if(last_cdr.equals("first_sent")){
						/*gcm 로그 발생*/
						result_msg= "처음 발송 / "+mn_dup_limit;
						
						if(cd_port.equals("0"))
						{
							cd_hp=st_hp_1;
						}
						
						chk_mms=true;
					
					/********************************************************************************
					* 9-2. void set_gcm_log
					* 중복 제한 보내면 안됨 
					* prq_gcm_log 중복제한 로그 발생
					********************************************************************************/
					}else if(mn_dup_limit>Integer.parseInt(last_cdr)){
						/*gcm 로그 발생*/
						/* 2016-11-22 (화)
						* https://github.com/Taebu/prq/issues/57
						* 조정흠씨 자체 개발로 인해 중복 제한 비활성화
						*/

						//$result_msg= $cd_date."/".$get_mno_limit->mn_dup_limit."일 중복 제한";
						result_msg= last_cdr+"/"+mn_dup_limit+"일 발송";
						
						if(cd_port.equals("0"))
						{
							cd_hp=st_hp_1;
						}
						
						if(is_hp)
						{
							gcm_log[0]=mms_title;
							gcm_log[1]=message;
							gcm_log[2]=cd_callerid;
							gcm_log[3]=cd_hp;
							gcm_log[4]=img_url;
							gcm_log[5]=result_msg;
							gcm_log[6]=gc_ipaddr;
							gcm_log[7]=st_no;
							set_gcm_log(gcm_log);
						}
						/* 2016-11-22 (화)
						* https://github.com/Taebu/prq/issues/57
						* 조정흠씨 자체 개발로 인해 중복 제한 비활성화
						*/
						//$chk_mms=false;
					/********************************************************************************
					* 9-3. void set_gcm_log
					* 150건 제한
					* prq_gcm_log 150건 제한 로그 발생
					********************************************************************************/
					}else if(daily_mms_cnt>mn_mms_limit){
						/*gcm 로그 발생*/
						result_msg= cd_day_cnt+"/"+mn_mms_limit+"건 제한";
						
						if(cd_port.equals("0"))
						{
							cd_hp=st_hp_1;
						}
						if(is_hp)
						{
							gcm_log[0]=mms_title;
							gcm_log[1]=message;
							gcm_log[2]=cd_callerid;
							gcm_log[3]=cd_hp;
							gcm_log[4]=img_url;
							gcm_log[5]=result_msg;
							gcm_log[6]=gc_ipaddr;
							gcm_log[7]=st_no;
							set_gcm_log(gcm_log);
						}
						chk_mms=false;
					}		


					
					/********************************************************************************
					*
					* 9-4. curl->simple_post('http://prq.co.kr/prq/set_gcm.php')
					* - 수신거부 중복, 150건 제한 혹은 설정한 일수 제한 아닌 경우만
					* - $chk_mms = true;
					*********************************************************************************/
					if(chk_mms&&is_hp)
					{
						/********************************************************************************
						* 10. void set_gcm
						* - curl 전송
						********************************************************************************/
						/*
						$config=array(
							'is_mms'=>'true',
							'message'=>$msg,
							'st_no'=>$st->st_no,
							'title'=>$mms_title,
							'receiver_num'=>$li->cd_callerid,
							'phone'=>$li->cd_hp,
							'img_url'=>"http://prq.co.kr/prq/uploads/TH/".$st->st_thumb_paper,
							'mode'=>'cront+'
						);
						*/
						set_gcmurl(message,st_no,mms_title,cd_callerid,cd_hp,st_thumb_paper,false);
					}
					
					/*if($chk_mms){...}*/
				//}/* foreach($store as $st){...}*/
					//select * from prq_cdr where cd_date='2017-01-11 10:06:54' and cd_id='0314348635@naver.com' and cd_port='0' and cd_callerid='01055599880';
					
					/* 발송 처리 핸드폰 일반 번호 */
					cdr_info[0]=cd_date;
					cdr_info[1]=cd_id;
					cdr_info[2]=cd_port;
					cdr_info[3]=cd_callerid;
					cdr_info[4]=cd_hp;
					cdr_info[5]=cd_tel;
					set_sendcdr(cdr_info);
					
					/*
					 * happycall 
					 * 
					 * */
					happy_log=get_happycall();
					is_hpcall=!happy_log[0].equals("-1");
					
					if(is_hpcall)
					{
						List<String> msgs = new ArrayList<String>();
						/*
						happy_log[0]=hc_hp	
						happy_log[1]=st_no
						*/	
						st_no=happy_log[1];
						cd_callerid=happy_log[0];
						
						store_info=get_storeno(st_no);
						st_no=store_info[0];
						st_name=store_info[1];
						st_mno=store_info[2];
						st_tel_1=store_info[3];
						st_hp_1=store_info[4];
						st_thumb_paper=store_info[5];
						st_top_msg=store_info[6];
						st_middle_msg=store_info[7];
						st_bottom_msg=store_info[8];
						st_modoo_url=store_info[9];
						cd_hp=st_hp_1;
						msgs.add("[광고]["+st_name+"]");
						msgs.add("사진찍고 리뷰 쓰면");
						msgs.add("2,000 포인트 드려요!");
						msgs.add("http://prq.co.kr/prq/blog/write/"+happy_log[1]);
						msgs.add("");
						msgs.add("수신거부");
						msgs.add("080-130-8119");
						mms_title="블로그 리뷰";
						if(st_mno.equals("LG")){
							message=String.join("\n", msgs);
						}else if(st_mno.equals("KT")){
						//msg=join("<br>",$msg);
							message=String.join("<br>", msgs);
						}else if(st_mno.equals("SK")){
						//msg=join("\r\n",$msg);
							message=String.join("\n", msgs);
						}
						
						set_gcmurl(message,st_no,mms_title,cd_callerid,cd_hp,st_thumb_paper,true);
					}/* if(is_hpcall){...}*/
					
				}/* while(dao.rs().next()){...} */	
			}
				
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS001";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS002";
				Utils.getLogger().warning(Utils.stack(e));
			}
			finally {
				dao.closePstmt();
		        endTime = System.currentTimeMillis();
		        // 시간 출력
		        Utils.getLogger().warning("##  소요시간(초.0f) : " + ( endTime - startTime )/1000.0f +"초");
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
		sb.append(" prq_cdr ");
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
	* get_mno_limit(cd_date, cd_tel,cd_hp,cd_callerid)
	* @param String cd_date 
	* @param String cd_tel
	* @param String cd_hp
	* @param String cd_callerid
	* @return array
	*/
	private static String[] get_mno_limit(String cd_hp) {
		String[] s = new String[2]; 
		s[0]="7";
		s[1]="150";
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT  ");
		sb.append(" mn_mms_limit,  ");
		sb.append(" mn_dup_limit  ");
		sb.append(" FROM ");
		sb.append(" prq_mno ");
		sb.append("WHERE mn_email=?");
		

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
	 * @param mb_hp
	 * @return int
	 */
	private static int get_send_cnt(String mb_hp){
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT count(*) cnt ");
		sb.append("FROM `prq_gcm_log` ");
		sb.append("WHERE date(now())=date(gc_datetime) ");
		sb.append("AND gc_sender=? ");

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
		/*
		//CDR 정보 조회
		$cdr_info = array(
		'cd_date'=> $li->cd_date,
		'cd_tel'=> $li->cd_tel,
		'cd_hp' =>$li->cd_hp,
		'get_day_cnt' =>$get_day_cnt->cnt);
		dao2.pstmt().executeUpdate()
		*/
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		
		sb.append("UPDATE prq_cdr SET ");
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
	
	//select bl_hp from `callerid`.black_hp where bl_dnis='0801308119';
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
				
/*
 * 						gcm_log[0]=mms_title;
						gcm_log[1]=message;
						gcm_log[2]=cd_callerid;
						gcm_log[3]=cd_hp;
						gcm_log[4]=img_url;
						gcm_log[5]=result_msg;
						gcm_log[6]=gc_ipaddr;
						gcm_log[7]=st_no;
 * */
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
	 * @param string $table 게시판 테이블
	 * @param string $id 게시물번호
	 * @return array
	 */
	private static void set_sendcdr(String[] str)
    {
		//select * from prq_cdr where cd_date='2017-01-11 10:06:54' and cd_id='0314348635@naver.com' and cd_port='0' and cd_callerid='01055599880';
		/*
		$sql = "UPDATE prq_cdr SET cd_state=1 WHERE cd_state=0 and cd_callerid like '01%';";

		$sql = "UPDATE prq_cdr SET cd_state=2 WHERE cd_state=0 and cd_callerid not like '01%';";
		 */
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		
		if(checkPattern("phone",str[3])){
			sb.append("UPDATE prq_cdr SET cd_state=1 ");
		}else if(!checkPattern("phone",str[3])){
			sb.append("UPDATE prq_cdr SET cd_state=2 ");
		}else if(str[4].equals("")){
			sb.append("UPDATE prq_cdr SET cd_state=3 ");
		}else if(str[5].equals("")){
			sb.append("UPDATE prq_cdr SET cd_state=4 ");
		}else{
			sb.append("UPDATE prq_cdr SET cd_state=2 ");
		}
		sb.append("WHERE cd_state=0 ");
		sb.append("and cd_date=? ");
		sb.append("and cd_id=? ");
		sb.append("and cd_port=? ");
		sb.append("and cd_callerid=?; ");			
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, str[0]);
			dao.pstmt().setString(2, str[1]);
			dao.pstmt().setString(3, str[2]);
			dao.pstmt().setString(4, str[3]);
			/* 조회한 콜로그의 일 발송량 갱신 */
			
			
			dao.pstmt().executeUpdate();
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
		 *
		 * @author Taebu Moon <mtaebu@gmail.com>
		 * @param string $table 게시판 테이블
		 * @param string $id 게시물번호
		 * @return array
		 */
	 /**
	  *  set_hist
	  * @param historytable
	  * @return void
	  */
		private static void set_hist(String historytable)
	    {
			StringBuilder sb = new StringBuilder();

			MyDataObject dao = new MyDataObject();

			sb.append("insert into "+historytable+" select * from prq_cdr where cd_state in (1,2,3,4)");// 처리가
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
					
			sb.append("delete from prq_cdr where cd_state in (1,2,3,4)");// 처리가 진행중인 것은 지우지 않는다. 
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
		private static void set_happycall(String mb_hp,String st_no) throws MySQLIntegrityConstraintViolationException
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
			String[] s = new String[2]; 
			s[0]="-1";
			s[1]="-1";
			
			StringBuilder sb = new StringBuilder();
			MyDataObject dao = new MyDataObject();
			StringBuilder sb2 = new StringBuilder();
			MyDataObject dao2 = new MyDataObject();
			long unixtime=0L;
			String hc_no="";
			String hc_status="1";
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
					hc_no=dao.rs().getString("hc_no");
					s[0]=dao.rs().getString("hc_hp");
					s[1]=dao.rs().getString("st_no");
					
					sb2.append("update prq_happycall_log set ");
					sb2.append("hc_status=? ");
					sb2.append("where hc_no=?;");
					dao2.openPstmt(sb2.toString());
					dao2.pstmt().setString(1, hc_status);
					dao2.pstmt().setString(2, hc_no);
					dao2.pstmt().executeUpdate();
				
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
				dao2.closePstmt();
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
			//$curl=$controller->curl->simple_post('http://prq.co.kr/prq/set_gcm.php', $config, array(CURLOPT_BUFFERSIZE => 10)); 
			//echo $curl;
			
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
				out.println(query);
				out.close();*/
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
}
