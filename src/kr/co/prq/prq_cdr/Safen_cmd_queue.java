package kr.co.prq.prq_cdr;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

//import com.nostech.safen.SafeNo;

/**

 * safen_cmd_queue 테이블 관련 객체
 * @author mtb
 * 2016-10-27 (목) 오전 10:42
 *  - 
 */
public class Safen_cmd_queue {
	
	/**
	 * safen_cmd_queue 테이블의 데이터를 처리하기 위한 주요한 처리를 수행한다.
	 */
	public static void doMainProcess() {
		Connection con = DBConn.getConnection();

		String ev_st_dt="";
		String ev_ed_dt="";
		String daily_st_dt="";
		String daily_ed_dt="";
		String mb_hp="";
		String eventcode="";
		String cash="";
		String ed_type="";
		String biz_code="";
		String call_hangup_dt="";
		String mb_id="";
		String certi_code="";
		String st_dt="";
		String ed_dt="";
		String moddate="1970-01-01 12:00:00";
		String accdate="1970-01-01 12:00:00";
		String str_hangup_time="";
		String tel="";
		String pt_stat="";

		int eventcnt = 0;
		int daycnt = 0;
		int pt_day_cnt = 0;
		int pt_event_cnt = 0;
		int usereventindex = 0;
		int user_event_dt_index = 0;
		int tcl_seq = 0;
		int service_sec=0;
		int hangup_time=0;

		boolean is_hp = false;
		boolean is_freedailypt = false;
		boolean is_freeuserpt = false;
		boolean is_fivept = false;

		boolean is_realcode=false;
		boolean is_userpt=false;
		boolean chk_realcode=false;
		boolean is_answer=false;

		/* 상점 정보 */
		String[] store_info			= new String[5];
		/* 포인트 이벤트 정보 */
		String[] point_event_info	= new String[7];
		/* 유저 이벤트 정보 */
		String[] user_event_info	= new String[3];
		
		String status_cd="";
		String conn_sdt="";
		String conn_edt="";
		String service_sdt="";
		String safen="";
		String safen_in="";
		String safen_out="";
		String calllog_rec_file="";

		String store_name = "";
		String pre_pay="";
		String store_seq="";
		String type="";
		String str_tcl_seq="";

		if (con != null) {
			MyDataObject dao = new MyDataObject();
			MyDataObject dao2 = new MyDataObject();
			MyDataObject dao3 = new MyDataObject();
			MyDataObject dao4 = new MyDataObject();
			MyDataObject dao5 = new MyDataObject();
;
			StringBuilder sb = new StringBuilder();
			StringBuilder sb_log = new StringBuilder();

			sb.append("select * from prq_cdr order by seq limit 1;");
			
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
						/*
						*/
						/**
						*   select conn_sdt,service_sdt,conn_edt,status_cd from prq_cdr;
							
							public static int set_TB_CALL_LOG(
							String status_cd 상태코드,
								po_status
							String conn_sdt 시작시간, 
								start_dt
							String conn_edt 종료시간,
								end_dt
							String service_sdt, 반응시간
								called_hangup_dt
							String safen 안심번호,
								virtual_num
							String safen_in 상점번호,
								called_num
							String safen_out 고객번호,
								caller_num
							String calllog_rec_file	콜로그 녹음파일(
								userfield
							int service_sec	서비스 제공시간(단위 :초)(
								userfield
								) 
						*/

						/*	String status_cd 상태코드, */
						status_cd=dao.rs().getString("status_cd")==null?"":dao.rs().getString("status_cd");
						
						/*	String conn_sdt 시작시간,  */
						conn_sdt=dao.rs().getString("conn_sdt")==null?"":dao.rs().getString("conn_sdt");
						
						/*	String conn_edt 종료시간, */
						conn_edt=dao.rs().getString("conn_edt")==null?"":dao.rs().getString("conn_edt");
						
						/*	String service_sdt, 반응시간 */
						service_sdt=dao.rs().getString("service_sdt")==null?"":dao.rs().getString("service_sdt");
						
						/*	String safen 안심번호, 050 */
						safen=dao.rs().getString("safen")==null?"":dao.rs().getString("safen");
						tel=safen;

						/*	String safen_in 상점번호, */
						safen_in=dao.rs().getString("safen_in")==null?"":dao.rs().getString("safen_in");
						
						/*	String calllog_rec_file	콜로그 녹음파일( */
						safen_out=dao.rs().getString("safen_out")==null?"":dao.rs().getString("safen_out");
						
						/*	String status_cd 상태코드, */
						calllog_rec_file=dao.rs().getString("calllog_rec_file")==null?"":dao.rs().getString("calllog_rec_file");
						
						/*	String status_cd 상태코드, */
						service_sec=dao.rs().getInt("service_sec");
						
						/*	String status_cd 상태코드, */
						call_hangup_dt=dao.rs().getString("service_sdt")==null?"":dao.rs().getString("service_sdt");
						
						/*	String status_cd 상태코드, */
						st_dt=dao.rs().getString("conn_sdt")==null?"":dao.rs().getString("conn_sdt");
						
						/*	String status_cd 상태코드, */
						ed_dt=dao.rs().getString("conn_edt")==null?"":dao.rs().getString("conn_edt");
						
						st_dt=chgDatetime(st_dt);
						ed_dt=chgDatetime(ed_dt);
						mb_hp=safen_out;
						
						is_answer = status_cd.equals("1");
						/* cashq.TB_CALL_LOG에 세팅합니다. */
						tcl_seq = set_TB_CALL_LOG(status_cd, conn_sdt,conn_edt, service_sdt, safen,safen_in,safen_out,calllog_rec_file);
						str_tcl_seq=Integer.toString(tcl_seq);
						str_hangup_time=Integer.toString(service_sec);

						/* cashq.store.callcnt 갱신*/
						update_stcall(safen);
						
						/* 4-2. Store Info 조회 */
						//store_info=getStoreInfo(safen_in);
						store_info=getStoreInfo(safen);
						/**
						* store_info[0] = store_name, 상점이름 
						* store_info[1] = pre_pay, 골드,실버,캐시큐,일반,PRQ 
						* store_info[2] = biz_code, 비즈코드 
						* store_info[3] = store_seq, 상점고유번호 
						* store_info[4] = type, 상점타입(예, 치킨,피자 등등의 코드) 
						*/
						store_name=store_info[0];
						pre_pay=store_info[1];
						biz_code=store_info[2];
						store_seq=store_info[3];
						type=store_info[4];

						/* 4-3. Event Info 조회*/
						if(biz_code!=null||biz_code!="")
						{
						point_event_info=getEventCodeInfo(biz_code);
						/**
						* point_event_info[0] = ev_st_dt, 이벤트 시작일
						* point_event_info[1] = ev_ed_dt, 이벤트 종료일
						* point_event_info[2] = eventcode, 이벤트 코드 
						* point_event_info[3] = cash, 캐시
						* point_event_info[4] = pt_day_cnt, 일정립제한 갯수
						* point_event_info[5] = pt_event_cnt, 이벤트코드 정립 제한 갯수
						* point_event_info[6] = ed_type, 이벤트코드 타입
						*/
						ev_st_dt=point_event_info[0];
						ev_ed_dt=point_event_info[1];
						eventcode=point_event_info[2]!=null?point_event_info[2]:"";
						cash=point_event_info[3]!=null?point_event_info[3]:"";
						pt_day_cnt = point_event_info[4]!=null?Integer.parseInt(point_event_info[4]):0;
						pt_event_cnt = point_event_info[5]!=null?Integer.parseInt(point_event_info[5]):0;
						ed_type=point_event_info[6];
						

						
						if(eventcode.length()>3&&biz_code.length()>3){
							chk_realcode=is_realcode(eventcode,biz_code);
						}

						/** 
						* 4-4. 이벤트 갯수 카운트
						* 4-5. 하루 포인트 한번 포인트 여부
						* 4-6. 핸드폰 번호 여부
						* 4-7. 포인트 재적립 시간 여부 단위
						*      point_event_dt, pt_event_cnt
						* 4-8. NEW freedailypt 여부 조회
						* 4-9. NEW 오늘을 기준으로 60일 코드를 가져온다.
						* 4-10. NEW freeuserpt 여부 조회
						* 4-11. NEW usereventcnt
						* 4-12. NEW
						* 4-13. NEW pt_stat
						*/


						/* 4-4 */
						eventcnt = get_eventcnt(mb_hp,eventcode);

						/* 4-5 */
						daycnt = get_daycnt(mb_hp);

						/* 4-6 */
						is_hp = is_hp(safen_out);

						/* 4-8 */
						is_freedailypt = is_freedailypt(ed_type);
						
						/* 4-10 */
						is_freeuserpt = is_freeuserpt(ed_type);
						
						/* 4-10-1 */
						is_fivept=is_fivept(ed_type);

						/* 4-11 */
						if(is_freeuserpt){
							usereventindex = get_user_event_index(mb_hp, biz_code);
						}
						
							daily_st_dt=Utils.getyyyymmdd();
							daily_ed_dt=Utils.add60day();
						/* 4-12 */
						if(is_freeuserpt&&usereventindex==0&&is_hp&&is_answer){
							/* user_event 생성하기 */
							daily_st_dt=Utils.getyyyymmdd();
							daily_ed_dt=Utils.add60day();
							eventcode=biz_code+"_1";
							user_event_dt_index = set_user_event_dt(biz_code, mb_hp, daily_st_dt,daily_ed_dt,eventcode);
						}else if(is_freeuserpt&&usereventindex>0&&is_hp&&is_answer){
							/* user_event 조회하기 */
							user_event_info = get_userevent(biz_code, mb_hp);
							/**
							* user_event_info[0]=ev_st_dt;
							* user_event_info[1]=ev_ed_dt;
							* user_event_info[2]=eventcode;
							*/
							if(is_datepoint(user_event_info[0],user_event_info[1])){
								daily_st_dt=user_event_info[0];
								daily_ed_dt=user_event_info[1];
								eventcode=user_event_info[2];
							}else{
								daily_st_dt=Utils.getyyyymmdd();
								daily_ed_dt=Utils.add60day();
								eventcode=chg_userevent(user_event_info[2]);
								user_event_dt_index = set_user_event_dt(biz_code, mb_hp, daily_st_dt,daily_ed_dt,eventcode);
							
							}
						}
						}/* if(biz_code!=null||biz_code!=""){ ... } */
						/*
						sb_log.append("mb_hp : ");
						sb_log.append(mb_hp);
						sb_log.append("store_name : ");
						sb_log.append(store_name);
						sb_log.append("str_hangup_time : ");
						sb_log.append(str_hangup_time);
						sb_log.append("biz_code : ");
						sb_log.append(biz_code);
						sb_log.append("call_hangup_dt : ");
						sb_log.append(call_hangup_dt);
						sb_log.append("ev_st_dt :");
						sb_log.append(ev_st_dt);
						sb_log.append("ev_ed_dt : ");
						sb_log.append(ev_ed_dt);
						sb_log.append("eventcode : ");
						sb_log.append(eventcode);
						sb_log.append("mb_id : ");
						sb_log.append(mb_id);
						sb_log.append("certi_code : ");
						sb_log.append(certi_code);
						sb_log.append("st_dt : ");
						sb_log.append(st_dt);
						sb_log.append("ed_dt : ");
						sb_log.append(ed_dt);
						sb_log.append("store_seq : ");
						sb_log.append(store_seq);
						sb_log.append("str_tcl_seq : ");
						sb_log.append(str_tcl_seq);
						sb_log.append("moddate : ");
						sb_log.append(moddate);
						sb_log.append("accdate : ");
						sb_log.append(accdate);
						Utils.getLogger().warning(sb_log.toString());
						*/

						/* 4-13 */
						pt_stat=chk_pt5(ed_type);

						/* fivept, freept 6. 적립조건*/
						if(is_point(pre_pay)
							&&service_sec>9
							&&is_datepoint(ev_st_dt,ev_ed_dt)
							&&daycnt==0
							&&eventcnt<pt_event_cnt
							&&is_hp
							&&is_fivept
							&&is_answer
							&&chk_realcode
						){
							/* 6-1 
								String mb_hp, 
								String store_name, 
								String hangup_time,
								String biz_code,
								String call_hangup_dt,
								String ev_st_dt,
								String ev_ed_dt,
								String eventcode,
								String mb_id,
								String certi_code,
								String st_dt,
								String ed_dt,
								String tcl_seq,
								String moddate,
								String accdate,
								String ed_type
							*/
							set_0507_point(
								mb_hp,store_name, str_hangup_time, 
								biz_code, call_hangup_dt, ev_st_dt, 
								ev_ed_dt, eventcode, mb_id, 
								certi_code, st_dt, ed_dt, 
								store_seq, str_tcl_seq, moddate, 
								accdate,ed_type,type,
								tel,pre_pay,pt_stat);
			
						}else if(is_point(pre_pay)
							&&service_sec>9
							&&is_datepoint(ev_st_dt,ev_ed_dt)
							&&daycnt==0
							&&eventcnt<pt_event_cnt
							&&is_hp
							&&is_answer
							&&is_freedailypt
							&&chk_realcode
						){
							/* freedailypt 7. 적립조건*/
							daily_st_dt=Utils.getyyyymmdd();
							daily_ed_dt=Utils.add60day();

							/* 7-1 */
							set_0507_point(
								mb_hp,store_name, str_hangup_time, 
								biz_code, call_hangup_dt, daily_st_dt,
								daily_ed_dt, eventcode, mb_id, 
								certi_code, st_dt, ed_dt, 
								store_seq, str_tcl_seq, moddate, 
								accdate,ed_type,type,
								tel,pre_pay,pt_stat);
						}else if(is_point(pre_pay)
							&&service_sec>9
							&&is_datepoint(ev_st_dt,ev_ed_dt)
							&&daycnt==0
							&&eventcnt<pt_event_cnt
							&&is_hp
							&&is_answer
							&&is_freeuserpt
							&&chk_realcode
						){
							/* freeuserpt 8. 적립조건*/
							/* 8-1 */
							set_0507_point(
								mb_hp,store_name, str_hangup_time, 
								biz_code, call_hangup_dt, daily_st_dt, 
								daily_ed_dt, eventcode, mb_id, 
								certi_code, st_dt, ed_dt, 
								store_seq, str_tcl_seq, moddate, 
								accdate,ed_type,type,
								tel,pre_pay,pt_stat);
						}


						sb2.append("insert into ");
						sb2.append(hist_table);
						/* 처리가 진행중인것은 포함하지 않는다. */
						sb2.append(" select * from prq_cdr where seq=?");
						
						// insert into safen_cmd_hist_201607 select * from
						// safen_cmd_queue where status_cd != ''
						dao2.openPstmt(sb2.toString());
						dao2.pstmt().setInt(1, dao.rs().getInt("seq"));

 						resultCnt2 = dao2.pstmt().executeUpdate();
						if(resultCnt2!=1) {
							Utils.getLogger().warning(dao2.getWarning(resultCnt2,1));
							DBConn.latest_warning = "ErrPOS027";
						}

						// region 3 start --->
						StringBuilder sb3 = new StringBuilder();

						/* 처리가 진행중인것은 지우지 않는다. */
						sb3.append("delete from prq_cdr where seq=?");
				
						// insert into safen_cmd_hist_201607 select * from
						// safen_cmd_queue where status_cd != ''
						dao3.openPstmt(sb3.toString());
						dao3.pstmt().setInt(1, dao.rs().getInt("seq"));

						int resultCnt3 = dao3.pstmt().executeUpdate();
						if(resultCnt3!=1) {
							Utils.getLogger().warning(dao3.getWarning(resultCnt3,1));
							DBConn.latest_warning = "ErrPOS028";
						}
						// region 3 end <---

						// region 4 start --->
						/*
						StringBuilder sb4 = new StringBuilder();
						
						sb4.append("select * from prq_cdr limit 1");
						dao4.openPstmt(sb4.toString());

						dao4.setRs(dao4.pstmt().executeQuery());

						if (dao4.rs().next()) {
							int seq = dao4.rs().getInt("seq");
							String safen = dao4.rs().getString("safen");
							String safen_in = dao4.rs().getString("safen_in");
							doMapping(seq, safen, safen_in);
						}
						*/
						// region 4 end <---
					} else {
						//Utils.getLogger().info("chk_seq false log");
						if (!"".equals(Env.confirmSafen)) {
							// cmq_queue에는 없는 경우라면
							//SafeNo safeNo = new SafeNo();
							String retCode = "";
							try {
								//retCode = safeNo.SafeNoAsk(Env.getInstance().CORP_CODE,Env.confirmSafen);
							} catch (Exception e) {
								Utils.getLogger().warning(e.getMessage());
								Utils.getLogger().warning(Utils.stack(e));
								DBConn.latest_warning = "ErrPOS029";
							}

							if (-1 < retCode.indexOf(Env.confirmSafen_in)) {
								/* retCode = "01040421182,01040421182" 와 같은 형태로 리턴되는 식임 */
								Utils.getLogger().info(
										"OK 착신연결성공" + Env.confirmSafen + "->"
												+ Env.confirmSafen_in);
							} else {// 취소된 경우 recCode = "E401"이 리턴됨
								if (Env.NULL_TEL_NUMBER
										.equals(Env.confirmSafen_in)
										&& "E401".equals(retCode)) {
									Utils.getLogger().info(
											"OK 착신취소성공" + Env.confirmSafen
													+ ", retCode:[" + retCode
													+ "]");
								} else {
									Utils.getLogger().warning(
											"Error! " + Env.confirmSafen + "->"
													+ Env.confirmSafen_in
													+ "? retCode:[" + retCode
													+ "]");
									DBConn.latest_warning = "ErrPOS030";
								}
							}

							Env.confirmSafen = "";
						}
					}
				}				
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS031";
				e.printStackTrace();
			} catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS032";
			}finally {
				dao.closePstmt();
				dao2.closePstmt();
				dao3.closePstmt();
				dao4.closePstmt();
				dao5.closePstmt();
			}
			
			//콜로그 마스터 정보의 레코드 1개를 갱신을 시도한다.
			//Safen_master.doWark2();
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


/*
*************************** 1. row ***************************
           seq: 945834
         mb_hp: 01077430009
         point: 2000
    store_name: 본사포인트 테스트
          type:
   hangup_time: 18
call_hangup_dt: 2016-07-22 18:13:16
      biz_code: testsub
      ev_st_dt: 2014-01-01
      ev_ed_dt: 2020-08-19
     eventcode: testsub_1
         mb_id:
    certi_code:
       insdate: 2016-07-22 18:15:20
         st_dt: 2016-07-22 18:13:05
         ed_dt: 2016-07-22 18:13:34
       tcl_seq: 3037187
     store_seq: 6797
        status: 1
       moddate: 2016-08-18 10:16:04
       accdate: 0000-00-00 00:00:00
     cashq_seq: NULL
          memo: NULL
           tel: NULL
      cnt_memo: NULL
       pre_pay: sl
       pt_stat: pt5
       ed_type: fivept
1 row in set (0.00 sec)


*/
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

}
