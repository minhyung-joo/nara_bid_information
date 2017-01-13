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
	
	final static String[] COLUMNS = { "", "���������ȣ", "���������Ͻ�", "�������ѻ���", "���ʱݾ�", "�����ݾ�", "�����ݾ�", "��÷����1", "��÷����15", "������", "�����Ͻ�(����)", "�����Ȳ", "������", "�����", "��ȸ��", "������", "������", "�������", "�����", "���̵����", "�������" };
	final static String[] WORKS = { "��ǰ", "����", "�뿪", "��ü" };
	
	final static String[] UPDATER_COLUMNS = { "�����Ͻ�", "����Ʈ", "�����ͺ��̽�", "����" };
	
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
