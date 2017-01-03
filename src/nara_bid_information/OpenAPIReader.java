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

public class OpenAPIReader {
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
	Type type; // 물품, 시설, 용역, 외자
	Option option; // 공고, 결과, 기초금액, 예비가격
	// String re; // 재입찰 flag
	
	public OpenAPIReader(String sd, String ed) {
		startDate = sd;
		endDate = ed;
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
	
	// Used to generate URL for getResponse() method.
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
			
			if (type == Type.PROD) path += EndPoints.PROD_TOTAL_NOTI;
			else if (type == Type.FACIL) path += EndPoints.FACIL_TOTAL_NOTI;
			else if (type == Type.SERV) path += EndPoints.SERV_TOTAL_NOTI;
			else if (type == Type.WEJA) path += EndPoints.WEJA_NOTI;
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
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
		case "시설":
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
		}
	}
	
	public void processNoti() throws IOException, ClassNotFoundException, SQLException {
		setOption("공고");
		
		processNoti("물품");
		processNoti("시설");
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
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			
			
		}
		
		closeDB();
	}
	
	public void processRes() throws IOException, ClassNotFoundException, SQLException {
		setOption("결과");
		
		processRes("물품");
		processRes("시설");
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
		
		Elements items = doc.getElementsByTag("item");
		for (int i = 0; i < totalCount; i++) {
			Element item = items.get(i);
			
			
		}
		
		closeDB();
	}
	
	public void processBasePrice() throws IOException, ClassNotFoundException, SQLException {
		setOption("기초금액");
		
		processBasePrice("물품");
		processBasePrice("시설");
		processBasePrice("용역");
		processBasePrice("외자");
	}
	
	public void processBasePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		/*
		 * Query a list of bid numbers and store in array.
		 */
		
		if (!bidNums.isEmpty()) {
			setType(t);
			
			for (int i = 0; i < bidNums.size(); i++) {
				String bidNum = bidNums.get(i);
				String path = buildItemPath(bidNum);
				
				Document doc = getResponse(path);
			}
		}
		
		closeDB();
	}
	
	public void processPrePrice() throws IOException, ClassNotFoundException, SQLException {
		setOption("예비가격");
		
		processPrePrice("물품");
		processPrePrice("시설");
		processPrePrice("용역");
		processPrePrice("외자");
	}
	
	public void processPrePrice(String t) throws IOException, ClassNotFoundException, SQLException {
		connectDB();
		
		ArrayList<String> bidNums = new ArrayList<String>();
		
		/*
		 * Query a list of bid numbers and store in array.
		 */
		
		if (!bidNums.isEmpty()) {
			setType(t);
			
			for (int i = 0; i < bidNums.size(); i++) {
				String bidNum = bidNums.get(i);
				String path = buildItemPath(bidNum);
				
				Document doc = getResponse(path);
			}
		}
		
		closeDB();
	}
}
