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
	Type type; // ��ǰ, ����, �뿪, ����
	Option option; // ����, ���, ���ʱݾ�, ���񰡰�
	// String re; // ������ flag
	
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
		OpenAPIReader tester = new OpenAPIReader("20131201", "20170121", "����", null);
		
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
		case "���������":
			type = Type.REWEJA;
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
		case "����":
			option = Option.DIFF;
			break;
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("����");
		
		if (!restarted) {
			processNoti("��ǰ");
			processNoti("����");
			processNoti("�뿪");
			processNoti("����");
		}
		else {
			if (savedType == Type.PROD) {
				processNoti("��ǰ");
				processNoti("����");
				processNoti("�뿪");
				processNoti("����");
			}
			else if (savedType == Type.FACIL) {
				processNoti("����");
				processNoti("�뿪");
				processNoti("����");
			}
			else if (savedType == Type.FACIL) {
				processNoti("�뿪");
				processNoti("����");
			}
			else {
				processNoti("����");
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
			
			String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // ���������ȣ
			String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // ������������
			String rebidno = "0"; // ��������ȣ
			String category = (type == Type.PROD) ? "1" : "0"; // �����з�
			String openDate = item.getElementsByTag("opengDt").text(); // �����Ͻ�
			String notiType = item.getElementsByTag("ntceKindNm").text(); // ��������
			String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // ����ڸ�
			String notiorg = item.getElementsByTag("ntceInsttNm").text(); // ��������
			String demorg = item.getElementsByTag("dminsttNm").text(); // ��������
			String rebid = item.getElementsByTag("rbidPermsnYn").text(); // ��������뿩��
			String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // �������
			String exec = item.getElementsByTag("exctvNm").text(); // �������
			String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // �ѿ�������
			String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // ��÷������
			String bidType = item.getElementsByTag("bidMethdNm").text(); // ������ĸ�
			String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // �����
			String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // �������ۼ�����
			String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // �������
			String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // ����������
			String license = ""; // ��������
			
			Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // ���Ŵ��ǰ���
			if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
				String buyInfo = buyCheck.first().text();
				buyInfo = buyInfo.replaceAll(",", "");
				String buyInfos[] = buyInfo.split("\\]");
				for (int j = 0; j < buyInfos.length; j++) {
					complete = false;
					
					String detail = buyInfos[j];
					detail = detail.substring(1);
					category = detail.split("\\^")[0];
					
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
						+ "����������=\"" + bidRate + "\", "
						+ "��������=\"" + license + "\", "
						+ "�����=\"" + compType + "\", ����Ϸ�=1";
				if (compType.contains("����")) sql += ", �����=1";
				if (rebid.equals("N")) sql += ", �������Ϸ�=1";
				if (!priceMethod.equals("��������")) sql += ", ���ʿϷ�=1";
				if (priceMethod.equals("�񿹰�") || priceMethod.equals("")) sql += ", �����Ϸ�=1";
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
			
			String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // ���������ȣ
			String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // ������������
			String rebidno = "0"; // ��������ȣ
			String category = (type == Type.PROD) ? "1" : "0"; // �����з�
			String openDate = item.getElementsByTag("opengDt").text(); // �����Ͻ�
			String notiType = item.getElementsByTag("ntceKindNm").text(); // ��������
			String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // ����ڸ�
			String notiorg = item.getElementsByTag("ntceInsttNm").text(); // ��������
			String demorg = item.getElementsByTag("dminsttNm").text(); // ��������
			String rebid = item.getElementsByTag("rbidPermsnYn").text(); // ��������뿩��
			String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // �������
			String exec = item.getElementsByTag("exctvNm").text(); // �������
			String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // �ѿ�������
			String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // ��÷������
			String bidType = item.getElementsByTag("bidMethdNm").text(); // ������ĸ�
			String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // �����
			String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // �������ۼ�����
			String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // �������
			String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // ����������
			String license = ""; // ��������
			
			Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // ���Ŵ��ǰ���
			if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
				String buyInfo = buyCheck.first().text();
				buyInfo = buyInfo.replaceAll(",", "");
				String buyInfos[] = buyInfo.split("\\]");
				for (int j = 0; j < buyInfos.length; j++) {
					complete = false;
					
					String detail = buyInfos[j];
					detail = detail.substring(1);
					category = detail.split("\\^")[0];
					
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
						+ "����������=\"" + bidRate + "\", "
						+ "��������=\"" + license + "\", "
						+ "�����=\"" + compType + "\", ����Ϸ�=1";
				if (compType.contains("����")) sql += ", �����=1";
				if (rebid.equals("N")) sql += ", �������Ϸ�=1";
				if (!priceMethod.equals("��������")) sql += ", ���ʿϷ�=1";
				if (priceMethod.equals("�񿹰�") || priceMethod.equals("")) sql += ", �����Ϸ�=1";
				sql += " " + where;
				
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
	}
	
	public void processRes() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("���");
		
		if (!restarted) {
			processRes("��ǰ");
			processRes("����");
			processRes("�뿪");
			processRes("����");
		}
		else {
			if (savedType == Type.PROD) {
				processRes("��ǰ");
				processRes("����");
				processRes("�뿪");
				processRes("����");
			}
			else if (savedType == Type.FACIL) {
				processRes("����");
				processRes("�뿪");
				processRes("����");
			}
			else if (savedType == Type.FACIL) {
				processRes("�뿪");
				processRes("����");
			}
			else {
				processRes("����");
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
			
			String bidnum = item.getElementsByTag("bidNtceNo").text(); // ���������ȣ
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // ������������
			String rebidno = item.getElementsByTag("rbidNo").text(); // ��������ȣ
			String category = item.getElementsByTag("bidClsfcNo").text(); // �����з�
			
			String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
					+ "������������=\"" + bidver + "\" AND "
					+ "��������ȣ=" + rebidno + " AND "
					+ "�����з�=" + category;
				
			String sql = "SELECT ����Ϸ�, ����Ϸ�, ���ʿϷ�, �����Ϸ� FROM narabidinfo " + where;
			rs = st.executeQuery(sql);
			if (rs.next()) {
				int finished = rs.getInt("����Ϸ�");
				int notiFinished = rs.getInt("����Ϸ�");
				int baseFinished = rs.getInt("���ʿϷ�");
				int preFinished = rs.getInt("�����Ϸ�");
				
				if (finished == 1) complete = true;
				if (notiFinished == 1) getNoti = true;
				if (baseFinished == 1) getBase = true;
				if (preFinished == 1) getPre = true;
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
				String openDate = item.getElementsByTag("opengDt").text(); // ���������Ͻ�
				String realDate = item.getElementsByTag("inptDt").text(); // ���������Ͻ�
				String winner = item.getElementsByTag("opengCorpInfo").text(); // ������ü����
				String result = item.getElementsByTag("progrsDivCdNm").text(); // ���౸���ڵ�
				String comp = item.getElementsByTag("prtcptCnum").text(); // �����ڼ�
				String notiOrg = item.getElementsByTag("ntceInsttNm").text(); // ���ֱ��
				String demOrg = item.getElementsByTag("dminsttNm").text(); // ������
				
				String bidPrice = "0";
				String winnerInfo[] = winner.split("\\^");
				if (winnerInfo.length == 5) bidPrice = winnerInfo[3];
				if (winnerInfo.length == 3) bidPrice = winnerInfo[1];
				if (bidPrice.equals("")) bidPrice = "0";
				if (!Resources.isNumeric(bidPrice)) bidPrice = "0";
				
				sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", "
						+ "���������Ͻ�=\"" + realDate + "\", "
						+ "���౸���ڵ�=\"" + result + "\", "
						+ "����=\"" + t + "\", "
						+ "���ֱ��=\"" + notiOrg + "\", "
						+ "������=\"" + demOrg + "\", "
						+ "�����ݾ�=\"" + bidPrice + "\", "
						+ "�����ڼ�=\"" + comp + "\", ����Ϸ�=1";
				if (result.equals("����")) sql += ", �����Ϸ�=1, �������Ϸ�=1";
				if (result.equals("�����Ϸ�")) sql += ", �������Ϸ�=1";
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
				setOption("����");
				processNoti(t, bidnum);
				setOption("���");
			}
			
			if (getBase) {
				setOption("���ʱݾ�");
				processBasePrice(t, bidnum, false);
				setOption("���");
			}
			
			if (getPre) {
				setOption("���񰡰�");
				processPrePrice(t, bidnum, false);
				setOption("���");
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
		
		setOption("���ʱݾ�");
		
		if (!restarted) {
			processBasePrice("��ǰ");
			processBasePrice("����");
			processBasePrice("�뿪");
			processBasePrice("����");
		}
		else {
			if (savedType == Type.PROD) {
				processBasePrice("��ǰ");
				processBasePrice("����");
				processBasePrice("�뿪");
				processBasePrice("����");
			}
			else if (savedType == Type.FACIL) {
				processBasePrice("����");
				processBasePrice("�뿪");
				processBasePrice("����");
			}
			else if (savedType == Type.FACIL) {
				processBasePrice("�뿪");
				processBasePrice("����");
			}
			else {
				processBasePrice("����");
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
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "����Ϸ�=1 AND ���ʿϷ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
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
			
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // ������������
			String series = item.getElementsByTag("bidClsfcNo").text(); // �����з�
			String level = item.getElementsByTag("dfcltydgrCfcnt").text(); // ���̵����
			String basePrice = item.getElementsByTag("bssamt").text(); // ���ʿ�������
			if (basePrice.equals("")) basePrice = "0";
			String lower = item.getElementsByTag("rsrvtnPrceRngBgnRate").text(); // ����
			String upper = item.getElementsByTag("rsrvtnPrceRngEndRate").text(); // ����
			
			String where = "WHERE ���������ȣ=\"" + bidno + "\" AND ������������=\"" + bidver + "\" AND �����з�=" + series;
			
			String sql = "UPDATE narabidinfo SET ���̵����=\"" + level + "\", "
					+ "���ʿ�������=" + basePrice + ", "
					+ "���Ѽ�=\"" + lower + "\", "
					+ "���Ѽ�=\"" + upper + "\", ���ʿϷ�=1 " + where;
			System.out.println(sql);
			st.executeUpdate(sql);
		}
	}
	
	public void processPrePrice() throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		totalItem = 0;
		
		setOption("���񰡰�");
		
		if (!restarted) {
			processPrePrice("��ǰ");
			processPrePrice("����");
			processPrePrice("�뿪");
			processPrePrice("����");
		}
		else {
			if (savedType == Type.PROD) {
				processPrePrice("��ǰ");
				processPrePrice("����");
				processPrePrice("�뿪");
				processPrePrice("����");
			}
			else if (savedType == Type.FACIL) {
				processPrePrice("����");
				processPrePrice("�뿪");
				processPrePrice("����");
			}
			else if (savedType == Type.SERV) {
				processPrePrice("�뿪");
				processPrePrice("����");
			}
			else processPrePrice("����");
		}
	}
	
	public void processPrePrice(String t) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		savedType = type;
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "����Ϸ�=1 AND �����Ϸ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
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
			String bidver = item.getElementsByTag("bidNtceOrd").text(); // ������������
			String series = item.getElementsByTag("bidClsfcNo").text(); // �����з�
			String rebidno = item.getElementsByTag("rbidNo").text(); // ��������ȣ
			String index = item.getElementsByTag("compnoRsrvtnPrceSno").text(); // ����
			String drawNum = item.getElementsByTag("drwtNum").text(); // ��÷Ƚ��
			String priceNum = item.getElementsByTag("totRsrvtnPrceNum").text(); // �ѿ�������
			if (priceNum.equals("")) priceNum = "0";
			String dupPrice = item.getElementsByTag("bsisPlnprc").text(); // ��������
			if (dupPrice.equals("")) dupPrice = "0";
			String expPrice = item.getElementsByTag("plnprc").text(); // ��������
			if (expPrice.equals("")) expPrice = "0";
			String basePrice = item.getElementsByTag("bssamt").text(); // ���ʱݾ�
			if (basePrice.equals("")) basePrice = "0";
			String priceDate = item.getElementsByTag("compnoRsrvtnPrceMkngDt").text(); // �������� �ۼ��Ͻ�
			
			String sql = "";
			String where = "WHERE ���������ȣ=\"" + bidNum + "\" AND ������������=\"" + bidver + "\" AND ��������ȣ=" + rebidno + " AND �����з�=" + series;
			if (!index.equals("")) {
				sql = "UPDATE narabidinfo SET �ѿ�������=" + priceNum + ", "
						+ "����" + index + "=" + dupPrice + ", "
						+ "����" + index + "=" + drawNum + ", "
						+ "���ʿ�������=" + basePrice + ", "
						+ "��������=" + expPrice + " ";
				if (!priceDate.equals("")) sql += ", ���������ۼ��Ͻ�=\"" + priceDate + "\" ";
				if (index.equals(priceNum)) sql += ", �����Ϸ�=1 ";
				sql += where;
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			else {
				sql = "UPDATE narabidinfo SET �ѿ�������=" + priceNum + ", "
						+ "��������=" + expPrice + ", �����Ϸ�=1 " + where;
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
					+ "��������, �������, �ѿ�������, ��÷��������, �������, ���ʿ�������, ���Ѽ�, ���Ѽ�, ���̵����, ����, ����Ϸ�, ���ʿϷ� FROM narabidinfo " + where;
			
			rs = st.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt("����Ϸ�") == 1) {
					String lowerBound = rs.getString("���Ѽ�");
					String upperBound = rs.getString("���Ѽ�");
					
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
							+ "���ʿ�������=" + rs.getString("���ʿ�������") + ", ";
					if (lowerBound != null) sql += "���Ѽ�=\"" + rs.getString("���Ѽ�") + "\", ";
					if (upperBound != null) sql += "���Ѽ�=\"" + rs.getString("���Ѽ�") + "\", ";
					sql += "���̵����=\"" + rs.getString("���̵����") + "\", "
							+ "����=\"" + rs.getString("����") + "\", "
							+ "����Ϸ�=" + rs.getString("����Ϸ�") + ", "
							+ "���ʿϷ�=" + rs.getString("���ʿϷ�") + " " + where;
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
		
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND �����=1 AND ����Ϸ�=1 AND �����Ϸ�=0";
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
	
	public void processDiff() throws ClassNotFoundException, SQLException, IOException {
		totalItem = 0;
		
		setOption("���");
		
		processDiff("��ǰ");
		processDiff("����");
		processDiff("�뿪");
		processDiff("����");
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
		String sql = "SELECT ���������ȣ, ������������, ��������ȣ, �����з�, ���������Ͻ� FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND ����Ϸ�=1";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
			bidVers.add(rs.getString("������������"));
			rebidNums.add(rs.getString("��������ȣ"));
			categories.add(rs.getString("�����з�"));
			dates.add(rs.getString("���������Ͻ�"));
		}
		
		String path = buildDatePath();
		Document doc = getResponse(path);
		Element count = doc.getElementsByTag("totalCount").first();
		int totalCount = Integer.parseInt(count.text());
		totalItem += totalCount;
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			
			String bidnum = item.getElementsByTag("���������ȣ").text();
			String bidver = item.getElementsByTag("������������").text();
			String rebidno = item.getElementsByTag("��������ȣ").text();
			String category = item.getElementsByTag("�����з�").text();
			
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
			String where = "WHERE ���������ȣ=\"" + bidNums.get(i) + "\" AND "
					+ "������������=\"" + bidVers.get(i) + "\" AND "
					+ "��������ȣ=" + rebidNums.get(i) + " AND "
					+ "�����з�=" + categories.get(i);
			
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
		
		setOption("����");
		
		processIncomplete("��ǰ");
		processIncomplete("����");
		processIncomplete("�뿪");
		processIncomplete("����");
	}
	
	public void processIncomplete(String t) throws SQLException, IOException, InterruptedException, ClassNotFoundException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		setType(t);
		
		String sd = parseDate(startDate);
		String ed = parseDate(endDate);
		String sql = "SELECT DISTINCT ���������ȣ FROM narabidinfo WHERE ����=\"" + t + "\" AND "
				+ "���������Ͻ� BETWEEN \"" + sd + " 00:00:00\" AND \"" + ed + " 23:59:59\" AND "
				+ "����Ϸ�=1 AND ����Ϸ�=0;";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			bidNums.add(rs.getString("���������ȣ"));
		}
		
		totalItem += bidNums.size();
		for (String bidNum : bidNums) {
			sql = "UPDATE narabidinfo SET ����Ϸ�=0 WHERE ���������ȣ=\"" + bidNum + "\"";
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
				
				String bidnum = item.getElementsByTag("bidNtceNo").first().text(); // ���������ȣ
				String bidver = item.getElementsByTag("bidNtceOrd").first().text(); // ������������
				String rebidno = "0"; // ��������ȣ
				String category = (type == Type.PROD) ? "1" : "0"; // �����з�
				String openDate = item.getElementsByTag("opengDt").text(); // �����Ͻ�
				String notiType = item.getElementsByTag("ntceKindNm").text(); // ��������
				String damdang = item.getElementsByTag("ntceInsttOfclNm").text(); // ����ڸ�
				String notiorg = item.getElementsByTag("ntceInsttNm").text(); // ��������
				String demorg = item.getElementsByTag("dminsttNm").text(); // ��������
				String rebid = item.getElementsByTag("rbidPermsnYn").text(); // ��������뿩��
				String jimyung = item.getElementsByTag("dsgntCmptYn").text(); // �������
				String exec = item.getElementsByTag("exctvNm").text(); // �������
				String priceNumber = parseNumber(item.getElementsByTag("totPrdprcNum").text()); // �ѿ�������
				String selectNumber = parseNumber(item.getElementsByTag("drwtPrdprcNum").text()); // ��÷������
				String bidType = item.getElementsByTag("bidMethdNm").text(); // ������ĸ�
				String compType = item.getElementsByTag("cntrctCnclsMthdNm").text(); // �����
				String reprice = item.getElementsByTag("rsrvtnPrceReMkngMthdNm").text(); // �������ۼ�����
				String priceMethod = item.getElementsByTag("prearngPrceDcsnMthdNm").text(); // �������
				String bidRate = item.getElementsByTag("sucsfbidLwltRate").text(); // ����������
				
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
				
				Elements buyCheck = item.getElementsByTag("purchsObjPrdctList"); // ���Ŵ��ǰ���
				if ( (buyCheck.size() > 0) && buyCheck.text().length() > 2 ) {
					String buyInfo = buyCheck.first().text();
					buyInfo = buyInfo.replaceAll(",", "");
					String buyInfos[] = buyInfo.split("\\]");
					for (int j = 0; j < buyInfos.length; j++) {
						complete = false;
						
						String detail = buyInfos[j];
						detail = detail.substring(1);
						category = detail.split("\\^")[0];
						
						String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
								+ "������������=\"" + bidver + "\" AND "
								+ "��������ȣ=" + rebidno + " AND "
								+ "�����з�=" + category;
							
						sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + where;
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
					}
				}
				else {
					String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
							+ "������������=\"" + bidver + "\" AND "
							+ "��������ȣ=" + rebidno + " AND "
							+ "�����з�=" + category;
					
					sql = "SELECT ���������ȣ, ����Ϸ� FROM narabidinfo " + where;
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
				}
				
				if (!complete) {
					String where = "WHERE ���������ȣ=\"" + bidnum + "\" AND "
							+ "������������=\"" + bidver + "\" AND "
							+ "�����з�=" + category;
					
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
							+ "����������=\"" + bidRate + "\", "
							+ "��������=\"" + license + "\", "
							+ "�����=\"" + compType + "\", ����Ϸ�=1";
					if (compType.contains("����")) sql += ", �����=1";
					if (rebid.equals("N")) sql += ", �������Ϸ�=1";
					if (!priceMethod.equals("��������")) sql += ", ���ʿϷ�=1";
					if (priceMethod.equals("�񿹰�") || priceMethod.equals("")) sql += ", �����Ϸ�=1";
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
