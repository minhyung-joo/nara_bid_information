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
	
	static String[] COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "재입찰", "집행관", "입회관", "공고기관", "수요기관", "입찰방식", "계약방식", "난이도계수", "예가방법" };
	static String[] WORKS = { "물품", "공사", "용역", "전체" };
}
