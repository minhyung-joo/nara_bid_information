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
	final String SERVER_KEY = "J0qA4h8ti9oPo90bJJ8COx%2BxiJ1AXL7dyffFfFGiHHVNKj2LWrFE1GJxJ2HdKmMfI%2BhSYKblaSLGnkAlvkW1gw%3D%3D";
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
		PRE_PRICE
	}
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	String startDate;
	String endDate;
	Type type; // ��ǰ, �ü�, �뿪, ����
	Option option; // ����, ���, ���ʱݾ�, ���񰡰�
	// String re; // ������ flag
	
	ProgressTracker tracker;
	int totalItem;
	
	public OpenAPIReader(String sd, String ed, String op, ProgressTracker pt) {
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
		case "�ü�":
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
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException {
		setOption("����");
		
		processNoti("��ǰ");
		processNoti("�ü�");
		processNoti("�뿪");
		processNoti("����");
	}
	
	public void processNoti(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		setType(t);
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		totalItem = Integer.parseInt(count.text());
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalItem; i++) {
			if (tracker != null) tracker.updateProgress(); 
			
			Element item = items.get(i);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("���������ȣ").first().text();
			String bidver = item.getElementsByTag("������������").first().text();
			String series = "0"; // �Ϸù�ȣ
			String category = "0"; // �����з�
			String rebidno = "0"; // ��������ȣ
			
			Elements seriesCheck = item.getElementsByTag("�Ϸù�ȣ");
			Elements buyCheck = item.getElementsByTag("���Ŵ��ǰ��ü");
			if ( (seriesCheck.size() > 0) && (buyCheck.size() > 0) ) {
				series = seriesCheck.first().text();
				String buyInfo = buyCheck.first().text();
				String buyInfos[] = buyInfo.split("##");
				for (int j = 0; j < buyInfos.length; j++) {
					String details[] = buyInfos[j].split("\\^");
					category = details[0];
					
					String tempwhere = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
							+ "������������=\"" + bidver + "\" AND "
							+ "��������ȣ=" + rebidno + " AND "
							+ "�Ϸù�ȣ=" + series + " AND "
							+ "�����з�=" + category;
						
					String sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + tempwhere;
					rs = st.executeQuery(sql);
					if (rs.next()) {
						int finished = rs.getInt("����Ϸ�");
						if (finished == 1) {
							complete = true;
						}
					}
					else {
						sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �Ϸù�ȣ, �����з�) VALUES ("
								+ "\"" + bidnum + "\", "
								+ "\"" + bidver + "\", "
								+ rebidno + ", "
								+ series + ", "
								+ category + ");";
						System.out.println(sql);
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String tempwhere = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
						+ "������������=\"" + bidver + "\" AND "
						+ "��������ȣ=" + rebidno + " AND "
						+ "�Ϸù�ȣ=" + series + " AND "
						+ "�����з�=" + category;
				
				String sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + tempwhere;
				rs = st.executeQuery(sql);
				if (rs.next()) {
					int finished = rs.getInt("����Ϸ�");
					if (finished == 1) {
						complete = true;
					}
				}
				else {
					sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �Ϸù�ȣ, �����з�) VALUES ("
							+ "\"" + bidnum + "\", "
							+ "\"" + bidver + "\", "
							+ rebidno + ", "
							+ series + ", "
							+ category + ");";
					System.out.println(sql);
					st.executeUpdate(sql);
				}
			}
			
			if (!complete) {
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
				
				String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
						+ "������������=\"" + bidver + "\"";
				
				String sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", "
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
						+ "�����=\"" + compType + "\", ����Ϸ�=1 " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processRes() throws IOException, ClassNotFoundException, SQLException {
		setOption("���");
		
		processRes("��ǰ");
		processRes("�ü�");
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
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			boolean complete = false;
			
			String bidnum = item.getElementsByTag("���������ȣ").text();
			String bidver = item.getElementsByTag("������������").text();
			String rebidno = item.getElementsByTag("��������ȣ").text();
			String category = item.getElementsByTag("�����з�").text();
			String series = (type == Type.PROD) ? "1" : "0";
			
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
				sql = "INSERT INTO narabidinfo (���������ȣ, ������������, ��������ȣ, �Ϸù�ȣ, �����з�) VALUES ("
						+ "\"" + bidnum + "\", "
						+ "\"" + bidver + "\", "
						+ rebidno + ", "
						+ series + ", "
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
						+ "�����ڼ�=\"" + comp + "\", ����Ϸ�=1 " + where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
		
		closeDB();
	}
	
	public void processBasePrice() throws IOException, ClassNotFoundException, SQLException {
		setOption("���ʱݾ�");
		
		processBasePrice("��ǰ");
		processBasePrice("�ü�");
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
				+ "�������=\"��������\" AND ����Ϸ�=1 AND ���ʿϷ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
		}
		
		for (String bidNum : bidNums) {
			String path = buildItemPath(bidNum);
			
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
		setOption("���񰡰�");
		
		processPrePrice("��ǰ");
		processPrePrice("�ü�");
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
				+ "���񰡰��������翩��=\"Y\" AND ����Ϸ�=1 AND �����Ϸ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
		}
		
		for (String bidNum : bidNums) {
			String path = buildItemPath(bidNum);
			
			Document doc = getResponse(path);
			int totalCount = Integer.parseInt(doc.getElementsByTag("totalCount").text());
			
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tracker.finish();
		}
	}
}
