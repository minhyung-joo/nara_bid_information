package nara_bid_information;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class OpenAPIReader implements Runnable {
	final String SERVER_KEY = Resources.SERVER_KEY;
	final String NUM_OF_ROWS = "50000";
	
	enum Type {
		PROD,
		FACIL,
		SERV,
		WEJA
	}
	
	enum Option {
		NOTI,
		RES,
		BASE_PRICE,
		PRE_PRICE,
		PERIODIC,
		REBID,
		NEGO
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
	
	public OpenAPIReader(String sd, String ed, String op, ProgressTracker pt) {
		if (sd.length() == 10) sd = sd.replaceAll("-", "");
		if (ed.length() == 10) ed = ed.replaceAll("-", "");
		startDate = sd;
		endDate = ed;
		tracker = pt;
		setOption(op);
	}
	
	public static void main(String args[]) throws ClassNotFoundException, IOException, SQLException {
		OpenAPIReader tester = new OpenAPIReader("20131216", "20131216", null, null);
		
		tester.processNoti();
		tester.processRes();
		tester.processBasePrice();
		tester.processPrePrice();
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
			
			return year + "-" + month + "-" + day + " 00:00:00";
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
			
			if (type == Type.PROD) path += EndPoints.PROD_TOTAL_NOTI;
			else if (type == Type.FACIL) path += EndPoints.FACIL_TOTAL_NOTI;
			else if (type == Type.SERV) path += EndPoints.SERV_TOTAL_NOTI;
			else if (type == Type.WEJA) path += EndPoints.WEJA_NOTI;
		}
		else if (option == Option.RES) {
			path = EndPoints.RES_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_RES;
			else if (type == Type.FACIL) path += EndPoints.FACIL_RES;
			else if (type == Type.SERV) path += EndPoints.SERV_RES;
			else if (type == Type.WEJA) path += EndPoints.WEJA_RES;
		}
		else if (option == Option.BASE_PRICE) {
			path = EndPoints.NOTI_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_TOTAL_BASE_PRICE;
			else if (type == Type.FACIL) path += EndPoints.FACIL_TOTAL_BASE_PRICE;
			else if (type == Type.SERV) path += EndPoints.SERV_TOTAL_BASE_PRICE;
			else if (type == Type.WEJA) path += EndPoints.WEJA_BASE_PRICE;
		}
		else if (option == Option.PRE_PRICE) {
			path = EndPoints.RES_BASE_PATH;
			
			if (type == Type.PROD) path += EndPoints.PROD_PRE_PRICE;
			else if (type == Type.FACIL) path += EndPoints.FACIL_PRE_PRICE;
			else if (type == Type.SERV) path += EndPoints.SERV_PRE_PRICE;
			else if (type == Type.WEJA) path += EndPoints.WEJA_PRE_PRICE;
		}
		
		path += "serviceKey=" + SERVER_KEY;
		path += "&numOfRows=" + NUM_OF_ROWS;
		
		return path;
	}
	
	public String buildDatePath() {
		String path = buildPath();

		path += "&sDate=" + startDate;
		path += "&eDate=" + endDate;
		
		return path;
	}
	
	public String buildItemPath(String item) {
		String path = buildPath();

		path += "&bidNum=" + item;
		
		return path;
	}
	
	public Document getResponse(String path) throws IOException {
		URL url = new URL(path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
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
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("공고");
		
		processNoti("물품");
		processNoti("공사");
		processNoti("용역");
		processNoti("외자");
	}
	
	public void processNoti(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		setType(t);
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			if (tracker != null) tracker.updateProgress(); 
			
			Element item = items.get(i);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("입찰공고번호").first().text();
			String bidver = item.getElementsByTag("입찰공고차수").first().text();
			String rebidno = "0"; // 재입찰번호
			String category = "0"; // 입찰분류
			String openDate = parseDate(item.getElementsByTag("개찰일시").text());
			String notiType = item.getElementsByTag("공고종류").text();
			String damdang = item.getElementsByTag("담당자명").text();
			String notiorg = item.getElementsByTag("발주기관").text();
			String demorg = item.getElementsByTag("수요기관명").text();
			String rebid = item.getElementsByTag("재입찰허용여부").text();
			String jimyung = item.getElementsByTag("지명경쟁").text();
			String exec = item.getElementsByTag("집행관명").text();
			String priceNumber = parseNumber(item.getElementsByTag("총예가갯수").text());
			String selectNumber = parseNumber(item.getElementsByTag("추첨예가갯수").text());
			String bidType = item.getElementsByTag("입찰방식").text();
			String compType = item.getElementsByTag("계약방법명").text();
			String reprice = item.getElementsByTag("예비가격재작성여부").text();
			String priceMethod = item.getElementsByTag("예가방법").text();
			
			String license = "";
			for (int k = 1; k <= 12; k++) {
				String key = "면허제한명" + k;
				Elements licenseCheck = item.getElementsByTag(key);
				if (licenseCheck.size() > 0) {
					String check = licenseCheck.first().text();
					if (!check.equals("/")) {
						license += check;
					}
				}
			}
			if (license.length() > 200) {
				license = license.substring(0, 200);
			}
			
			if (!notiType.equals("재입찰")) {
				Elements buyCheck = item.getElementsByTag("구매대상물품전체");
				if ( buyCheck.size() > 0 ) {
					String buyInfo = buyCheck.first().text();
					String buyInfos[] = buyInfo.split("##");
					for (int j = 0; j < buyInfos.length; j++) {
						String details[] = buyInfos[j].split("\\^");
						category = details[0];
						
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
						
						if (!complete) {
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
									+ "면허제한=\"" + license + "\", "
									+ "계약방법=\"" + compType + "\", 공고완료=1";
							if (rebid.equals("N")) sql += ", 재입찰완료=1";
							if (!priceMethod.equals("복수예가")) sql += ", 기초완료=1";
							if (priceMethod.equals("비예가")) sql += ", 복수완료=1";
							sql += " " + where;
							
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
					
					if (!complete) {
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
								+ "면허제한=\"" + license + "\", "
								+ "계약방법=\"" + compType + "\", 공고완료=1";
						if (rebid.equals("N")) sql += ", 재입찰완료=1";
						if (!priceMethod.equals("복수예가")) sql += ", 기초완료=1";
						if (priceMethod.equals("비예가") || priceMethod.equals("")) sql += ", 복수완료=1";
						sql += " " + where;
						
						System.out.println(sql);
						st.executeUpdate(sql);
					}
				}
			}
		}
		
		closeDB();
	}
	
	public void processRes() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("결과");
		
		processRes("물품");
		processRes("공사");
		processRes("용역");
		processRes("외자");
	}
	
	public void processRes(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		setType(t);
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			if (tracker != null) tracker.updateProgress(); 
			
			Element item = items.get(i);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("입찰공고번호").text();
			String bidver = item.getElementsByTag("입찰공고차수").text();
			String rebidno = item.getElementsByTag("재입찰번호").text();
			String category = item.getElementsByTag("입찰분류").text();
			
			String where = "WHERE 입찰공고번호=\"" + bidnum + "\" AND "
					+ "입찰공고차수=\"" + bidver + "\" AND "
					+ "재입찰번호=" + rebidno + " AND "
					+ "입찰분류=" + category;
				
			String sql = "SELECT 결과완료 FROM narabidinfo " + where;
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int finished = rs.getInt("결과완료");
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
			
			if (!complete) {
				String openDate = parseDate(item.getElementsByTag("개찰일시").text());
				String hasPrice = item.getElementsByTag("예비가격파일존재여부").text();
				String winner = item.getElementsByTag("순위업체정보").text();
				String result = item.getElementsByTag("진행구분코드").text();
				String comp = item.getElementsByTag("참가자수").text();
				
				String winnerInfo[] = winner.split("###");
				String bidPrice = "0";
				if (winnerInfo.length == 3) bidPrice = winnerInfo[1];
				if (bidPrice.equals("")) bidPrice = "0";
				
				sql = "UPDATE narabidinfo SET 실제개찰일시=\"" + openDate + "\", "
						+ "예비가격파일존재여부=\"" + hasPrice + "\", "
						+ "진행구분코드=\"" + result + "\", "
						+ "업무=\"" + t + "\", "
						+ "투찰금액=\"" + bidPrice + "\", "
						+ "참가자수=\"" + comp + "\", 결과완료=1";
				if (result.equals("유찰")) sql += ", 복수완료=1, 재입찰완료=1";
				if (result.equals("개찰완료")) sql += ", 재입찰완료=1";
				sql += " " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processBasePrice() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("기초금액");
		
		processBasePrice("물품");
		processBasePrice("공사");
		processBasePrice("용역");
		processBasePrice("외자");
	}
	
	public void processBasePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "예정개찰일시 BETWEEN \"" + sd + "\" AND \"" + ed + "\" AND "
				+ "공고완료=1 AND 기초완료=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			String path = buildItemPath(bidNum);
			
			if (tracker != null) tracker.updateProgress();
			
			Document doc = getResponse(path);
			int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
			
			Elements items = doc.getElementsByTag("item");
			for (int i = 0; i < totalCount; i++) {
				Element item = items.get(i);
				
				String bidver = item.getElementsByTag("입찰공고차수").text();
				String series = item.getElementsByTag("입찰분류").text();
				String level = item.getElementsByTag("난이도계수").text();
				String basePrice = item.getElementsByTag("기초예정가격").text();
				if (basePrice.equals("")) basePrice = "0";
				String lower = item.getElementsByTag("예비가격범위from").text();
				String upper = item.getElementsByTag("예비가격범위to").text();
				
				String where = "WHERE 입찰공고번호=\"" + bidNum + "\" AND 입찰공고차수=\"" + bidver + "\" AND 입찰분류=" + series;
				
				sql = "UPDATE narabidinfo SET 난이도계수=\"" + level + "\", "
						+ "기초예정가격=" + basePrice + ", "
						+ "하한=\"" + lower + "\", "
						+ "상한=\"" + upper + "\", 기초완료=1 " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processPrePrice() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("예비가격");
		
		processPrePrice("물품");
		processPrePrice("공사");
		processPrePrice("용역");
		processPrePrice("외자");
	}
	
	public void processPrePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND "
				+ "실제개찰일시 BETWEEN \"" + sd + "\" AND \"" + ed + "\" AND "
				+ "결과완료=1 AND 복수완료=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("입찰공고번호"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			String path = buildItemPath(bidNum);
			
			Document doc = getResponse(path);
			int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
			if (totalCount > 15) totalCount = 15;
			
			if (tracker != null) tracker.updateProgress();
			
			Elements items = doc.getElementsByTag("item");
			for (int i = 0; i < totalCount; i++) {
				Element item = items.get(i);
				
				String bidver = item.getElementsByTag("입찰공고차수").text();
				String series = item.getElementsByTag("입찰분류").text();
				String rebidno = item.getElementsByTag("재입찰번호").text();
				String index = item.getElementsByTag("일련번호").text();
				String priceNum = item.getElementsByTag("총예가갯수").text();
				if (priceNum.equals("")) priceNum = "0";
				String dupPrice = item.getElementsByTag("기초예정가격").text();
				if (dupPrice.equals("")) dupPrice = "0";
				String expPrice = item.getElementsByTag("예정가격금액").text();
				if (expPrice.equals("")) expPrice = "0";
				
				String where = "WHERE 입찰공고번호=\"" + bidNum + "\" AND 입찰공고차수=\"" + bidver + "\" AND 재입찰번호=" + rebidno + " AND 입찰분류=" + series;
				if (!index.equals("")) {
					sql = "UPDATE narabidinfo SET 총예가갯수=" + priceNum + ", "
							+ "복수" + index + "=" + dupPrice + ", "
							+ "예정가격=" + expPrice + ", 복수완료=1 " + where;
					System.out.println(sql);
					st.executeUpdate(sql);
				}
				else {
					sql = "UPDATE narabidinfo SET 총예가갯수=" + priceNum + ", "
							+ "예정가격=" + expPrice + ", 복수완료=1 " + where;
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
		}
		
		closeDB();
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
					+ "면허제한, 예가방법, 총예가갯수, 추첨예가갯수, 지명경쟁, 기초예정가격, 하한, 상한, 난이도계수, 업무, 공고완료, 기초완료 FROM narabidinfo " + where;
			
			rs = st.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt("공고완료") == 1) {
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
							+ "기초예정가격=" + rs.getString("기초예정가격") + ", "
							+ "하한=\"" + rs.getString("하한") + "\", "
							+ "상한=\"" + rs.getString("상한") + "\", "
							+ "난이도계수=\"" + rs.getString("난이도계수") + "\", "
							+ "업무=\"" + rs.getString("업무") + "\", "
							+ "공고완료=" + rs.getString("공고완료") + ", "
							+ "기초완료=" + rs.getString("기초완료") + " " + where;
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
		
		String sql = "SELECT DISTINCT 입찰공고번호 FROM narabidinfo WHERE 업무=\"" + t + "\" AND LEFT(계약방법, 2)=\"수의\" AND 결과완료=1 AND 복수완료=0";
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
	
	public int getTotal() {
		return totalItem;
	}

	public void run() {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tracker.finish();
		}
	}

	public int checkTotal() throws IOException {
		int total = 0;
		
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
