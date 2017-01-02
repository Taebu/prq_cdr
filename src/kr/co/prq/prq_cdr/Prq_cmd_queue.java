package kr.co.prq.prq_cdr;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

//import com.nostech.safen.SafeNo;

/**

 * safen_cmd_queue 테이블 관련 객체
 * dtlog
 * @author Taebu
 * 2017-01-02 (목) 오전 10:42
 *  
 */
public class Prq_cmd_queue {
	
	/**
	 * safen_cmd_queue 테이블의 데이터를 처리하기 위한 주요한 처리를 수행한다.
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

		/* 멤버 정보 */
		String[] member_info		= new String[75];
		/* 상점 정보 */
		String[] store_info			= new String[41];
		/* 포인트 이벤트 정보 */
		String[] point_event_info	= new String[7];
		/* 유저 이벤트 정보 */
		String[] user_event_info	= new String[3];
		/* 콜로그 데이터 */
		String[] cdr_info	= new String[6];
		/* config 데이터 */
		String[] config	= new String[2];
		
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

				if (dao.rs().next()) {
					
					PRQ_CDR.heart_beat = 1;
					Boolean chk_seq=dao.rs().getInt("seq")>0;

					if (chk_seq) {
						StringBuilder sb2 = new StringBuilder();
						StringBuilder sb5 = new StringBuilder();
						String hist_table = DBConn.isExistTableYYYYMM();
						int resultCnt2 = 0;
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
						}
					}
				}
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
	 * 취소시는 safen_in010에 "1234567890"을 넣어야 함. 리턴코드4자리에 따른 의미
	 * 
	 * 0000:성공 처리(인증서버에서 요청 처리가 성공.) E101:Network 장애(인증서버와 연결 실패.) E102:System
	 * 장애(인증서버의 일시적 장애. 재시도 요망.) E201:제휴사 인증 실패(유효한 제휴사 코드가 아님.) E202:유효 기간
	 * 만료(제휴사와의 계약기간 만료.) E301:안심 번호 소진(유효한 안심번호 자원이 없음.) E401:Data Not
	 * Found(요청한 Data와 일치하는 Data가 없음.) E402:Data Overlap(요청한 Data가 이미 존재함.)
	 * E501:전문 오류(전문 공통부 혹은 본문의 Data가 비정상일 경우.) E502:전화 번호(오류 요청한 착신번호가 맵핑불가 번호일
	 * 경우.)
	 */
	private static String doMapping(int seq, String safen0504,
			String safen_in010) {

		String corpCode = Env.getInstance().CORP_CODE;
		String safeNum = null;
		String telNum1 = null;// "1234567890";
		String newNum1 = null;
		String telNum2 = null;
		String newNum2 = null;

		int mapping_option = 0;
		if (Env.NULL_TEL_NUMBER.equals(safen_in010)) {
			// 취소
			mapping_option = 2;

			String safen_in = getSafenInBySafen(safen0504);

			safeNum = safen0504;
			telNum1 = safen_in;
			newNum1 = Env.NULL_TEL_NUMBER;// "1234567890";;
			telNum2 = safen_in;
			newNum2 = Env.NULL_TEL_NUMBER;
		} else {
			// 등록 Create
			mapping_option = 1;
			safeNum = safen0504;
			telNum1 = Env.NULL_TEL_NUMBER;// "1234567890";
			newNum1 = safen_in010;
			telNum2 = Env.NULL_TEL_NUMBER;
			newNum2 = safen_in010;
		}

		// String groupCode = "anpr_1";
		String groupCode = "grp_1";
		
		groupCode = Safen_master.getGroupCode(safen0504);

		String reserved1 = "";
		String reserved2 = "";
		String retCode = "";

		//SafeNo safeNo = new SafeNo();

		try {
			update_cmd_queue(seq, safen0504, safen_in010, mapping_option, "");
			//retCode = safeNo.SafeNoMod(corpCode, safeNum, telNum1, newNum1,telNum2, newNum2, groupCode, reserved1, reserved2);
		} catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS033";
		}

		// 후처리
		if ("0000".equals(retCode)) {
			Safen_master.update_safen_master(safen0504, safen_in010,
					mapping_option);

			Env.confirmSafen = safen0504;
			Env.confirmSafen_in = safen_in010;// 취소인경우는 1234567890 임

		}
		update_cmd_queue(seq, safen0504, safen_in010, mapping_option, retCode);

		return retCode;
	}

	/**
	 * 안심번호테이블을 갱신한다. 단, 이때 retCode가 공백이면 status_cd를 i로 넣고 진행중으로만 마킹하고 프로세스를
	 * 종료한다. retCode가 "0000"(성공)인경우에는 status_cd값을 "s"로 그렇지 않은 경우에는 "e"로 셋팅한 후 큐를
	 * 지우고 로그로 보낸다. 
	 * @param safen0504
	 * @param safen_in010
	 * @param mapping_option
	 * @param retCode
	 */
	private static void update_cmd_queue(int seq, String safen0504,
			String safen_in010, int mapping_option, String retCode) {

		MyDataObject dao = new MyDataObject();
		MyDataObject dao2 = new MyDataObject();
		MyDataObject dao3 = new MyDataObject();
		
		try {
			if ("".equals(retCode)) {
				StringBuilder sb = new StringBuilder();
				sb.append("update safen_cmd_queue set status_cd=? where seq=?");

				// status_cd 컬럼을 "i"<진행중>상태로 바꾼다.
				dao.openPstmt(sb.toString());

				dao.pstmt().setString(1, "i");
				dao.pstmt().setInt(2, seq);

				int cnt = dao.pstmt().executeUpdate();
				if(cnt!=1) {
					Utils.getLogger().warning(dao.getWarning(cnt,1));
					DBConn.latest_warning = "ErrPOS034";
				}

				dao.tryClose();

			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("update safen_cmd_queue set status_cd=?,result_cd=? where seq=?");

				if ("0000".equals(retCode)) {
					// status_cd 컬럼을 "s"<성공>상태로 바꾼다.
					
					dao2.openPstmt(sb.toString());

					dao2.pstmt().setString(1, "s");
					dao2.pstmt().setString(2, retCode);
					dao2.pstmt().setInt(3, seq);

					int cnt = dao2.pstmt().executeUpdate();
					if(cnt!=1) {
						Utils.getLogger().warning(dao2.getWarning(cnt,1));
						DBConn.latest_warning = "ErrPOS035";
					}

					dao2.tryClose();
				} else {
					// status_cd 컬럼을 "e"<오류>상태로 바꾼다.
					dao3.openPstmt(sb.toString());

					dao3.pstmt().setString(1, "e");
					dao3.pstmt().setString(2, retCode);
					dao3.pstmt().setInt(3, seq);

					int cnt = dao3.pstmt().executeUpdate();
					if(cnt!=1) {
						Utils.getLogger().warning(dao3.getWarning(cnt,1));
						DBConn.latest_warning = "ErrPOS036";
					}					
					dao3.tryClose();
				}
			}
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
		}
	}

	/**
	 * 마스터 테이블에서 안심번호에 따른 착신번호를 리턴한다.
	 * @param safen0504
	 * @return
	 */
	private static String getSafenInBySafen(String safen0504) {
		String retVal = "";
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("select safen_in from safen_master where safen = ?");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, safen0504);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				retVal = dao.rs().getString("safen_in");
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
	 * TB_CALL_LOG에 추가한다.
	 * @param String status_cd	콜로그 상태 코드
	 * @param String conn_sdt	콜로그 시작시간
	 * @param String conn_edt	콜로그 종료시간
	 * @param String service_sdt	콜로그 제공시간
	 * @param String safen	안심번호
	 * @param String safen_in
	 * @param String safen_out
	 * @param String calllog_rec_file
	 * @return
	 */
	public static int set_TB_CALL_LOG(String status_cd, 
		String conn_sdt, String conn_edt,String service_sdt,
		String safen,String safen_in,String safen_out,
		String calllog_rec_file) 
	{
		boolean retVal = false;
		int last_id = 0;
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		MyDataObject dao2 = new MyDataObject();
		/*
		Table: TB_CALL_LOG
		Create Table: CREATE TABLE `TB_CALL_LOG` (
		  `seq` int(11) NOT NULL AUTO_INCREMENT,
		  `SVC_ID` varchar(4) DEFAULT NULL,
		  `START_DT` datetime DEFAULT NULL,
		  `END_DT` datetime DEFAULT NULL,
		  `CALLED_HANGUP_DT` datetime DEFAULT NULL,
		  `CALLER_NUM` varchar(16) DEFAULT NULL,
		  `CALLED_NUM` varchar(16) DEFAULT NULL,
		  `VIRTUAL_NUM` varchar(16) DEFAULT NULL,
		  `REASON_CD` varchar(16) DEFAULT NULL,
		  `REG_DT` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
		  `userfield` varchar(255) DEFAULT NULL,
		  `biz_code` varchar(20) DEFAULT NULL,
		  `po_status` enum('0','1','2','3','4','5','6','99') NOT NULL DEFAULT '0',
		  PRIMARY KEY (`seq`)
		) ENGINE=MyISAM AUTO_INCREMENT=3143353 DEFAULT CHARSET=utf8
		1 row in set (0.00 sec)

		ERROR:
		*/

		sb.append("INSERT INTO `cashq`.`TB_CALL_LOG` SET ");
		sb.append("SVC_ID='81',");
		sb.append("START_DT=?,");
		sb.append("END_DT=?,");
		sb.append("CALLED_HANGUP_DT=?,");
		sb.append("VIRTUAL_NUM=?,");
		sb.append("CALLED_NUM=?,");
		sb.append("CALLER_NUM=?,");
		sb.append("userfield=?,");
		sb.append("REASON_CD=?");

		/*
		sb.append("insert into cashq.site_push_log set "
				+ "stype='SMS', biz_code='ANP', caller=?, called=?, wr_subject=?, regdate=now(), result=''");
		*/
		try {
			dao.openPstmt(sb.toString());

			//Utils.getLogger().warning(sb.toString());

			if ("1".equals(status_cd)) {
			/* GCM LOG 발생*/
			set_stgcm(safen, safen_in);

			/* 통화성공 */
			dao.pstmt().setString(1, conn_sdt);
			dao.pstmt().setString(2, conn_edt);
			dao.pstmt().setString(3, service_sdt);
			dao.pstmt().setString(4, safen);
			dao.pstmt().setString(5, safen_in);
			dao.pstmt().setString(6, safen_out);
			dao.pstmt().setString(7, calllog_rec_file);
			dao.pstmt().setString(8, status_cd);
			}else{
			/* 통화실패*/
			dao.pstmt().setString(1, conn_sdt);
			dao.pstmt().setString(2, conn_edt);
			dao.pstmt().setString(3, "1970-01-01 09:00:00");
			dao.pstmt().setString(4, safen);
			dao.pstmt().setString(5, safen_in);
			dao.pstmt().setString(6, safen_out);
			dao.pstmt().setString(7, calllog_rec_file);
			dao.pstmt().setString(8, status_cd);
			}

			dao.pstmt().executeUpdate();


			sb2.append("select LAST_INSERT_ID() last_id;");
			dao2.openPstmt(sb2.toString());
			dao2.setRs(dao2.pstmt().executeQuery());
			
			if (dao2.rs().next()) {
				last_id = dao2.rs().getInt("last_id");
			}
			
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
			dao2.closePstmt();
		}

		return last_id;
	}


	/**
	 * 0507_point에 추가한다.
	* @param  mb_hp, 
	* @param  store_name, 
	* @param  hangup_time,
	* @param  biz_code,
	* @param  call_hangup_dt,
	* @param  pev_st_dt,
	* @param  pev_ed_dt,
	* @param  eventcode,
	* @param  mb_id,
	* @param  certi_code,
	* @param  st_dt,
	* @param  ed_dt,
	* @param  store_seq,
	* @param  tcl_seq,
	* @param  moddate,
	* @param  accdate,
	* @param  ed_type,
	* @param  type
	* @param  tel
	* @param  pre_pay
	* @param  pt_stat
	 * @return void
	 */
	public static void set_0507_point(
		String mb_hp, 
		String store_name, 
		String hangup_time,
		String biz_code,
		String call_hangup_dt,
		String pev_st_dt,
		String pev_ed_dt,
		String eventcode,
		String mb_id,
		String certi_code,
		String st_dt,
		String ed_dt,
		String store_seq,
		String tcl_seq,
		String moddate,
		String accdate,
		String ed_type,
		String type,
		String tel,
		String pre_pay,
		String pt_stat
	) 
	{

		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		sb.append("INSERT INTO `cashq`.`0507_point` SET ");
		sb.append("mb_hp=?,");
		sb.append("store_name=?,");
		sb.append("point='2000',");
		sb.append("hangup_time=?,");
		sb.append("biz_code=?,");
		sb.append("call_hangup_dt=?,");
		sb.append("ev_st_dt=?,");
		sb.append("ev_ed_dt=?,");
		sb.append("eventcode=?,");
		sb.append("mb_id=?,");
		sb.append("certi_code=?,");
		sb.append("insdate=now(),");
		sb.append("st_dt=?,");
		sb.append("ed_dt=?,");
		sb.append("tcl_seq=?,");
		sb.append("store_seq=?,");
		sb.append("moddate=?,");
		sb.append("accdate=?, ");
		sb.append("ed_type=?, ");
		sb.append("type=?, ");
		sb.append("tel=?, ");
		sb.append("pre_pay=?, ");
		sb.append("pt_stat=? ");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, mb_hp);
			dao.pstmt().setString(2, store_name);
			dao.pstmt().setString(3, hangup_time);
			dao.pstmt().setString(4, biz_code);
			dao.pstmt().setString(5, call_hangup_dt);
			dao.pstmt().setString(6, pev_st_dt);
			dao.pstmt().setString(7, pev_ed_dt);
			dao.pstmt().setString(8, eventcode);
			dao.pstmt().setString(9, mb_id);
			dao.pstmt().setString(10, certi_code);
			dao.pstmt().setString(11, st_dt);
			dao.pstmt().setString(12, ed_dt);
			dao.pstmt().setString(13, tcl_seq);
			dao.pstmt().setString(14, store_seq);
			dao.pstmt().setString(15, moddate);
			dao.pstmt().setString(16, accdate);
			dao.pstmt().setString(17, ed_type);
			dao.pstmt().setString(18, type);
			dao.pstmt().setString(19, tel);
			dao.pstmt().setString(20, pre_pay);
			dao.pstmt().setString(21, pt_stat);

			//dao.pstmt().executeQuery();
			dao.pstmt().executeUpdate();
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
	 * 상점 콜로그로 갱신한다.  retCode가 "0000"(성공)인경우에는 status_cd값을 "s"로 그렇지 않은 경우에는 "e"로 셋팅한 후 큐를
	 * 지우고 로그로 보낸다. 
	 * @param safen_in
	 * @param retCode
	 */
	private static void update_stcall(String safen) {

		MyDataObject dao = new MyDataObject();
		
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE `cashq`.`store` SET callcnt=callcnt+1 WHERE tel=?");

			// status_cd 컬럼을 "i"<진행중>상태로 바꾼다.
			dao.openPstmt(sb.toString());

			dao.pstmt().setString(1, safen);

			int cnt = dao.pstmt().executeUpdate();

			if(cnt!=1) {
				Utils.getLogger().warning(dao.getWarning(cnt,1));
				DBConn.latest_warning = "ErrPOS034";
			}

			dao.tryClose();


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
		}
	}


	/**
	 * 캐시큐 상점에서 안심번호에 따른 상점 정보를 리턴한다.
	 * @param safen
	 * @return
	 */
	private static String[] getStoreInfo(String safen) {
		String[] s = new String[5];
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("select name,pre_pay,biz_code,seq,type from `cashq`.`store` where tel= ?");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, safen);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				s[0] = dao.rs().getString("name");
				s[1] = dao.rs().getString("pre_pay");
				s[2] = dao.rs().getString("biz_code");
				s[3] = dao.rs().getString("seq");
				s[4] = dao.rs().getString("type");
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
	 * 비즈코드에 따른 이벤트 코드 정보를 리턴한다.
	 * @param biz_code
	 * @return
	 */
	private static String[] getEventCodeInfo(String biz_code) {
		String[] s = new String[7];
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT ");
		sb.append("ev_st_dt,");
		sb.append("ev_ed_dt,");
		sb.append("eventcode,");
		sb.append("cash,");
		sb.append("pt_day_cnt,");
		sb.append("pt_event_cnt,");
		sb.append("ed_type ");
		sb.append("FROM `cashq`.`point_event_dt` ");
		sb.append("WHERE biz_code=? and used='1' ");
		sb.append("order by seq desc limit 1;");

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, biz_code);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				s[0] = dao.rs().getString("ev_st_dt");
				s[1] = dao.rs().getString("ev_ed_dt");
				s[2] = dao.rs().getString("eventcode");
				s[3] = dao.rs().getString("cash");
				s[4] = dao.rs().getString("pt_day_cnt");
				s[5] = dao.rs().getString("pt_event_cnt");
				s[6] = dao.rs().getString("ed_type");
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
	* is_realcode
	*/
	private static boolean is_realcode(String eventcode,String biz_code) {
		boolean is_code=false;

		String[] explode=eventcode.split("\\_");

		is_code=explode[0].equals(biz_code);
		return is_code;
	}

	/**
	* int get_eventcnt
	* @param mb_hp
	* @param eventcode
	* @return int
	*/
	private static int get_eventcnt(String mb_hp, String eventcode){
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT count(*) cnt FROM `cashq`.`0507_point` ");
		sb.append("WHERE mb_hp=? ");
		sb.append("AND eventcode=? ");
		sb.append("AND status in ('1','2','3','4');");

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, mb_hp);
			dao.pstmt().setString(2, eventcode);
			
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
	* int get_daycnt
	* @param mb_hp
	* @return int
	*/
	private static int get_daycnt(String mb_hp){
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT count(*) cnt FROM `cashq`.`0507_point` ");
		sb.append("WHERE mb_hp=? ");
//		sb.append("AND date(insdate)=date(now()) ");
		sb.append("AND date(st_dt)=date(now()) ");
		sb.append("AND status in ('1','2','3','4')");
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
	* boolean is_hp
	* @param hp
	* @return boolean
	*/
	private static boolean is_hp(String hp){
		boolean retVal=false;
			if(hp.length()>=2){
				retVal=hp.substring(0,2).equals("01");
			}
		return retVal;
	}
	
	/**
	* boolean is_freedailypt
	* @param ed_type
	* @return boolean
	*/
	private static boolean is_freedailypt(String ed_type){
		boolean retVal=false;
		if(ed_type!=null){
			if(ed_type.length()>=11){
				retVal = ed_type.substring(0,11).equals("freedailypt");
			}
		}
		return retVal;
	}


	/**
	* boolean is_freeuserpt
	* @param ed_type
	* @return boolean
	*/
	private static boolean is_freeuserpt(String ed_type){
		boolean retVal=false;
		if(ed_type!=null){
			if(ed_type.length()>=10){
				retVal = ed_type.substring(0,10).equals("freeuserpt");
			}
		}
		return retVal;
	}

	/**
	* boolean is_fivept
	* @param ed_type
	* @return boolean
	*/
	private static boolean is_fivept(String ed_type){
		boolean retVal=false;
		if(ed_type!=null){
			if(ed_type.length()>=6){
				retVal = ed_type.substring(0,6).equals("fivept");
			}else{
				retVal = ed_type.equals("");
			}
		}
		return retVal;
	}

	/**
	* int get_user_event_index
	* @param mb_hp
	* @param biz_code
	* @return int
	*/
	private static int get_user_event_index(String mb_hp,String biz_code){
		int retVal = 0;
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT count(*) cnt FROM `cashq`.`user_event_dt` ");
		sb.append("WHERE biz_code=? ");
		sb.append("and mb_hp=? ");
		sb.append("order by seq desc limit 1");
		
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, biz_code);
			dao.pstmt().setString(2, mb_hp);
			
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
	 * set_user_event_dt에 추가한다.
	 * @param String biz_code	콜로그 상태 코드
	 * @param String mb_hp	콜로그 시작시간
	 * @param String conn_edt	콜로그 종료시간
	 * @param String service_sdt	콜로그 제공시간
	 * @param String safen	안심번호
	 * @param String safen_in	링크된번호
	 * @param String safen_out	소비자 번호
	 * @param String calllog_rec_file	
	 * @return
	 */
	public static int set_user_event_dt(String biz_code, 
		String mb_hp, 
		String daily_st_dt,
		String daily_ed_dt,
		String eventcode) 
	{

		boolean retVal = false;
		int last_id = 0;
		StringBuilder sb = new StringBuilder();
		MyDataObject dao = new MyDataObject();
		sb.append("INSERT INTO `cashq`.`user_event_dt` SET ");
		sb.append("biz_code=?,");
		sb.append("mb_hp=?,");
		sb.append("ev_st_dt=?,");
		sb.append("ev_ed_dt=?,");
		sb.append("eventcode=?,");
		sb.append("insdate=now()");

		/*
		sb.append("insert into cashq.site_push_log set "
				+ "stype='SMS', biz_code='ANP', caller=?, called=?, wr_subject=?, regdate=now(), result=''");
		*/
		try {
			dao.openPstmt(sb.toString());

			dao.pstmt().setString(1, biz_code);
			dao.pstmt().setString(2, mb_hp);
			dao.pstmt().setString(3, daily_st_dt);
			dao.pstmt().setString(4, daily_ed_dt);
			dao.pstmt().setString(5, eventcode);

			//dao.pstmt().executeQuery();
			dao.pstmt().executeUpdate();
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
		return last_id;
	}

	/**
	* get_userevent(biz_code, mb_hp)
	* @param biz_code
	* @param mb_hp
	* @return array
	*/
	private static String[] get_userevent(String biz_code, String mb_hp) {
		String[] s = new String[3];
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("SELECT  ");
		sb.append("eventcode,");
		sb.append("ev_ed_dt,");
		sb.append("ev_st_dt ");
		sb.append("FROM `cashq`.`user_event_dt` ");
		sb.append("WHERE biz_code=? ");
		sb.append("AND mb_hp=? ");
		sb.append("ORDER BY seq desc limit 1;");

		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, biz_code);
			dao.pstmt().setString(2, mb_hp);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				s[0] = dao.rs().getString("ev_st_dt");
				s[1] = dao.rs().getString("ev_ed_dt");
				s[2] = dao.rs().getString("eventcode");
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
	 * app_toeken 아이디의 지역 정보를 갱신해서 넣는다.

	 * @param biz_code
	 * @param mb_hp
	 * @return void
	 */
	private static void set_app_token_id(String biz_code,String mb_hp) {

		MyDataObject dao = new MyDataObject();
		
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE `cashq`.`app_token_id` SET biz_code=? where tel=?");

			dao.openPstmt(sb.toString());

			dao.pstmt().setString(1, biz_code);
			dao.pstmt().setString(2, mb_hp);

			int cnt = dao.pstmt().executeUpdate();
			if(cnt!=1) {
				Utils.getLogger().warning(dao.getWarning(cnt,1));
				DBConn.latest_warning = "ErrPOS034";
			}

			dao.tryClose();


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
		}
	}


	/**
	 * set_stgcm 아이디의 지역 정보를 갱신해서 넣는다.
	 * set_stgcm(safen, safen_in);
	 * @param safen
	 * @param safen_in
	 * @return void
	 */
	private static void set_stgcm(String safen,String safen_in) 
	{

		MyDataObject dao = new MyDataObject();
		
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO cashq.st_gcm SET VIRTUAL_NUM=?,CALLED_NUM=?,insdate=now();");

			dao.openPstmt(sb.toString());

			dao.pstmt().setString(1, safen);
			dao.pstmt().setString(2, safen_in);

			int cnt = dao.pstmt().executeUpdate();
			if(cnt!=1) {
				Utils.getLogger().warning(dao.getWarning(cnt,1));
				DBConn.latest_warning = "ErrPOS034";
			}

			dao.tryClose();


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
		}
	}


	/**
	* boolean is_point
	* @param pre_pay
	* @return boolean
	*/
	private static boolean is_point(String pre_pay){
		boolean retVal=false;
		
		if(pre_pay!=null){
			retVal = pre_pay.equals("gl")||pre_pay.equals("sl")||pre_pay.equals("on")||pre_pay.equals("br");
		}
		
		return retVal;
	}

	/**
	* boolean is_datepoint
	* @param ev_st_dt
	* @param ev_ed_dt
	* @return boolean
	*/
	private static boolean is_datepoint(String ev_st_dt,String ev_ed_dt){
		boolean is_date=false;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try{

		/* null check 하나라도 널이면 에러 */
		if(ev_st_dt==null||ev_ed_dt==null){

		}else{
			Date todayDate = new Date();
			
			Date historyDate = sdf.parse(ev_st_dt);
			Date futureDate = sdf.parse(ev_ed_dt);

			/* 기간 이내 */
			is_date=todayDate.after(historyDate)&&todayDate.before(futureDate);
			
			/* 이벤트 종료 시간과 같은 날 */
			if(sdf.format(todayDate).equals(sdf.format(futureDate))){
				is_date=true;
			}		
		}
		
		}catch(ParseException e){
		
		}
		
		return is_date;
	}


	// yyyy-MM-dd HH:mm:ss.0 을 yyyy-MM-dd HH:mm:ss날짜로 변경
	public static String chgDatetime(String str)
	{
		String retVal="";

		try{
		String source = str; 
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date historyDate = simpleDate.parse(str);
		retVal=simpleDate.format(historyDate);
		}catch(ParseException e){
		}
		return retVal;
	}


	public static String chk_pt5(String str)
	{
		String retVal="pt5";
		String[] ed_type= new String[] {"freept","freedailypt","freeuserpt"};

		if(Arrays.asList(ed_type).contains(str)){
			retVal="free";
		}

			return retVal; 
	}

	private static String chg_userevent(String eventcode) {
		String retVal="";
		String[] explode=eventcode.split("\\_");
		int up_usercnt=Integer.parseInt(explode[1]);
		up_usercnt++;

		retVal=explode[0]+"_"+up_usercnt;
		return retVal;
	}


	/**
	 * 콜 리스트 가져오기
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @return ArrayList<HashMap<String, String>>
	 * @return list
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

	// 데이터 유효성 null 체크에 대한 값을 "" 로 리턴한다. 
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
	* get_last_cdr(cd_date, cd_tel,cd_hp,cd_callerid)
	* @param String cd_date 
	* @param String cd_tel
	* @param String cd_hp
	* @param String cd_callerid
	* @return array
	*/
	private static String get_last_cdr(String cd_date, String cd_tel,String cd_hp,String cd_callerid) {
		String retVal = "";
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		StringBuilder sb2 = new StringBuilder();

		MyDataObject dao2 = new MyDataObject();
		sb.append("SELECT  ");
		sb.append(" cd_date ");
		sb.append(" FROM ");
		sb.append(" prq_cdr ");
		sb.append("W.HERE cd_tel=? ");
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
				
				
				dao2.setRs (dao2.pstmt().executeQuery());
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
	* int get_send_cnt
	* @param mb_hp
	* @param eventcode
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
	 * mms 디바이스 발송 갯수 가져오기
	 * get_mms_daily
	 * @author Taebu Moon <mtaebu@gmail.com>
	 * @param string $st_hp 상점 핸드폰 번호
	 * @return array
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
