package nara_bid_information;

public class Resources {
	// DB authentication info.
	static String DB_ID = "root";
	static String DB_PW = "qldjel123";
		
	static String SCHEMA = "bid_db";
	static String BASE_PATH = "C:/Users/owner/Documents/";
	
	public static void initialize(String id, String pw, String schema, String path) {
		DB_ID = id;
		DB_PW = pw;
		SCHEMA = schema;
		BASE_PATH = path;
	}
	
	static String[] COLUMNS = { "", "���������ȣ", "���������Ͻ�", "�������ѻ���", "���ʱݾ�", "�����ݾ�", "�����ݾ�", "��÷����1", "��÷����15", "������", "�����Ͻ�(����)", "�����Ȳ", "������", "�����", "��ȸ��", "������", "������", "�������", "�����", "���̵����", "�������" };
	static String[] WORKS = { "��ǰ", "����", "�뿪", "��ü" };
}
