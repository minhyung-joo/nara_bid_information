package nara_bid_information;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Resources {
	// DB authentication info.
	static String DB_ID = "root";
	static String DB_PW = "qldjel123";
		
	static String SCHEMA = "bid_db";
	static String BASE_PATH = "C:/Users/Minhyung Joo/workspace/nara_bid_information/excel/";
	static String SERVER_KEY = "J0qA4h8ti9oPo90bJJ8COx%2BxiJ1AXL7dyffFfFGiHHVNKj2LWrFE1GJxJ2HdKmMfI%2BhSYKblaSLGnkAlvkW1gw%3D%3D";
	
	public static void initialize() {
		FileReader fr;
		try {
			fr = new FileReader("config.ini");
			BufferedReader br = new BufferedReader(fr);
			
			DB_ID = br.readLine().split("=")[1];
			DB_PW = br.readLine().split("=")[1];
			SCHEMA = br.readLine().split("=")[1];
			BASE_PATH = br.readLine().split("=")[1];
			SERVER_KEY = br.readLine().split("=")[1];
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String parseRate(String rate) {
		if (rate.length() < 4) return rate;
		else {
			if (rate.charAt(rate.length() - 1) == '0') {
				rate = rate.substring(0, rate.length() - 1);
			}
			return rate;
		}
	}
	
	final static String[] COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "재입찰", "집행관", "입회관", "공고기관", "수요기관", "입찰방식", "계약방식", "난이도계수", "예가방법" };
	final static String[] WORKS = { "물품", "공사", "용역", "전체" };
	
	final static String[] UPDATER_COLUMNS = { "개찰일시", "사이트", "데이터베이스", "차수" };
	
	final static String START_DATE = "2013-12-01";

	public static void setValues(String id, String pw, String schema, String path, String key) {
		DB_ID = id;
		DB_PW = pw;
		SCHEMA = schema;
		BASE_PATH = path;
		SERVER_KEY = key;
		
		try {
			FileWriter fw = new FileWriter("config.ini");
			
			fw.write("db_id="+id+"\n");
			fw.write("db_pw="+pw+"\n");
			fw.write("schema="+schema+"\n");
			fw.write("base_path="+path+"\n");
			fw.write("serv_key="+key+"\n");
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
