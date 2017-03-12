package nara_bid_information;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class OpenAPIReader implements Runnable {
	final String SERVER_KEY = Resources.SERVER_KEY;
	final String NUM_OF_ROWS = "1500";
	
	enum Type {
		PROD,
		FACIL,
		SERV,
		WEJA, 
		REWEJA
	}
	
	enum Option {
		NOTI,
		RES,
		BASE_PRICE,
		PRE_PRICE,
		PERIODIC,
		REBID,
		NEGO,
		DIFF
	}
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	String startDate;
	String endDate;
	Type type; // 물품, 공사, 용역, 외자
	Option option; // 공고, 결과, 기초금액, 예비가격
	// String re; // 재입찰 flag
	
	ProgressTracker tracker;
	int totalItem;
	boolean checkOnly;
	boolean incompleteProcess;
	boolean restarted;
	Type savedType;
	int savedItem;
	int savedIndex;
	int savedPage;
	
	public OpenAPIReader(String sd, String ed, String op, ProgressTracker pt) {
		if (sd.length() == 10) sd = sd.replaceAll("-", "");
		if (ed.length() == 10) ed = ed.replaceAll("-", "");
		startDate = sd;
		endDate = ed;
		tracker = pt;
		setOption(op);
		incompleteProcess = false;
		restarted = false;
		savedItem = 0;
		savedIndex = 0;
		savedPage = 1;
	}
	
	public static void main(String args[]) throws ClassNotFoundException, IOException, SQLException, InterruptedException {
		OpenAPIReader tester = new OpenAPIReader("20131201", "20170121", "공고", null);
		
		tester.processIncomplete();
	}
	
	public void connectDB() throws SQLException, ClassNotFoundException {
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		db_con = DriverManager.getConnection("jdbc:mysql://localhost/"+Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = db_con.createStatement();
		rs = null;
	}
	
	public void closeDB() throws SQLException {
		db_con.close();
		st.close();
	}
	
	public void setDate(String sd, String ed) {
		if (sd.length() == 10) sd = sd.replaceAll("-", "");
		if (ed.length() == 10) ed = ed.replaceAll("-", "");
		startDate = sd;
		endDate = ed;
	}
	
	public String parseDate(String d) {
		if (d.length() == 12) {
			String year = d.substring(0, 4);
			String month = d.substring(4, 6);
			String day = d.substring(6, 8);
			String hour = d.substring(8, 10);
			String min = d.substring(10);
			
			return year + "-" + month + "-" + day + " " + hour + ":" + min;
		}
		if (d.length() == 8) {
			String year = d.substring(0, 4);
			String month = d.substring(4, 6);
			String day = d.substring(6, 8);
			
			return year + "-" + month + "-" + day;
		}
		else return "";
	}
	
	public String parseNumber(String n) {
		if (n.equals("")) return "0";
		else return n;
	}
	
	public String buildPath() {
		String path = "";
		
		if (option == Option.NOTI) {
			path = EndPoints.NOTI_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_NOTI;
			else if (type == Type.FACIL) path += EndPoints.FACIL_NOTI;
			else if (type == Type.SERV) path += EndPoints.SERV_NOTI;
			else if (type == Type.WEJA) path += EndPoints.WEJA_NOTI;
			
			if (incompleteProcess) path += "inqryDiv=2&";
			else path += "inqryDiv=1&";
		}
		else if (option == Option.RES) {
			path = EndPoints.RES_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_RES;
			else if (type == Type.FACIL) path += EndPoints.FACIL_RES;
			else if (type == Type.SERV) path += EndPoints.SERV_RES;
			else if (type == Type.WEJA) path += EndPoints.WEJA_RES;
			
			path += "inqryDiv=3&";
		}
		else if (option == Option.BASE_PRICE) {
			path = EndPoints.NOTI_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_BASE_PRICE;
			else if (type == Type.FACIL) path += EndPoints.FACIL_BASE_PRICE;
			else if (type == Type.SERV) path += EndPoints.SERV_BASE_PRICE;
			else if (type == Type.WEJA) path += EndPoints.WEJA_BASE_PRICE;
			
			path += "inqryDiv=2&";
		}
		else if (option == Option.PRE_PRICE) {
			path = EndPoints.RES_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_PRE_PRICE;
			else if (type == Type.FACIL) path += EndPoints.FACIL_PRE_PRICE;
			else if (type == Type.SERV) path += EndPoints.SERV_PRE_PRICE;
			else if (type == Type.WEJA) path += EndPoints.WEJA_PRE_PRICE;
			
			path += "inqryDiv=2&";
		}
		
		path += "ServiceKey=" + SERVER_KEY;
		if (!checkOnly) path += "&numOfRows=" + NUM_OF_ROWS;
		
		return path;
	}
	
	public String buildDatePath() {
		String path = buildPath();

		path += "&inqryBgnDt=" + startDate + "0000";
		path += "&inqryEndDt=" + endDate + "2359";
		
		return path;
	}
	
	public String buildDatePath(String sd, String ed) {
		String path = buildPath();

		path += "&inqryBgnDt=" + sd;
		path += "&inqryEndDt=" + ed;
		
		return path;
	}
	
	public String buildItemPath(String item) {
		String path = "";
		
		if (option == Option.NOTI) {
			path = EndPoints.NOTI_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_NOTI;
			else if (type == Type.FACIL) path += EndPoints.FACIL_NOTI;
			else if (type == Type.SERV) path += EndPoints.SERV_NOTI;
			else if (type == Type.WEJA) path += EndPoints.WEJA_NOTI;
			
			path += "inqryDiv=2";
			path += "&ServiceKey=" + SERVER_KEY;
			path += "&bidNtceNo=" + item;
		}
		else {
			path = buildPath();
			path += "&bidNtceNo=" + item;
		}
		
		return path;
	}
	
	public String buildLicensePath(String item, String ver) {
		String path = EndPoints.NOTI_BASE_PATH;
		path += EndPoints.NOTI_LICENSE;
		path += "inqryDiv=2";
		path += "&bidNtceNo=" + item;
		path += "&bidNtceOrd=" + ver;
		path += "&ServiceKey=" + SERVER_KEY;
		path += "&numOfRows=" + NUM_OF_ROWS;
		
		return path;
	}
	
	public Document getResponse(String path) throws IOException {
		URL url = new URL(path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setReadTimeout(15000);
		
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending GET request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
		String inputLine;
		StringBuffer reader = new StringBuffer();
		
		while ( (inputLine = in.readLine()) != null ) {
			reader.append(inputLine);
		}
		in.close();
		
		return Jsoup.parse(reader.toString());
	}

	public void setType(String t) {
		switch(t) {
		case "물품":
			type = Type.PROD;
			break;
		case "공사":
			type = Type.FACIL;
			break;
		case "용역":
			type = Type.SERV;
			break;
		case "외자":
			type = Type.WEJA;
			break;
		case "외자재공고":
			type = Type.REWEJA;
			break;
		}
	}
	
	public void setOption(String o) {
		switch(o) {
		case "공고":
			option = Option.NOTI;
			break;
		case "결과":
			option = Option.RES;
			break;
		case "기초금액":
			option = Option.BASE_PRICE;
			break;
		case "예비가격":
			option = Option.PRE_PRICE;
			break;
		case "일자별":
			option = Option.PERIODIC;
			break;
		case "재입찰":
			option = Option.REBID;
			break;
		case "협상":
			option = Option.NEGO;
			break;
		case "차수":
			option = Option.DIFF;
			break;
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("공고");
		
		if (!restarted) {
			processNoti("물품");
			processNoti("공사");
			processNoti("용역");
			processNoti("외자");
		}
		else {
			if (savedType == Type.PROD) {
				processNoti("물품");
				processNoti("공사");
				processNoti("용역");
				processNoti("외자");
			}
			else if (savedType == Type.FACIL) {
				processNoti("공사");
				processNoti("용역");
				processNoti("외자");
			}
			else if (savedType == Type.FACIL) {
				processNoti("용역");
				processNoti("외자");
			}
			else {
				processNoti("외자");
			}
		}
	}
	
	public void processNoti(String t) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		connectDB();
		
		setType(t);
		savedType = type;
		int page = (restarted) ? savedPage : 1;
		
		String path = buildDatePath() + "&pageNo=" + page;
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		while ( count == null || count.text().equals("") ) {
			Thread.sleep(500);
			doc = getResponse(path);
			count = doc.getElementsByTag("totalCount").first();
		}
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		int index = (restarted) ? savedIndex : 0;
		Elements items = doc.getElementsByTag("item");
		int i = (restarted) ? savedItem : 0;
		for ( ; i < totalCount; i++) {
			if (tracker != null) tracker.updateProgress(); 
			
			savedItem = i;
			Element item = items.get(index);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // 입찰공고번호
			String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // 입찰공고차수
			String rebidno = "0"; // 재입찰번호
			String category = (type == Type.PROD) ? "1" : "0"; // 입찰분류
			String openDate = item.getElementsByTag("opengDt").text(); // 개찰일시
			String notiType = item.getElementsByTag("ntceKindNm").text(); // 공고종류
			String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // 담당자명
			String notiorg = item.getElementsByTag("ntceInsttNm").text(); // 공고기관명
			String demorg = item.getElementsByTag("dminsttNm").text(); // 수요기관명
			String rebid = item.getElementsByTag("rbidPermsnYn").text(); // 재입찰허용여부
			String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // 지명경쟁
			String exec = item.getElementsByTag("exctvNm").text(); // 집행관명
			String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // 총예가갯수
			String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // 추첨예가수
			String bidType = item.getElementsByTag("bidMethdNm").text(); // 입찰방식명
			String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // 계약방법
			String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // 예가재작성여부
			String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // 예가방식
			String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // 낙찰하한율
			String license = ""; // 면허제한
			
			Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // 구매대상물품목록
			if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
				String buyInfo = buyCheck.first().text();
				buyInfo = buyInfo.replaceAll(",", "");
				String buyInfos[] = buyInfo.split("\\]");
				for (int j = 0; j < buyInfos.length; j++) {
					complete = false;
					
					String detail = buyInfos[j];
					detail = detail.substring(1);
					category = detail.split("\\^")[0];
					
					String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
							+ "입찰공고차수=\"" + bidver + "\" AND "
							+ "재입찰번호=" + rebidno + " AND "
							+ "입찰분류=" + category;
						
					String sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
					rs = st.executeQuery(sql);
					if (rs.next()) {
						int finished = rs.getInt("공고완료");
						if (finished == 1) {
							complete = true;
						}
					}
					else {
						sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
								+ "\"" + bidnum + "\", "
								+ "\"" + bidver + "\", "
								+ rebidno + ", "
								+ category + ");";
						System.out.println(sql);
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
						+ "입찰공고차수=\"" + bidver + "\" AND "
						+ "재입찰번호=" + rebidno + " AND "
						+ "입찰분류=" + category;
				
				String sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
				rs = st.executeQuery(sql);
				if (rs.next()) {
					int finished = rs.getInt("공고완료");
					if (finished == 1) {
					complete = true;
					}
				}
				else {
					sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
							+ "\"" + bidnum + "\", "
							+ "\"" + bidver + "\", "
							+ rebidno + ", "
							+ category + ");";
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
				
			if (!complete) {
				int licenseCount = 0;
				if (Resources.isNumeric(bidnum)) {
					String licensePath = buildLicensePath(bidnum, bidver);
					Document licenseDoc = getResponse(licensePath);
					Element countDiv = licenseDoc.getElementsByTag("totalcount").first();
					if ( countDiv == null || countDiv.text().equals("") ) {
						Thread.sleep(500);
						licenseDoc = getResponse(licensePath);
						countDiv = licenseDoc.getElementsByTag("totalcount").first();
					}
					if ( countDiv == null || countDiv.text().equals("") ) {
						licenseCount = 0;
					}
					else licenseCount = Integer.parseInt(countDiv.text()); 
					
					Elements licenseItems = licenseDoc.getElementsByTag("item");
					for (int j = 0; j < licenseCount; j++) {
						Element licenseItem = licenseItems.get(j);
						String licenseText = licenseItem.getElementsByTag("lcnsLmtNm").text(); 
						if (!licenseText.equals("")) {
							license += "[" + licenseText + "] ";
						}
					}
					if (license.length() > 200) license = license.substring(0, 200);
				}
				
				String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
						+ "입찰공고차수=\"" + bidver + "\"";
				
				String sql = "UPDATE narabidinfo SET 예정개찰일시=\"" + openDate + "\", "
						+ "공고종류=\"" + notiType + "\", "
						+ "업무=\"" + t + "\", "
						+ "담당자=\"" + damdang + "\", "
						+ "발주기관=\"" + notiorg + "\", "
						+ "수요기관=\"" + demorg + "\", "
						+ "재입찰허용여부=\"" + rebid + "\", "
						+ "예비가격재작성여부=\"" + reprice + "\", "
						+ "예가방법=\"" + priceMethod + "\", "
						+ "지명경쟁=\"" + jimyung + "\", "
						+ "집행관=\"" + exec + "\", "
						+ "총예가갯수=" + priceNumber + ", "
						+ "추첨예가갯수=" + selectNumber + ", "
						+ "입찰방식=\"" + bidType + "\", "
						+ "낙찰하한율=\"" + bidRate + "\", "
						+ "면허제한=\"" + license + "\", "
						+ "계약방법=\"" + compType + "\", 공고완료=1";
				if (compType.contains("협상")) sql += ", 협상건=1";
				if (rebid.equals("N")) sql += ", 재입찰완료=1";
				if (!priceMethod.equals("복수예가")) sql += ", 기초완료=1";
				if (priceMethod.equals("비예가") || priceMethod.equals("")) sql += ", 복수완료=1";
				sql += " " + where;
				
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			
			if ( (index + 1) % Integer.parseInt(NUM_OF_ROWS) == 0) {
				page++;
				index = 0;
				savedPage = page;
				savedIndex = index;
				
				String newPath = path + "&pageNo=" + page;
				doc = getResponse(newPath);
				while (doc.getElementsByTag("item").size() < 1) {
					Thread.sleep(500);
					doc = getResponse(newPath);
				}
				items = doc.getElementsByTag("item");
			}
			else {
				index++;
				savedIndex = index;
			}
		}
		
		closeDB();
		savedIndex = 0;
		savedPage = 1;
	}
	
	public void processNoti(String t, String bidno) throws IOException, InterruptedException, SQLException {
		String path = buildItemPath(bidno);
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		while ( count == null || count.text().equals("") ) {
			Thread.sleep(500);
			doc = getResponse(path);
			count = doc.getElementsByTag("totalCount").first();
		}
		int totalCount = Integer.parseInt(count.text());
		
		int index = 0;
		Elements items = doc.getElementsByTag("item");
		int i = 0;
		for ( ; i < totalCount; i++) {
			Element item = items.get(index);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // 입찰공고번호
			String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // 입찰공고차수
			String rebidno = "0"; // 재입찰번호
			String category = (type == Type.PROD) ? "1" : "0"; // 입찰분류
			String openDate = item.getElementsByTag("opengDt").text(); // 개찰일시
			String notiType = item.getElementsByTag("ntceKindNm").text(); // 공고종류
			String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // 담당자명
			String notiorg = item.getElementsByTag("ntceInsttNm").text(); // 공고기관명
			String demorg = item.getElementsByTag("dminsttNm").text(); // 수요기관명
			String rebid = item.getElementsByTag("rbidPermsnYn").text(); // 재입찰허용여부
			String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // 지명경쟁
			String exec = item.getElementsByTag("exctvNm").text(); // 집행관명
			String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // 총예가갯수
			String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // 추첨예가수
			String bidType = item.getElementsByTag("bidMethdNm").text(); // 입찰방식명
			String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // 계약방법
			String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // 예가재작성여부
			String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // 예가방식
			String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // 낙찰하한율
			String license = ""; // 면허제한
			
			Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // 구매대상물품목록
			if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
				String buyInfo = buyCheck.first().text();
				buyInfo = buyInfo.replaceAll(",", "");
				String buyInfos[] = buyInfo.split("\\]");
				for (int j = 0; j < buyInfos.length; j++) {
					complete = false;
					
					String detail = buyInfos[j];
					detail = detail.substring(1);
					category = detail.split("\\^")[0];
					
					String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
							+ "입찰공고차수=\"" + bidver + "\" AND "
							+ "재입찰번호=" + rebidno + " AND "
							+ "입찰분류=" + category;
						
					String sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
					rs = st.executeQuery(sql);
					if (rs.next()) {
						int finished = rs.getInt("공고완료");
						if (finished == 1) {
							complete = true;
						}
					}
					else {
						sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
								+ "\"" + bidnum + "\", "
								+ "\"" + bidver + "\", "
								+ rebidno + ", "
								+ category + ");";
						System.out.println(sql);
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
						+ "입찰공고차수=\"" + bidver + "\" AND "
						+ "재입찰번호=" + rebidno + " AND "
						+ "입찰분류=" + category;
				
				String sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
				rs = st.executeQuery(sql);
				if (rs.next()) {
					int finished = rs.getInt("공고완료");
					if (finished == 1) {
					complete = true;
					}
				}
				else {
					sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
							+ "\"" + bidnum + "\", "
							+ "\"" + bidver + "\", "
							+ rebidno + ", "
							+ category + ");";
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
				
			if (!complete) {
				int licenseCount = 0;
				if (Resources.isNumeric(bidnum)) {
					String licensePath = buildLicensePath(bidnum, bidver);
					Document licenseDoc = getResponse(licensePath);
					Element countDiv = licenseDoc.getElementsByTag("totalcount").first();
					if ( countDiv == null || countDiv.text().equals("") ) {
						Thread.sleep(500);
						licenseDoc = getResponse(licensePath);
						countDiv = licenseDoc.getElementsByTag("totalcount").first();
					}
					if ( countDiv == null || countDiv.text().equals("") ) {
						licenseCount = 0;
					}
					else licenseCount = Integer.parseInt(countDiv.text()); 
					
					Elements licenseItems = licenseDoc.getElementsByTag("item");
					for (int j = 0; j < licenseCount; j++) {
						Element licenseItem = licenseItems.get(j);
						String licenseText = licenseItem.getElementsByTag("lcnsLmtNm").text(); 
						if (!licenseText.equals("")) {
							license += "[" + licenseText + "] ";
						}
					}
					if (license.length() > 200) license = license.substring(0, 200);
				}
				
				String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
						+ "입찰공고차수=\"" + bidver + "\"";
				
				String sql = "UPDATE narabidinfo SET 예정개찰일시=\"" + openDate + "\", "
						+ "공고종류=\"" + notiType + "\", "
						+ "업무=\"" + t + "\", "
						+ "담당자=\"" + damdang + "\", "
						+ "발주기관=\"" + notiorg + "\", "
						+ "수요기관=\"" + demorg + "\", "
						+ "재입찰허용여부=\"" + rebid + "\", "
						+ "예비가격재작성여부=\"" + reprice + "\", "
						+ "예가방법=\"" + priceMethod + "\", "
						+ "지명경쟁=\"" + jimyung + "\", "
						+ "집행관=\"" + exec + "\", "
						+ "총예가갯수=" + priceNumber + ", "
						+ "추첨예가갯수=" + selectNumber + ", "
						+ "입찰방식=\"" + bidType + "\", "
						+ "낙찰하한율=\"" + bidRate + "\", "
						+ "면허제한=\"" + license + "\", "
						+ "계약방법=\"" + compType + "\", 공고완료=1";
				if (compType.contains("협상")) sql += ", 협상건=1";
				if (rebid.equals("N")) sql += ", 재입찰완료=1";
				if (!priceMethod.equals("복수예가")) sql += ", 기초완료=1";
				if (priceMethod.equals("비예가") || priceMethod.equals("")) sql += ", 복수완료=1";
				sql += " " + where;
				
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
	}
	
	public void processRes() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("결과");
		
		if (!restarted) {
			processRes("물품");
			processRes("공사");
			processRes("용역");
			processRes("외자");
		}
		else {
			if (savedType == Type.PROD) {
				processRes("물품");
				processRes("공사");
				processRes("용역");
				processRes("외자");
			}
			else if (savedType == Type.FACIL) {
				processRes("공사");
				processRes("용역");
				processRes("외자");
			}
			else if (savedType == Type.FACIL) {
				processRes("용역");
				processRes("외자");
			}
			else {
				processRes("외자");
			}
		}
	}
	
	public void processRes(String t) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		connectDB();
		
		setType(t);
		savedType = type;
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		while ( count == null || count.text().equals("") ) {
			Thread.sleep(500);
			doc = getResponse(path);
			count = doc.getElementsByTag("totalCount").first();	
		}
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		boolean getNoti = false;
		boolean getBase = false;
		boolean getPre = false;
		int index = 0;
		int page = 1;
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			if (tracker != null) tracker.updateProgress(); 
			
			Element item = items.get(index);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("bidNtceNo").text(); // 입찰공고번호
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // 입찰공고차수
			String rebidno = item.getElementsByTag("rbidNo").text(); // 재입찰번호
			String category = item.getElementsByTag("bidClsfcNo").text(); // 입찰분류
			
			String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
					+ "입찰공고차수=\"" + bidver + "\" AND "
					+ "재입찰번호=" + rebidno + " AND "
					+ "입찰분류=" + category;
				
			String sql = "SELECT 결과완료, 공고완료, 기초완료, 복수완료 FROM narabidinfo " + where;
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int finished = rs.getInt("결과완료");
				int notiFinished = rs.getInt("공고완료");
				int baseFinished = rs.getInt("기초완료");
				int preFinished = rs.getInt("복수완료");
				
				if (finished == 1) complete = true;
				if (notiFinished == 1) getNoti = true;
				if (baseFinished == 1) getBase = true;
				if (preFinished == 1) getPre = true;
			}
			else {
				sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
						+ "\"" + bidnum + "\", "
						+ "\"" + bidver + "\", "
						+ rebidno + ", "
						+ category + ");";
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			
			if (!complete) {
				String openDate = item.getElementsByTag("opengDt").text(); // 예정개찰일시
				String realDate = item.getElementsByTag("inptDt").text(); // 실제개찰일시
				String winner = item.getElementsByTag("opengCorpInfo").text(); // 순위업체정보
				String result = item.getElementsByTag("progrsDivCdNm").text(); // 진행구분코드
				String comp = item.getElementsByTag("prtcptCnum").text(); // 참가자수
				String notiOrg = item.getElementsByTag("ntceInsttNm").text(); // 발주기관
				String demOrg = item.getElementsByTag("dminsttNm").text(); // 수요기관
				
				String bidPrice = "0";
				String winnerInfo[] = winner.split("\\^");
				if (winnerInfo.length == 5) bidPrice = winnerInfo[3];
				if (winnerInfo.length == 3) bidPrice = winnerInfo[1];
				if (bidPrice.equals("")) bidPrice = "0";
				if (!Resources.isNumeric(bidPrice)) bidPrice = "0";
				
				sql = "UPDATE narabidinfo SET 예정개찰일시=\"" + openDate + "\", "
						+ "실제개찰일시=\"" + realDate + "\", "
						+ "진행구분코드=\"" + result + "\", "
						+ "업무=\"" + t + "\", "
						+ "발주기관=\"" + notiOrg + "\", "
						+ "수요기관=\"" + demOrg + "\", "
						+ "투찰금액=\"" + bidPrice + "\", "
						+ "참가자수=\"" + comp + "\", 결과완료=1";
				if (result.equals("유찰")) sql += ", 복수완료=1, 재입찰완료=1";
				if (result.equals("개찰완료")) sql += ", 재입찰완료=1";
				sql += " " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
				
				String checkDate = openDate.substring(0, 10);
				sql = "SELECT openDate FROM naracounter WHERE openDate=\"" + checkDate + "\"";
				rs = st.executeQuery(sql);
				
				if (rs.next()) {
					sql = "UPDATE naracounter SET counter=counter+1 WHERE openDate=\"" + checkDate + "\"";
					System.out.println(sql);
					st.executeUpdate(sql);
				}
				else {
					sql = "INSERT INTO naracounter (openDate, counter) VALUES (\"" + checkDate + "\", 1)";
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
			
			if (getNoti) {
				setOption("공고");
				processNoti(t, bidnum);
				setOption("결과");
			}
			
			if (getBase) {
				setOption("기초금액");
				processBasePrice(t, bidnum, false);
				setOption("결과");
			}
			
			if (getPre) {
				setOption("예비가격");
				processPrePrice(t, bidnum, false);
				setOption("결과");
			}
			
			if ( (index + 1) % Integer.parseInt(NUM_OF_ROWS) == 0) {
				page++;
				index = 0;
				
				String newPath = path + "&pageNo=" + page;
				doc = getResponse(newPath);
				while (doc.getElementsByTag("item").size() < 1) {
					Thread.sleep(500);
					doc = getResponse(newPath);
				}
				items = doc.getElementsByTag("item");
			}
			else {
				index++;
			}
		}
		
		closeDB();
	}
	
	public void processBasePrice() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("기초금액");
		
		if (!restarted) {
			processBasePrice("물품");
			processBasePrice("공사");
			processBasePrice("용역");
			processBasePrice("외자");
		}
		else {
			if (savedType == Type.PROD) {
				processBasePrice("물품");
				processBasePrice("공사");
				processBasePrice("용역");
				processBasePrice("외자");
			}
			else if (savedType == Type.FACIL) {
				processBasePrice("공사");
				processBasePrice("용역");
				processBasePrice("외자");
			}
			else if (savedType == Type.FACIL) {
				processBasePrice("용역");
				processBasePrice("외자");
			}
			else {
				processBasePrice("외자");
			}
		}
	}
	
	public void processBasePrice(String t) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		savedType = type;
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "예정개찰일시 BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "공고완료=1 AND 기초완료=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			processBasePrice(t, bidNum, true);
		}
		
		closeDB();
	}
	
	public void processBasePrice(String t, String bidno, boolean mainProcess) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		setType(t);
		
		String path = buildItemPath(bidno);
		
		if (mainProcess && tracker != null) tracker.updateProgress();
		
		Document doc = getResponse(path);
		while (doc.getElementsByTag("errMsg").size() > 0) {
			Thread.sleep(500);
			doc = getResponse(path);
		}
		int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // 입찰공고차수
			String series = item.getElementsByTag("bidClsfcNo").text(); // 입찰분류
			String level = item.getElementsByTag("dfcltydgrCfcnt").text(); // 난이도계수
			String basePrice = item.getElementsByTag("bssamt").text(); // 기초예정가격
			if (basePrice.equals("")) basePrice = "0";
			String lower = item.getElementsByTag("rsrvtnPrceRngBgnRate").text(); // 하한
			String upper = item.getElementsByTag("rsrvtnPrceRngEndRate").text(); // 상한
			
			String where = "WHERE 입찰공고번호=\"" + bidno + "\" AND 입찰공고차수=\"" + bidver + "\" AND 입찰분류=" + series;
			
			String sql = "UPDATE narabidinfo SET 난이도계수=\"" + level + "\", "
					+ "기초예정가격=" + basePrice + ", "
					+ "하한수=\"" + lower + "\", "
					+ "상한수=\"" + upper + "\", 기초완료=1 " + where;
			System.out.println(sql);
			st.executeUpdate(sql);
		}
	}
	
	public void processPrePrice() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("예비가격");
		
		if (!restarted) {
			processPrePrice("물품");
			processPrePrice("공사");
			processPrePrice("용역");
			processPrePrice("외자");
		}
		else {
			if (savedType == Type.PROD) {
				processPrePrice("물품");
				processPrePrice("공사");
				processPrePrice("용역");
				processPrePrice("외자");
			}
			else if (savedType == Type.FACIL) {
				processPrePrice("공사");
				processPrePrice("용역");
				processPrePrice("외자");
			}
			else if (savedType == Type.SERV) {
				processPrePrice("용역");
				processPrePrice("외자");
			}
			else processPrePrice("외자");
		}
	}
	
	public void processPrePrice(String t) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		savedType = type;
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "실제개찰일시 BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "결과완료=1 AND 복수완료=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (int i = 0; i < bidNums.size(); i++) {
			
			String bidNum = bidNums.get(i);
			processPrePrice(t, bidNum, true);
			
			Thread.sleep(200);
		}
		
		closeDB();
	}
	
	public void processPrePrice(String t, String bidNum, boolean mainProcess) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		String path = buildItemPath(bidNum);
		
		if (mainProcess && tracker != null) tracker.updateProgress();
		
		Document doc = getResponse(path);
		while (doc.getElementsByTag("errMsg").size() > 0) {
			Thread.sleep(500);
			doc = getResponse(path);
		}
		int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
		
		int num = 0;
		int page = 1;
		Elements items = doc.getElementsByTag("item");
		for (int j = 0; j < totalCount; j++) {
			Element item = items.get(num);
			
			//String bidNum = item.getElementsByTag("bidNtceNo").text();
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // 입찰공고차수
			String series = item.getElementsByTag("bidClsfcNo").text(); // 입찰분류
			String rebidno = item.getElementsByTag("rbidNo").text(); // 재입찰번호
			String index = item.getElementsByTag("compnoRsrvtnPrceSno").text(); // 순번
			String drawNum = item.getElementsByTag("drwtNum").text(); // 추첨횟수
			String priceNum = item.getElementsByTag("totRsrvtnPrceNum").text(); // 총예가갯수
			if (priceNum.equals("")) priceNum = "0";
			String dupPrice = item.getElementsByTag("bsisPlnprc").text(); // 복수예가
			if (dupPrice.equals("")) dupPrice = "0";
			String expPrice = item.getElementsByTag("plnprc").text(); // 예정가격
			if (expPrice.equals("")) expPrice = "0";
			String basePrice = item.getElementsByTag("bssamt").text(); // 기초금액
			if (basePrice.equals("")) basePrice = "0";
			String priceDate = item.getElementsByTag("compnoRsrvtnPrceMkngDt").text(); // 복수예가 작성일시
			
			String sql = "";
			String where = "WHERE 입찰공고번호=\"" + bidNum + "\" AND 입찰공고차수=\"" + bidver + "\" AND 재입찰번호=" + rebidno + " AND 입찰분류=" + series;
			if (!index.equals("")) {
				sql = "UPDATE narabidinfo SET 총예가갯수=" + priceNum + ", "
						+ "복수" + index + "=" + dupPrice + ", "
						+ "복참" + index + "=" + drawNum + ", "
						+ "기초예정가격=" + basePrice + ", "
						+ "예정가격=" + expPrice + " ";
				if (!priceDate.equals("")) sql += ", 복수예가작성일시=\"" + priceDate + "\" ";
				if (index.equals(priceNum)) sql += ", 복수완료=1 ";
				sql += where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			else {
				sql = "UPDATE narabidinfo SET 총예가갯수=" + priceNum + ", "
						+ "예정가격=" + expPrice + ", 복수완료=1 " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			
			if ( (num + 1) % Integer.parseInt(NUM_OF_ROWS) == 0) {
				page++;
				num = 0;
				
				String newPath = path + "&pageNo=" + page;
				doc = getResponse(newPath);
				while (doc.getElementsByTag("item").size() < 1) {
					Thread.sleep(500);
					doc = getResponse(newPath);
				}
				items = doc.getElementsByTag("item");
			}
			else {
				num++;
			}
		}
	}
	
	public void processRebid() throws ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		ArrayList<String> bidVers = new ArrayList<String>();
		ArrayList<String> rebidNums = new ArrayList<String>();
		ArrayList<String> categories = new ArrayList<String>();
		
		String sql = "SELECT 입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류 FROM narabidinfo WHERE 진행구분코드=\"재입찰\" AND 재입찰완료=0";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
			bidVers.add(rs.getString("입찰공고차수"));
			rebidNums.add(rs.getString("재입찰번호"));
			categories.add(rs.getString("입찰분류"));
		}
		
		totalItem += bidNums.size();
		
		for (int i = 0; i < bidNums.size(); i++) {
			if (tracker != null) tracker.updateProgress();
			
			String bidno = bidNums.get(i);
			String bidver = bidVers.get(i);
			String rebidno = rebidNums.get(i);
			String category = categories.get(i);
			
			rebidno = "" + (Integer.parseInt(rebidno) + 1);
			
			sql = "SELECT 입찰공고번호 FROM narabidinfo WHERE 입찰공고번호=\"" + bidno + "\" AND "
					+ "입찰공고차수=\"" + bidver + "\" AND "
					+ "재입찰번호=" + rebidno + " AND "
					+ "입찰분류=" + category;
			rs = st.executeQuery(sql);
			
			if (!rs.next()) {
				sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES "
						+ "(\"" + bidno + "\", \"" + bidver + "\", " + rebidno + ", " + category + ")";
				st.executeUpdate(sql);
			}
			
			rebidno = "" + (Integer.parseInt(rebidno) - 1);
			
			sql = "UPDATE narabidinfo SET 재입찰완료=1 WHERE 입찰공고번호=\"" + bidno + "\" AND "
					+ "입찰공고차수=\"" + bidver + "\" AND "
					+ "재입찰번호=" + rebidno + " AND "
					+ "입찰분류=" + category;
		}
		
		sql = "SELECT 입찰공고번호, 입찰공고차수, 입찰분류 FROM narabidinfo WHERE 재입찰번호>0 AND 공고완료=0 AND 기초완료=0";
		rs = st.executeQuery(sql);
		
		bidNums.clear();
		bidVers.clear();
		rebidNums.clear();
		categories.clear();
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
			bidVers.add(rs.getString("입찰공고차수"));
			categories.add(rs.getString("입찰분류"));
		}
		
		totalItem += bidNums.size();
		
		for (int i = 0; i < bidNums.size(); i++) {
			if (tracker != null) tracker.updateProgress();
			
			String bidno = bidNums.get(i);
			String bidver = bidVers.get(i);
			String category = bidVers.get(i);
			
			String where = "WHERE 입찰공고번호=\"" + bidno + "\" AND "
					+ "입찰공고차수=\"" + bidver + "\" AND "
					+ "입찰분류=" + category;
			
			sql = "SELECT 예정개찰일시, 발주기관, 수요기관, 입찰방식, 계약방법, 담당자, 집행관, 재입찰허용여부, 예비가격재작성여부, "
					+ "면허제한, 예가방법, 총예가갯수, 추첨예가갯수, 지명경쟁, 기초예정가격, 하한수, 상한수, 난이도계수, 업무, 공고완료, 기초완료 FROM narabidinfo " + where;
			
			rs = st.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt("공고완료") == 1) {
					String lowerBound = rs.getString("하한수");
					String upperBound = rs.getString("상한수");
					
					sql = "UPDATE narabidinfo SET 예정개찰일시=\"" + rs.getString("예정개찰일시") + "\", "
							+ "발주기관=\"" + rs.getString("발주기관") + "\", "
							+ "수요기관=\"" + rs.getString("수요기관") + "\", "
							+ "입찰방식=\"" + rs.getString("입찰방식") + "\", "
							+ "계약방법=\"" + rs.getString("계약방법") + "\", "
							+ "담당자=\"" + rs.getString("담당자") + "\", "
							+ "집행관=\"" + rs.getString("집행관") + "\", "
							+ "재입찰허용여부=\"" + rs.getString("재입찰허용여부") + "\", "
							+ "예비가격재작성여부=\"" + rs.getString("예비가격재작성여부") + "\", "
							+ "면허제한=\"" + rs.getString("면허제한") + "\", "
							+ "예가방법=\"" + rs.getString("예가방법") + "\", "
							+ "총예가갯수=" + rs.getString("총예가갯수") + ", "
							+ "추첨예가갯수=" + rs.getString("추첨예가갯수") + ", "
							+ "지명경쟁=\"" + rs.getString("지명경쟁") + "\", "
							+ "기초예정가격=" + rs.getString("기초예정가격") + ", ";
					if (lowerBound != null) sql += "하한수=\"" + rs.getString("하한수") + "\", ";
					if (upperBound != null) sql += "상한수=\"" + rs.getString("상한수") + "\", ";
					sql += "난이도계수=\"" + rs.getString("난이도계수") + "\", "
							+ "업무=\"" + rs.getString("업무") + "\", "
							+ "공고완료=" + rs.getString("공고완료") + ", "
							+ "기초완료=" + rs.getString("기초완료") + " " + where;
					System.out.println(sql);
					st.executeUpdate(sql);
					
					break;
				}
			}
		}
		
		closeDB();
	}
	
	public void processNegoPrice() throws ClassNotFoundException, SQLException, IOException {
		totalItem = 0;
		
		setOption("예비가격");
		
		processNegoPrice("물품");
		processNegoPrice("공사");
		processNegoPrice("용역");
		processNegoPrice("외자");
	}
	
	public void processNegoPrice(String t) throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND 협상건=1 AND 결과완료=1 AND 복수완료=0";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			String path = buildItemPath(bidNum);
			
			Document doc = getResponse(path);
			int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
			
			if (tracker != null) tracker.updateProgress();
			
			Elements items = doc.getElementsByTag("item");
			for (int i = 0; i < totalCount; i++) {
				Element item = items.get(i);
				
				String bidver = item.getElementsByTag("입찰공고차수").text();
				String series = item.getElementsByTag("입찰분류").text();
				String rebidno = item.getElementsByTag("재입찰번호").text();
				String index = item.getElementsByTag("일련번호").text();
				String priceNum = item.getElementsByTag("총예가갯수").text();
				String dupPrice = item.getElementsByTag("기초예정가격").text();
				if (dupPrice.equals("")) dupPrice = "0";
				String expPrice = item.getElementsByTag("예정가격금액").text();
				if (expPrice.equals("")) expPrice = "0";
				
				if (!index.equals("")) {
					String where = "WHERE 입찰공고번호=\"" + bidNum + "\" AND 입찰공고차수=\"" + bidver + "\" AND 재입찰번호=" + rebidno + " AND 입찰분류=" + series;
					
					sql = "UPDATE narabidinfo SET 총예가갯수=\"" + priceNum + "\", "
							+ "복수" + index + "=" + dupPrice + ", "
							+ "예정가격=" + expPrice + ", 복수완료=1 " + where;
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
		}
		
		closeDB();
	}
	
	public void processDiff() throws ClassNotFoundException, SQLException, IOException {
		totalItem = 0;
		
		setOption("결과");
		
		processDiff("물품");
		processDiff("공사");
		processDiff("용역");
		processDiff("외자");
	}
	
	public void processDiff(String t) throws SQLException, ClassNotFoundException, IOException {
		connectDB();
		
		setType(t);
		
		ArrayList<String> bidNums = new ArrayList<String>();
		ArrayList<String> bidVers = new ArrayList<String>();
		ArrayList<String> rebidNums = new ArrayList<String>();
		ArrayList<String> categories = new ArrayList<String>();
		ArrayList<String> dates = new ArrayList<String>();
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT 입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류, 실제개찰일시 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "실제개찰일시 BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND 결과완료=1";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
			bidVers.add(rs.getString("입찰공고차수"));
			rebidNums.add(rs.getString("재입찰번호"));
			categories.add(rs.getString("입찰분류"));
			dates.add(rs.getString("실제개찰일시"));
		}
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			
			String bidnum = item.getElementsByTag("입찰공고번호").text();
			String bidver = item.getElementsByTag("입찰공고차수").text();
			String rebidno = item.getElementsByTag("재입찰번호").text();
			String category = item.getElementsByTag("입찰분류").text();
			
			String key = bidnum + bidver + rebidno + category;
			String dbKey = bidNums.get(0) + bidVers.get(0) + rebidNums.get(0) + categories.get(0);
			
			int index = 0;
			while ( (!key.equals(dbKey)) && ( (index + 1) < bidNums.size()) ) {
				index++;
				dbKey = bidNums.get(index) + bidVers.get(index) + rebidNums.get(index) + categories.get(index);
			}
			
			if (index < bidNums.size()) {
				bidNums.remove(index);
				bidVers.remove(index);
				rebidNums.remove(index);
				categories.remove(index);
				dates.remove(index);
			}
		}
		
		// Remove items not matched
		for (int i = 0; i < bidNums.size(); i++) {
			String where = "WHERE 입찰공고번호=\"" + bidNums.get(i) + "\" AND "
					+ "입찰공고차수=\"" + bidVers.get(i) + "\" AND "
					+ "재입찰번호=" + rebidNums.get(i) + " AND "
					+ "입찰분류=" + categories.get(i);
			
			sql = "DELETE FROM narabidinfo " + where;
			System.out.println(sql);
			st.executeUpdate(sql);
			
			sql = "UPDATE naracounter SET counter=counter-1 WHERE openDate=\"" + dates.get(i).substring(0, 10) + "\"";
			System.out.println(sql);
			st.executeUpdate(sql);
		}
		
		closeDB();
	}
	
	public void processIncomplete() throws SQLException, IOException, InterruptedException, ClassNotFoundException {
		totalItem = 0;
		incompleteProcess = true;
		
		setOption("공고");
		
		processIncomplete("물품");
		processIncomplete("공사");
		processIncomplete("용역");
		processIncomplete("외자");
	}
	
	public void processIncomplete(String t) throws SQLException, IOException, InterruptedException, ClassNotFoundException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "예정개찰일시 BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "결과완료=1 AND 공고완료=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			sql = "UPDATE narabidinfo SET 공고완료=0 WHERE 입찰공고번호=\"" + bidNum + "\"";
			System.out.println(sql);
			st.executeUpdate(sql);
			
			String path = buildItemPath(bidNum);
			
			Document doc = getResponse(path);
			while (doc.getElementsByTag("errMsg").size() > 0) {
				Thread.sleep(500);
				doc = getResponse(path);
			}
			int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
			if (totalCount > 15) totalCount = 15;
			
			if (tracker != null) tracker.updateProgress();
			
			int index = 0;
			int page = 1;
			Elements items = doc.getElementsByTag("item");
			for (int i = 0; i < totalCount; i++) {
				if (tracker != null) tracker.updateProgress(); 
				
				Element item = items.get(index);
				boolean complete = false;
				
				String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // 입찰공고번호
				String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // 입찰공고차수
				String rebidno = "0"; // 재입찰번호
				String category = (type == Type.PROD) ? "1" : "0"; // 입찰분류
				String openDate = item.getElementsByTag("opengDt").text(); // 개찰일시
				String notiType = item.getElementsByTag("ntceKindNm").text(); // 공고종류
				String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // 담당자명
				String notiorg = item.getElementsByTag("ntceInsttNm").text(); // 공고기관명
				String demorg = item.getElementsByTag("dminsttNm").text(); // 수요기관명
				String rebid = item.getElementsByTag("rbidPermsnYn").text(); // 재입찰허용여부
				String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // 지명경쟁
				String exec = item.getElementsByTag("exctvNm").text(); // 집행관명
				String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // 총예가갯수
				String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // 추첨예가수
				String bidType = item.getElementsByTag("bidMethdNm").text(); // 입찰방식명
				String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // 계약방법
				String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // 예가재작성여부
				String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // 예가방식
				String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // 낙찰하한율
				
				String license = item.getElementsByTag("mainCnsttyNm").text();
				if (!license.equals("")) {
					for (int k = 1; k <= 9; k++) {
						String key = "subsiCnsttyNm1" + k;
						Elements licenseCheck = item.getElementsByTag(key);
						if (licenseCheck.size() > 0) {
							String check = licenseCheck.first().text();
							if (!check.equals("")) {
								license += ", " + check;
							}
							else {
								break;
							}
						}
					}
					if (license.length() > 200) {
						license = license.substring(0, 200);
					}
				}
				
				Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // 구매대상물품목록
				if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
					String buyInfo = buyCheck.first().text();
					buyInfo = buyInfo.replaceAll(",", "");
					String buyInfos[] = buyInfo.split("\\]");
					for (int j = 0; j < buyInfos.length; j++) {
						complete = false;
						
						String detail = buyInfos[j];
						detail = detail.substring(1);
						category = detail.split("\\^")[0];
						
						String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
								+ "입찰공고차수=\"" + bidver + "\" AND "
								+ "재입찰번호=" + rebidno + " AND "
								+ "입찰분류=" + category;
							
						sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
						rs = st.executeQuery(sql);
						if (rs.next()) {
							int finished = rs.getInt("공고완료");
							if (finished == 1) {
								complete = true;
							}
						}
						else {
							sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
									+ "\"" + bidnum + "\", "
									+ "\"" + bidver + "\", "
									+ rebidno + ", "
									+ category + ");";
							System.out.println(sql);
							st.executeUpdate(sql);
						}
					}
				}
				else {
					String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
							+ "입찰공고차수=\"" + bidver + "\" AND "
							+ "재입찰번호=" + rebidno + " AND "
							+ "입찰분류=" + category;
					
					sql = "SELECT 입찰공고번호, 공고완료 FROM narabidinfo " + where;
					rs = st.executeQuery(sql);
					if (rs.next()) {
						int finished = rs.getInt("공고완료");
						if (finished == 1) {
						complete = true;
						}
					}
					else {
						sql = "INSERT INTO narabidinfo (입찰공고번호, 입찰공고차수, 재입찰번호, 입찰분류) VALUES ("
								+ "\"" + bidnum + "\", "
								+ "\"" + bidver + "\", "
								+ rebidno + ", "
								+ category + ");";
						System.out.println(sql);
						st.executeUpdate(sql);
					}
				}
				
				if (!complete) {
					String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
							+ "입찰공고차수=\"" + bidver + "\" AND "
							+ "입찰분류=" + category;
					
					sql = "UPDATE narabidinfo SET 예정개찰일시=\"" + openDate + "\", "
							+ "공고종류=\"" + notiType + "\", "
							+ "업무=\"" + t + "\", "
							+ "담당자=\"" + damdang + "\", "
							+ "발주기관=\"" + notiorg + "\", "
							+ "수요기관=\"" + demorg + "\", "
							+ "재입찰허용여부=\"" + rebid + "\", "
							+ "예비가격재작성여부=\"" + reprice + "\", "
							+ "예가방법=\"" + priceMethod + "\", "
							+ "지명경쟁=\"" + jimyung + "\", "
							+ "집행관=\"" + exec + "\", "
							+ "총예가갯수=" + priceNumber + ", "
							+ "추첨예가갯수=" + selectNumber + ", "
							+ "입찰방식=\"" + bidType + "\", "
							+ "낙찰하한율=\"" + bidRate + "\", "
							+ "면허제한=\"" + license + "\", "
							+ "계약방법=\"" + compType + "\", 공고완료=1";
					if (compType.contains("협상")) sql += ", 협상건=1";
					if (rebid.equals("N")) sql += ", 재입찰완료=1";
					if (!priceMethod.equals("복수예가")) sql += ", 기초완료=1";
					if (priceMethod.equals("비예가") || priceMethod.equals("")) sql += ", 복수완료=1";
					sql += " " + where;
						
					System.out.println(sql);
					st.executeUpdate(sql);
				}
				
				if ( (index + 1) % Integer.parseInt(NUM_OF_ROWS) == 0) {
					page++;
					index = 0;
				
					String newPath = path + "&pageNo=" + page;
					doc = getResponse(newPath);
					while (doc.getElementsByTag("item").size() < 1) {
						Thread.sleep(500);
						doc = getResponse(newPath);
					}
					items = doc.getElementsByTag("item");
				}
				else {
					index++;
				}
			}
		}
		
		closeDB();
	}
	
	public int getTotal() {
		return totalItem;
	}

	public void run() {
		checkOnly = false;
		
		try {
			switch(option) {
			case NOTI:
				processNoti();
				break;
			case RES:
				processRes();
				break;
			case BASE_PRICE:
				processBasePrice();
				break;
			case PRE_PRICE:
				processPrePrice();
				break;
			case PERIODIC:
				processRes();
				processBasePrice();
				break;
			case REBID:
				processRebid();
				break;
			case NEGO:
				processNegoPrice();
				break;
			case DIFF:
				processDiff();
				break;
			}
		} catch(SocketTimeoutException ste) {
			ste.printStackTrace();
			if (tracker != null) tracker.restart();
			restarted = true;
			run();
		} catch (Exception e) {
			Logger.getGlobal().log(Level.WARNING, e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (tracker != null) tracker.finish();
		}
	}

	public int checkTotal() throws IOException {
		int total = 0;
		checkOnly = true;
		
		setType("물품");
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("공사");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("용역");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("외자");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		return total;
	}
}
