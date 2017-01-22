package nara_bid_information;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseManager {
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	public DatabaseManager() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = con.createStatement();
		System.out.println("DB Connected");
	}
	
	public static void main(String args[]) throws ClassNotFoundException, SQLException {
		DatabaseManager dbm = new DatabaseManager();
		dbm.resetCounter();
	}
	
	public void resetCounter() throws SQLException {
		ArrayList<String> dates = new ArrayList<String>();
		
		String sql = "SELECT 예정개찰일시 FROM narabidinfo WHERE 결과완료=1";
		rs = st.executeQuery(sql);
		
		while (rs.next()) {
			dates.add(rs.getString("예정개찰일시").substring(0, 10));
		}
		
		for (String date : dates) {
			sql = "SELECT openDate FROM naracounter WHERE openDate=\"" + date + "\"";
			rs = st.executeQuery(sql);
			
			if (rs.next()) {
				sql = "UPDATE naracounter SET counter=counter+1 WHERE openDate=\"" + date + "\"";
				System.out.println(sql);
				st.executeUpdate(sql);
			}
			else {
				sql = "INSERT INTO naracounter (openDate, counter) VALUES (\"" + date + "\", 1)";
				System.out.println(sql);
				st.executeUpdate(sql);
			}
		}
	}
}
