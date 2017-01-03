package kr.co.prq.prq_cdr;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mysql.jdbc.Connection;

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
		String last_cdr="";
		String chk_limit_date="";
		
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

		int cd_state=0;					
		int cd_day_cnt=0;				
		int cd_day_limit=0;				
		int cd_device_day_cnt=0;		
		int day_cnt=0;
		int mno_device_daily=0;
		int mn_mms_limit=0;
		int mn_dup_limit=0;
		int chk_cd_date=0;
						
		int eventcnt = 0;
		
		String[] mno_limit = new String[2];
		
		boolean is_hp = false;
		boolean chk_mms = true;

		/* 멤버 정보 */
		String[] member_info		= new String[75];
		/* 상점 정보 */
		String[] store_info			= new String[10];
		/* 포인트 이벤트 정보 */
		String[] point_event_info	= new String[7];
		/* 유저 이벤트 정보 */
		String[] user_event_info	= new String[3];
		/* 콜로그 데이터 */
		String[] cdr_info	= new String[6];
		/* config 데이터 */
		String[] config	= new String[6];
		
		if (con != null) {
			MyDataObject dao = new MyDataObject();
			MyDataObject dao2 = new MyDataObject();
			MyDataObject dao3 = new MyDataObject();
			MyDataObject dao4 = new MyDataObject();
			MyDataObject dao5 = new MyDataObject();

			StringBuilder sb = new StringBuilder();
			StringBuilder sb_log = new StringBuilder();

			sb.append("select * from prq_cdr  ");
			sb.append("WHERE cd_state=0 ");
			sb.append("AND cd_callerid LIKE '01%';");
			
			try {
				dao.openPstmt(sb.toString());

				dao.setRs(dao.pstmt().executeQuery());

				if (dao.rs().next()) 
				{
					
					PRQ_CDR.heart_beat = 1;
					StringBuilder sb2 = new StringBuilder();
					StringBuilder sb5 = new StringBuilder();
					String hist_table = DBConn.isExistTableYYYYMM();
					int resultCnt2 = 0;
					
					/*****************************************************
					* 2-2. 리스트 출력 
					* SELECT TIMESTAMPDIFF(DAY,'2009-05-18','2009-07-29');
					***************************************************/
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
					last_cdr=get_last_cdr(cd_date,cd_tel,cd_hp,cd_callerid);
					
					/*******************************************************************************
					* 4. get_mno_limit
					* - 중복 발송일 수 조회 기본값은 0인데  
					* - 값이 mn_dup_limit 만약 3이라면, 
					* - 마지막 콜로그와 대조해 보아서 
					* - 3일 동안 보내지 않습니다.  
					* - NEW] mn_limit_
					* return array[0]
					* array[1]
					********************************************************************************/						
					mno_limit=get_mno_limit(cd_id);


					/********************************************************************************
					* 5. array get_send_cnt
					* - 이번달 발송 수 조회 
					********************************************************************************/
					day_cnt=get_send_cnt(cd_hp);
					
					/********************************************************************************
					* 6-1. array get_mms_daily
					* - mms_daily 정보 가져 오기
					********************************************************************************/
					mno_device_daily=get_mms_daily(cd_hp);

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
					 */
					cdr_info[0]=cd_date;
					cdr_info[1]=cd_tel;
					cdr_info[2]=cd_hp;
					cdr_info[3]=mno_limit[0];
					cdr_info[4]=mno_limit[1];
					cdr_info[5]=Integer.toString(day_cnt);
					     
					set_cdr(cdr_info);
					
					chk_cd_date=Integer.parseInt(last_cdr);
					
					if(cd_date=="first_sent"){
					chk_limit_date="처음 보냄";
					}else{
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
					
					/* 콜로그가 KT 장비 인 경우*/
					if(!store_info[0].equals(null))
					{
						/**
						 * store_info[0]=st_no;
						 * store_info[1]=st_name;
						 * store_info[2]=st_mno;
						 * store_info[3]=st_tel_1;
						 * store_info[4]=st_hp_1;
						 * store_info[5]=st_thumb_paper;
						 * store_info[6]=st_top_msg;
						 * store_info[7]=st_middle_msg;
						 * store_info[8]=st_bottom_msg;
						 * store_info[9]=st_modoo_url;
						 * */
						
						st_hp=store_info[4];
						if(cd_port.equals("0"))
						{
							/********************************************************************************
							* 8. void set_cdr_kt 
							* - KT_CID 포트 구분이 없다.
							* - PRQ_CID 역시 포트 구분이 없다.
							* - 개발 당시 한번의 핸드폰 건만 하루 전송하고 이외에 콜은 인정하지 않는다.
							* - cdr kt 세팅
							********************************************************************************/
							cdr_info[0]=cd_date;
							cdr_info[1]=cd_callerid;
							cdr_info[2]=cd_calledid;
							cdr_info[3]=store_info[1];
							cdr_info[4]=store_info[3];
							cdr_info[5]=store_info[4];
							//페이지네이션 기본 설정
							
							set_cdr_kt(cdr_info);
							//$li->cd_hp=$st->st_hp_1;
							cd_hp=store_info[4];
						}/* if($li->cd_port=="0"){...} */
						

						/*mms 발송 여부*/
						chk_mms=true;
						
						StringBuilder msg= new StringBuilder();
						msg.append(st_top_msg);
						if(st_mno.equals("LG")){
							//$msg[]=str_replace(array("\r\n", "\r",'<br />','<br>'), '\n', $st->st_middle_msg);
							msg.append(st_middle_msg);
						}else if(st_mno.equals("KT")){
							msg.append(st_middle_msg.replaceAll("(\\r|\\n)","<br>"));		
						}else if(st_mno.equals("SK")){
							// SK 는 아무것도 하지 않는다
							msg.append(st_middle_msg);
						}
						//$mms_title=strlen($st->st_top_msg)>3?$st->st_top_msg:"web";
						mms_title=st_top_msg;
						msg.append(st_bottom_msg);
						msg.append(st_modoo_url);
						/*
						$param=ARRAY();
						$param['url']="http://prq.co.kr/prq/set_gcm.php";
						$param['return_type']='';
						*/
						if(st_mno.equals("LG")){
						msg=join("\r\n",$msg);
						}else if(st_mno.equals("KT")){
						msg=join("<br>",$msg);
						}else if(st_mno.equals("SK")){
						msg=join("\r\n",$msg);
						}

						
						msg=str_replace("#{homepage}","http://prq.co.kr/prq/page/".st_no,$msg);
						msg=str_replace("#{st_tel}",phone_format(st_tel_1),$msg);
						//echo $msg;

						/********************************************************************************
						* 9. void set_gcm_log
						* - gcm 로그에 따라 prq DB에 gcm_log 발생
						*
						********************************************************************************/
						img_url="http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
						//수신거부 여부 체크
						if(in_array(cd_callerid,black_arr))
						{
							/*gcm 로그 발생*/
							result_msg= "수신거부";
							gc_ipaddr='123.142.52.91';
							sql=array();

							if(cd_port.equals("0"))
							{
								$li->cd_hp=$st->st_hp_1;
							}
							$sql[]="INSERT INTO `prq_gcm_log` SET ";
							$sql[]="gc_subject='".$mms_title."',";
							$sql[]="gc_content='".$msg."',";
							$sql[]="gc_ismms='true',";
							$sql[]="gc_receiver='".$li->cd_callerid."',";
							$sql[]="gc_sender='".$li->cd_hp."',";
							$sql[]="gc_imgurl='".$img_url."',";
							$sql[]="gc_result='".$result_msg."',";
							$sql[]="gc_ipaddr='".$gc_ipaddr."',";
							$sql[]="gc_stno='".$st->st_no."',";
							$sql[]="gc_datetime=now();";
							echo join("",$sql);
							mysql_query(join("",$sql));
							$chk_mms=false;
						}



						/* 일간 mms 발송건 초기값 */
						$daily_mms_cnt=0;
						/* 일간 mms 발송건 디바이스 값 */
						$daily_mms_cnt+=$mno_device_daily->mm_daily_cnt;
						/* 일간 mms 발송건 prq 값 */
						$daily_mms_cnt+=$li->cd_day_cnt;
						
						/********************************************************************************
						*
						* 9-1. if($cd_date=="first_send"){...}
						* - 처음 보낼 때 안보내지던 버그 수정
						* - $chk_mms = true;
						*********************************************************************************/
						if($cd_date=="first_sent"){
							/*gcm 로그 발생*/
							$result_msg= "처음 발송 / ".$get_mno_limit->mn_dup_limit;
							$gc_ipaddr='123.142.52.90';
							$sql=array();
							if($li->cd_port==0)
							{
								$li->cd_hp=$st->st_hp_1;
							}
							
							$chk_mms=true;
						
						/********************************************************************************
						* 9-2. void set_gcm_log
						* 중복 제한 보내면 안됨 
						* prq_gcm_log 중복제한 로그 발생
						********************************************************************************/
						}else if($get_mno_limit->mn_dup_limit>$cd_date){
							/*gcm 로그 발생*/
							/* 2016-11-22 (화)
							* https://github.com/Taebu/prq/issues/57
							* 조정흠씨 자체 개발로 인해 중복 제한 비활성화
							*/

							//$result_msg= $cd_date."/".$get_mno_limit->mn_dup_limit."일 중복 제한";
							$result_msg= $cd_date."/".$get_mno_limit->mn_dup_limit."일 발송";
							$gc_ipaddr='123.142.52.90';
							$sql=array();
							if($li->cd_port==0)
							{
								$li->cd_hp=$st->st_hp_1;
							}

							$sql[]="INSERT INTO `prq_gcm_log` SET ";
							$sql[]="gc_subject='".$mms_title."',";
							$sql[]="gc_content='".$msg."',";
							$sql[]="gc_ismms='true',";
							$sql[]="gc_receiver='".$li->cd_callerid."',";
							$sql[]="gc_sender='".$li->cd_hp."',";
							$sql[]="gc_imgurl='".$img_url."',";
							$sql[]="gc_result='".$result_msg."',";
							$sql[]="gc_ipaddr='".$gc_ipaddr."',";
							$sql[]="gc_stno='".$st->st_no."',";
							$sql[]="gc_datetime=now();";
							mysql_query(join("",$sql));
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
						}else if($daily_mms_cnt>$get_mno_limit->mn_mms_limit){
							/*gcm 로그 발생*/
							$result_msg= $li->cd_day_cnt."/".$get_mno_limit->mn_mms_limit."건 제한";
							$gc_ipaddr='123.142.52.90';
							$sql=array();
							if($li->cd_port==0)
							{
								$li->cd_hp=$st->st_hp_1;
							}

							$sql[]="INSERT INTO `prq_gcm_log` SET ";
							$sql[]="gc_subject='".$mms_title."',";
							$sql[]="gc_content='".$msg."',";
							$sql[]="gc_ismms='true',";
							$sql[]="gc_receiver='".$li->cd_callerid."',";
							$sql[]="gc_sender='".$li->cd_hp."',";
							$sql[]="gc_imgurl='".$img_url."',";
							$sql[]="gc_result='".$result_msg."',";
							$sql[]="gc_ipaddr='".$gc_ipaddr."',";
							$sql[]="gc_stno='".$st->st_no."',";
							$sql[]="gc_datetime=now();";
							mysql_query(join("",$sql));
							$chk_mms=false;
						}		


						
						/********************************************************************************
						*
						* 9-4. curl->simple_post('http://prq.co.kr/prq/set_gcm.php')
						* - 수신거부 중복, 150건 제한 혹은 설정한 일수 제한 아닌 경우만
						* - $chk_mms = true;
						*********************************************************************************/
						if(chk_mms)
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
								'mode'=>'crontab'
							);
							*/
							config[0]="true";
							config[1]=msg.toString();
							config[2]=st_no;
							config[3]=mms_title;
							config[4]=cd_callerid;
							config[5]=cd_hp;
							config[6]="http://prq.co.kr/prq/uploads/TH/"+st_thumb_paper;
							config[7]="crontab";
							
							$curl=$controller->curl->simple_post('http://prq.co.kr/prq/set_gcm.php', $config, array(CURLOPT_BUFFERSIZE => 10)); 
							echo $curl;
							

							HttpURLConnection con = (HttpURLConnection) new URL("https://www.example.com").openConnection();
							con.setRequestMethod("POST");
							con.getOutputStream().write("LOGIN".getBytes("UTF-8"));
							con.getInputStream();
						}
						*/
						/*if($chk_mms){...}*/
					//}/* foreach($store as $st){...}*/

						
				}/* if (dao.rs().next()){...} */
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS037";
				e.printStackTrace();
			}
			catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS038";
				Utils.getLogger().warning(Utils.stack(e));
			}
			finally {
				dao.closePstmt();
				dao2.closePstmt();
				dao3.closePstmt();
				dao4.closePstmt();
				dao5.closePstmt();
			}
		
		}
	}

	
	/**
	 * cdr 에 추가한다.
	 * @param String status_cd	콜로그 상태 코드
	 * @param String conn_sdt	콜로그 시작시간
	 * @param String conn_edt	콜로그 종료시간
	 * @param String service_sdt	콜로그 제공시간
	 * @param String safen	
	 * @param String safen_in
	 * @param String safen_out
	 * @param String calllog_rec_file
	 * @return
	 */
	public static int set_cdr(String status_cd, 
		String conn_sdt, String conn_edt,String service_sdt,
		String safen,String safen_in,String safen_out,
		String calllog_rec_file) 
	{
		boolean retVal = false;
		int last_id = 0;
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		sb.append("INSERT INTO `asteriskcdrdb`.`cdr` SET ");
		sb.append("calldate=?,");
		sb.append("src=?,");
		sb.append("dst=?,");
		sb.append("duration=?,");
		sb.append("billsec=?,");
		sb.append("accountcode=?,");
		sb.append("uniqueid=?,");
		sb.append("userfield=?;");
		//Utils.getLogger().warning(sb.toString());


		/*
		sb.append("insert into cashq.site_push_log set "
				+ "stype='SMS', biz_code='ANP', caller=?, called=?, wr_subject=?, regdate=now(), result=''");
		*/
		try {
			dao.openPstmt(sb.toString());

			dao.pstmt().setString(1, dao.rs().getString("conn_sdt"));
			dao.pstmt().setString(2, dao.rs().getString("safen_in"));
			dao.pstmt().setString(3, dao.rs().getString("safen"));
			dao.pstmt().setString(4, dao.rs().getString("conn_sec"));
			dao.pstmt().setString(5, dao.rs().getString("service_sec"));
			dao.pstmt().setString(6, dao.rs().getString("safen_out"));
			dao.pstmt().setString(7, dao.rs().getString("unique_id"));
			dao.pstmt().setString(8, dao.rs().getString("rec_file_cd"));

			dao.pstmt().executeUpdate();
			retVal = true;
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS060";
			/* grant로 해당 사용자에 대한 권한을 주어 문제 해결이 가능하다.
			grant all privileges on cashq.site_push_log to sktl@"%" identified by 'sktl@9495';
			grant all privileges on cashq.site_push_log to sktl@"localhost" identified by 'sktl@9495';
			 */
		} catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS061";
		} finally {
			dao.closePstmt();
		}
		//return retVal;
		return last_id;
	}

	/**
	 * 콜 리스트 가져오기
	 * getCdr()
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @return ArrayList<HashMap<String, String>> list
	 */
    public static ArrayList<HashMap<String, String>> getCdr() {
		ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
		String sql="";
					
		MyDataObject dao = new MyDataObject();
		MyDataObject dao2 = new MyDataObject();
		MyDataObject dao3 = new MyDataObject();

		sql="select * from prq_cdr  "+
		"WHERE cd_state=0 "+
		"AND cd_callerid LIKE '01%';";

		try {
			dao.openPstmt(sql);
			//dao.pstmt().setString(1, mb_hp);
			dao.setRs(dao.pstmt().executeQuery());
			while (dao.rs().next()) 
			{
				HashMap<String,String> map = new HashMap<String,String>();
				map.put("cd_date",dao.rs().getString("cd_date"));
				map.put("cd_id",dao.rs().getString("cd_id"));
				map.put("cd_port",dao.rs().getString("cd_port"));
				map.put("cd_callerid",dao.rs().getString("cd_callerid"));
				map.put("cd_calledid",dao.rs().getString("cd_calledid"));
				map.put("cd_state",dao.rs().getString("cd_state"));
				map.put("cd_name",dao.rs().getString("cd_name"));
				map.put("cd_tel",dao.rs().getString("cd_tel"));
				map.put("cd_hp",dao.rs().getString("cd_hp"));
				map.put("cd_day_cnt",dao.rs().getString("cd_day_cnt"));
				map.put("cd_day_limit",dao.rs().getString("cd_day_limit"));
				map.put("cd_device_day_cnt", dao.rs().getString("cd_device_day_cnt"));
				list.add(map);
			}
			
			/*처리한 번호 핸드폰 발송 처리 */
			sql="UPDATE prq_cdr SET cd_state=1 "+
			"WHERE cd_state=0 "+
			"and cd_callerid like '01%';";
			dao2.openPstmt(sql);
			dao2.pstmt().executeUpdate();

			/*처리한 번호 일반 번호  미발송  처리 cd_state=2 */
			sql="UPDATE prq_cdr SET cd_state=2 "+
			"WHERE cd_state=0 "+
			"and cd_callerid not like '01%';";
			dao3.openPstmt(sql);
			dao3.pstmt().executeUpdate();
			

		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
		}
		finally {
			dao.closePstmt();
			dao2.closePstmt();
			dao3.closePstmt();
		}

		return list;
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
		String retVal = "";
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
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
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
			if(dao.rs().wasNull()){
				s[0]="0";
				s[1]="150";				
			}else if (dao.rs().next()) {
				s[0]=dao.rs().getString("mn_mms_limit");
				s[1]=dao.rs().getString("mn_dup_limit");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
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
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
		}
		finally {
			dao.closePstmt();
		}
		return retVal;
	}
	
	/**
	 * get_mms_daily
	 * mms 디바이스 발송 갯수 가져오기
	 * @author Taebu  Moon <mtaebu@gmail.com>
	 * @param st_hp 상점 핸드폰 번호
	 * @return int
	 */
	private static int get_mms_daily(String st_hp)
    {
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT ");
		sb.append(" mm_daily_cnt ");
		sb.append(" FROM ");
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
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
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
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
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
			if(dao.rs().wasNull()){
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
			}else if (dao.rs().next()) {
				s[0]=dao.rs().getString("st_no");
				s[1]=dao.rs().getString("st_name");
				s[2]=dao.rs().getString("st_mno");
				s[3]=dao.rs().getString("st_tel_1");
				s[4]=dao.rs().getString("st_hp_1");
				s[5]=dao.rs().getString("st_thumb_paper");
				s[6]=dao.rs().getString("st_top_msg");
				s[7]=dao.rs().getString("st_middle_msg");
				s[8]=dao.rs().getString("st_bottom_msg");
				s[9]=dao.rs().getString("st_modoo_url ");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
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
			if(dao.rs().wasNull()){
				s[0]="0";
				s[1]="150";				
			}else if (dao.rs().next()) {
				s[0]=dao.rs().getString("st_no");
				s[1]=dao.rs().getString("st_name");
				s[2]=dao.rs().getString("st_mno");
				s[3]=dao.rs().getString("st_tel_1");
				s[4]=dao.rs().getString("st_hp_1");
				s[5]=dao.rs().getString("st_thumb_paper");
				s[6]=dao.rs().getString("st_top_msg");
				s[7]=dao.rs().getString("st_middle_msg");
				s[8]=dao.rs().getString("st_bottom_msg");
				s[9]=dao.rs().getString("st_modoo_url ");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
		}
		finally {
			dao.closePstmt();
		}
		return s;
 	}
 }
