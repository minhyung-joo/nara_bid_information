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
	Type type; // ��ǰ, ����, �뿪, ����
	Option option; // ����, ���, ���ʱݾ�, ���񰡰�
	// String re; // ������ flag
	
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
		case "��ǰ":
			type = Type.PROD;
			break;
		case "����":
			type = Type.FACIL;
			break;
		case "�뿪":
			type = Type.SERV;
			break;
		case "����":
			type = Type.WEJA;
			break;
		}
	}
	
	public void setOption(String o) {
		switch(o) {
		case "����":
			option = Option.NOTI;
			break;
		case "���":
			option = Option.RES;
			break;
		case "���ʱݾ�":
			option = Option.BASE_PRICE;
			break;
		case "���񰡰�":
			option = Option.PRE_PRICE;
			break;
		case "���ں�":
			option = Option.PERIODIC;
			break;
		case "������":
			option = Option.REBID;
			break;
		case "����":
			option = Option.NEGO;
			break;
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("����");
		
		processNoti("��ǰ");
		processNoti("����");
		processNoti("�뿪");
		processNoti("����");
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
			
			String bidnum = item.getElementsByTag("���������ȣ").first().text();
			String bidver = item.getElementsByTag("������������").first().text();
			String rebidno = "0"; // ��������ȣ
			String category = "0"; // �����з�
			String openDate = parseDate(item.getElementsByTag("�����Ͻ�").text());
			String notiType = item.getElementsByTag("��������").text();
			String damdang = item.getElementsByTag("����ڸ�").text();
			String notiorg = item.getElementsByTag("���ֱ��").text();
			String demorg = item.getElementsByTag("��������").text();
			String rebid = item.getElementsByTag("��������뿩��").text();
			String jimyung = item.getElementsByTag("�������").text();
			String exec = item.getElementsByTag("�������").text();
			String priceNumber = parseNumber(item.getElementsByTag("�ѿ�������").text());
			String selectNumber = parseNumber(item.getElementsByTag("��÷��������").text());
			String bidType = item.getElementsByTag("�������").text();
			String compType = item.getElementsByTag("�������").text();
			String reprice = item.getElementsByTag("���񰡰����ۼ�����").text();
			String priceMethod = item.getElementsByTag("�������").text();
			
			String license = "";
			for (int k = 1; k <= 12; k++) {
				String key = "�������Ѹ�" + k;
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
			
			if (!notiType.equals("������")) {
				Elements buyCheck = item.getElementsByTag("���Ŵ��ǰ��ü");
				if ( buyCheck.size() > 0 ) {
					String buyInfo = buyCheck.first().text();
					String buyInfos[] = buyInfo.split("##");
					for (int j = 0; j < buyInfos.length; j++) {
						String details[] = buyInfos[j].split("\\^");
						category = details[0];
						
						String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
								+ "������������=\"" + bidver + "\" AND "
								+ "��������ȣ=" + rebidno + " AND "
								+ "�����з�=" + category;
							
						String sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + where;
						rs = st.executeQuery(sql);
						if (rs.next()) {
							int finished = rs.getInt("����Ϸ�");
							if (finished == 1) {
								complete = true;
							}
						}
						else {
							sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �����з�) VALUES ("
									+ "\"" + bidnum + "\", "
									+ "\"" + bidver + "\", "
									+ rebidno + ", "
									+ category + ");";
							System.out.println(sql);
							st.executeUpdate(sql);
						}
						
						if (!complete) {
							sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", "
									+ "��������=\"" + notiType + "\", "
									+ "����=\"" + t + "\", "
									+ "�����=\"" + damdang + "\", "
									+ "���ֱ��=\"" + notiorg + "\", "
									+ "������=\"" + demorg + "\", "
									+ "��������뿩��=\"" + rebid + "\", "
									+ "���񰡰����ۼ�����=\"" + reprice + "\", "
									+ "�������=\"" + priceMethod + "\", "
									+ "�������=\"" + jimyung + "\", "
									+ "�����=\"" + exec + "\", "
									+ "�ѿ�������=" + priceNumber + ", "
									+ "��÷��������=" + selectNumber + ", "
									+ "�������=\"" + bidType + "\", "
									+ "��������=\"" + license + "\", "
									+ "�����=\"" + compType + "\", ����Ϸ�=1";
							if (rebid.equals("N")) sql += ", �������Ϸ�=1";
							if (!priceMethod.equals("��������")) sql += ", ���ʿϷ�=1";
							if (priceMethod.equals("�񿹰�")) sql += ", �����Ϸ�=1";
							sql += " " + where;
							
							System.out.println(sql);
							st.executeUpdate(sql);
						}
					}
				}
				else {
					String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
							+ "������������=\"" + bidver + "\" AND "
							+ "��������ȣ=" + rebidno + " AND "
							+ "�����з�=" + category;
					
					String sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + where;
					rs = st.executeQuery(sql);
					if (rs.next()) {
						int finished = rs.getInt("����Ϸ�");
						if (finished == 1) {
							complete = true;
						}
					}
					else {
						sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �����з�) VALUES ("
								+ "\"" + bidnum + "\", "
								+ "\"" + bidver + "\", "
								+ rebidno + ", "
								+ category + ");";
						System.out.println(sql);
						st.executeUpdate(sql);
					}
					
					if (!complete) {
						sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", "
								+ "��������=\"" + notiType + "\", "
								+ "����=\"" + t + "\", "
								+ "�����=\"" + damdang + "\", "
								+ "���ֱ��=\"" + notiorg + "\", "
								+ "������=\"" + demorg + "\", "
								+ "��������뿩��=\"" + rebid + "\", "
								+ "���񰡰����ۼ�����=\"" + reprice + "\", "
								+ "�������=\"" + priceMethod + "\", "
								+ "�������=\"" + jimyung + "\", "
								+ "�����=\"" + exec + "\", "
								+ "�ѿ�������=" + priceNumber + ", "
								+ "��÷��������=" + selectNumber + ", "
								+ "�������=\"" + bidType + "\", "
								+ "��������=\"" + license + "\", "
								+ "�����=\"" + compType + "\", ����Ϸ�=1";
						if (rebid.equals("N")) sql += ", �������Ϸ�=1";
						if (!priceMethod.equals("��������")) sql += ", ���ʿϷ�=1";
						if (priceMethod.equals("�񿹰�") || priceMethod.equals("")) sql += ", �����Ϸ�=1";
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
		
		setOption("���");
		
		processRes("��ǰ");
		processRes("����");
		processRes("�뿪");
		processRes("����");
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
			
			String bidnum = item.getElementsByTag("���������ȣ").text();
			String bidver = item.getElementsByTag("������������").text();
			String rebidno = item.getElementsByTag("��������ȣ").text();
			String category = item.getElementsByTag("�����з�").text();
			
			String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
					+ "������������=\"" + bidver + "\" AND "
					+ "��������ȣ=" + rebidno + " AND "
					+ "�����з�=" + category;
				
			String sql = "SELECT ����Ϸ� FROM narabidinfo " + where;
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int finished = rs.getInt("����Ϸ�");
				if (finished == 1) {
					complete = true;
				}
			}
			else {
				sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �����з�) VALUES ("
						+ "\"" + bidnum + "\", "
						+ "\"" + bidver + "\", "
						+ rebidno + ", "
						+ category + ");";
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			
			if (!complete) {
				String openDate = parseDate(item.getElementsByTag("�����Ͻ�").text());
				String hasPrice = item.getElementsByTag("���񰡰��������翩��").text();
				String winner = item.getElementsByTag("������ü����").text();
				String result = item.getElementsByTag("���౸���ڵ�").text();
				String comp = item.getElementsByTag("�����ڼ�").text();
				
				String winnerInfo[] = winner.split("###");
				String bidPrice = "0";
				if (winnerInfo.length == 3) bidPrice = winnerInfo[1];
				if (bidPrice.equals("")) bidPrice = "0";
				
				sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", "
						+ "���񰡰��������翩��=\"" + hasPrice + "\", "
						+ "���౸���ڵ�=\"" + result + "\", "
						+ "����=\"" + t + "\", "
						+ "�����ݾ�=\"" + bidPrice + "\", "
						+ "�����ڼ�=\"" + comp + "\", ����Ϸ�=1";
				if (result.equals("����")) sql += ", �����Ϸ�=1, �������Ϸ�=1";
				if (result.equals("�����Ϸ�")) sql += ", �������Ϸ�=1";
				sql += " " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processBasePrice() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("���ʱݾ�");
		
		processBasePrice("��ǰ");
		processBasePrice("����");
		processBasePrice("�뿪");
		processBasePrice("����");
	}
	
	public void processBasePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + "\" AND \"" + ed + "\" AND "
				+ "����Ϸ�=1 AND ���ʿϷ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
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
				
				String bidver = item.getElementsByTag("������������").text();
				String series = item.getElementsByTag("�����з�").text();
				String level = item.getElementsByTag("���̵����").text();
				String basePrice = item.getElementsByTag("���ʿ�������").text();
				if (basePrice.equals("")) basePrice = "0";
				String lower = item.getElementsByTag("���񰡰ݹ���from").text();
				String upper = item.getElementsByTag("���񰡰ݹ���to").text();
				
				String where = "WHERE ���������ȣ=\"" + bidNum + "\" AND ������������=\"" + bidver + "\" AND �����з�=" + series;
				
				sql = "UPDATE narabidinfo SET ���̵����=\"" + level + "\", "
						+ "���ʿ�������=" + basePrice + ", "
						+ "����=\"" + lower + "\", "
						+ "����=\"" + upper + "\", ���ʿϷ�=1 " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processPrePrice() throws IOException, ClassNotFoundException, SQLException {
		totalItem = 0;
		
		setOption("���񰡰�");
		
		processPrePrice("��ǰ");
		processPrePrice("����");
		processPrePrice("�뿪");
		processPrePrice("����");
	}
	
	public void processPrePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + "\" AND \"" + ed + "\" AND "
				+ "����Ϸ�=1 AND �����Ϸ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
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
				
				String bidver = item.getElementsByTag("������������").text();
				String series = item.getElementsByTag("�����з�").text();
				String rebidno = item.getElementsByTag("��������ȣ").text();
				String index = item.getElementsByTag("�Ϸù�ȣ").text();
				String priceNum = item.getElementsByTag("�ѿ�������").text();
				if (priceNum.equals("")) priceNum = "0";
				String dupPrice = item.getElementsByTag("���ʿ�������").text();
				if (dupPrice.equals("")) dupPrice = "0";
				String expPrice = item.getElementsByTag("�������ݱݾ�").text();
				if (expPrice.equals("")) expPrice = "0";
				
				String where = "WHERE ���������ȣ=\"" + bidNum + "\" AND ������������=\"" + bidver + "\" AND ��������ȣ=" + rebidno + " AND �����з�=" + series;
				if (!index.equals("")) {
					sql = "UPDATE narabidinfo SET �ѿ�������=" + priceNum + ", "
							+ "����" + index + "=" + dupPrice + ", "
							+ "��������=" + expPrice + ", �����Ϸ�=1 " + where;
					System.out.println(sql);
					st.executeUpdate(sql);
				}
				else {
					sql = "UPDATE narabidinfo SET �ѿ�������=" + priceNum + ", "
							+ "��������=" + expPrice + ", �����Ϸ�=1 " + where;
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
		
		String sql = "SELECT ���������ȣ, ������������, ��������ȣ, �����з� FROM narabidinfo WHERE ���౸���ڵ�=\"������\" AND �������Ϸ�=0";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
			bidVers.add(rs.getString("������������"));
			rebidNums.add(rs.getString("��������ȣ"));
			categories.add(rs.getString("�����з�"));
		}
		
		totalItem += bidNums.size();
		
		for (int i = 0; i < bidNums.size(); i++) {
			if (tracker != null) tracker.updateProgress();
			
			String bidno = bidNums.get(i);
			String bidver = bidVers.get(i);
			String rebidno = rebidNums.get(i);
			String category = categories.get(i);
			
			rebidno = "" + (Integer.parseInt(rebidno) + 1);
			
			sql = "SELECT ���������ȣ FROM narabidinfo WHERE ���������ȣ=\"" + bidno + "\" AND "
					+ "������������=\"" + bidver + "\" AND "
					+ "��������ȣ=" + rebidno + " AND "
					+ "�����з�=" + category;
			rs = st.executeQuery(sql);
			
			if (!rs.next()) {
				sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �����з�) VALUES "
						+ "(\"" + bidno + "\", \"" + bidver + "\", " + rebidno + ", " + category + ")";
				st.executeUpdate(sql);
			}
			
			rebidno = "" + (Integer.parseInt(rebidno) - 1);
			
			sql = "UPDATE narabidinfo SET �������Ϸ�=1 WHERE ���������ȣ=\"" + bidno + "\" AND "
					+ "������������=\"" + bidver + "\" AND "
					+ "��������ȣ=" + rebidno + " AND "
					+ "�����з�=" + category;
		}
		
		sql = "SELECT ���������ȣ, ������������, �����з� FROM narabidinfo WHERE ��������ȣ>0 AND ����Ϸ�=0 AND ���ʿϷ�=0";
		rs = st.executeQuery(sql);
		
		bidNums.clear();
		bidVers.clear();
		rebidNums.clear();
		categories.clear();
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
			bidVers.add(rs.getString("������������"));
			categories.add(rs.getString("�����з�"));
		}
		
		totalItem += bidNums.size();
		
		for (int i = 0; i < bidNums.size(); i++) {
			if (tracker != null) tracker.updateProgress();
			
			String bidno = bidNums.get(i);
			String bidver = bidVers.get(i);
			String category = bidVers.get(i);
			
			String where = "WHERE ���������ȣ=\"" + bidno + "\" AND "
					+ "������������=\"" + bidver + "\" AND "
					+ "�����з�=" + category;
			
			sql = "SELECT ���������Ͻ�, ���ֱ��, ������, �������, �����, �����, �����, ��������뿩��, ���񰡰����ۼ�����, "
					+ "��������, �������, �ѿ�������, ��÷��������, �������, ���ʿ�������, ����, ����, ���̵����, ����, ����Ϸ�, ���ʿϷ� FROM narabidinfo " + where;
			
			rs = st.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt("����Ϸ�") == 1) {
					sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + rs.getString("���������Ͻ�") + "\", "
							+ "���ֱ��=\"" + rs.getString("���ֱ��") + "\", "
							+ "������=\"" + rs.getString("������") + "\", "
							+ "�������=\"" + rs.getString("�������") + "\", "
							+ "�����=\"" + rs.getString("�����") + "\", "
							+ "�����=\"" + rs.getString("�����") + "\", "
							+ "�����=\"" + rs.getString("�����") + "\", "
							+ "��������뿩��=\"" + rs.getString("��������뿩��") + "\", "
							+ "���񰡰����ۼ�����=\"" + rs.getString("���񰡰����ۼ�����") + "\", "
							+ "��������=\"" + rs.getString("��������") + "\", "
							+ "�������=\"" + rs.getString("�������") + "\", "
							+ "�ѿ�������=" + rs.getString("�ѿ�������") + ", "
							+ "��÷��������=" + rs.getString("��÷��������") + ", "
							+ "�������=\"" + rs.getString("�������") + "\", "
							+ "���ʿ�������=" + rs.getString("���ʿ�������") + ", "
							+ "����=\"" + rs.getString("����") + "\", "
							+ "����=\"" + rs.getString("����") + "\", "
							+ "���̵����=\"" + rs.getString("���̵����") + "\", "
							+ "����=\"" + rs.getString("����") + "\", "
							+ "����Ϸ�=" + rs.getString("����Ϸ�") + ", "
							+ "���ʿϷ�=" + rs.getString("���ʿϷ�") + " " + where;
					st.executeUpdate(sql);
					
					break;
				}
			}
		}
		
		closeDB();
	}
	
	public void processNegoPrice() throws ClassNotFoundException, SQLException, IOException {
		totalItem = 0;
		
		setOption("���񰡰�");
		
		processNegoPrice("��ǰ");
		processNegoPrice("����");
		processNegoPrice("�뿪");
		processNegoPrice("����");
	}
	
	public void processNegoPrice(String t) throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND LEFT(�����, 2)=\"����\" AND ����Ϸ�=1 AND �����Ϸ�=0";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
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
				
				String bidver = item.getElementsByTag("������������").text();
				String series = item.getElementsByTag("�����з�").text();
				String rebidno = item.getElementsByTag("��������ȣ").text();
				String index = item.getElementsByTag("�Ϸù�ȣ").text();
				String priceNum = item.getElementsByTag("�ѿ�������").text();
				String dupPrice = item.getElementsByTag("���ʿ�������").text();
				if (dupPrice.equals("")) dupPrice = "0";
				String expPrice = item.getElementsByTag("�������ݱݾ�").text();
				if (expPrice.equals("")) expPrice = "0";
				
				if (!index.equals("")) {
					String where = "WHERE ���������ȣ=\"" + bidNum + "\" AND ������������=\"" + bidver + "\" AND ��������ȣ=" + rebidno + " AND �����з�=" + series;
					
					sql = "UPDATE narabidinfo SET �ѿ�������=\"" + priceNum + "\", "
							+ "����" + index + "=" + dupPrice + ", "
							+ "��������=" + expPrice + ", �����Ϸ�=1 " + where;
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
		
		setType("��ǰ");
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("����");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("�뿪");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		setType("����");
		path = buildDatePath();
		doc = getResponse(path);
		count = doc.getElementsByTag("totalCount").first();
		total += Integer.parseInt(count.text());
		
		return total;
	}
}
