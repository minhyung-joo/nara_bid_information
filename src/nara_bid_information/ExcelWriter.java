package nara_bid_information;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelWriter {
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	Workbook workbook;
	Sheet sheet;
	HSSFCellStyle money;
	SimpleDateFormat sdf;
	
	String defaultPath;
	String basePath;
	String today;
	String workType;
	String org;
	String lowerBound;
	String upperBound;
	boolean negoFlag;
	
	public ExcelWriter(String org, String workType, String lowerBound, String upperBound) {
		defaultPath = "F:/";
		basePath = Resources.BASE_PATH;
		this.workType = workType;
		this.org = org;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		negoFlag = false;
		
		workbook = new HSSFWorkbook();
		sheet = workbook.createSheet("입찰정보");
		money = (HSSFCellStyle) workbook.createCellStyle();
		HSSFDataFormat moneyFormat = (HSSFDataFormat) workbook.createDataFormat();
		money.setDataFormat(moneyFormat.getFormat(BuiltinFormats.getBuiltinFormat(42)));
		sdf = new SimpleDateFormat("yyyy-MM-dd");
		today = sdf.format(new Date()) + " 00:00:00";
	}
	
	public void setNego(boolean nego) {
		negoFlag = nego;
	}
	
	public void connectDB() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = con.createStatement();
		System.out.println("DB Connected");
	}
	
	public void adjustColumns() {
		for (int i = 0; i < 53; i++) {
			if (i == 3) sheet.setColumnWidth(i, 1000);
			else {
				sheet.autoSizeColumn(i);
				if (sheet.getColumnWidth(i) > 3600) sheet.setColumnWidth(i, 3600);
			}
		}
		
		for (int i = 8; i < 37; i++) {
			if (i != 21) sheet.setColumnHidden(i, true);
		}
	}
	
	public void labelColumns() {
		int cellIndex = 0;
		
		Row columnNames = sheet.createRow(0);
		columnNames.createCell(cellIndex++).setCellValue("순번");
		columnNames.createCell(cellIndex++).setCellValue("입찰공고번호");
		columnNames.createCell(cellIndex++).setCellValue("실제개찰일시");
		columnNames.createCell(cellIndex++).setCellValue("업종제한사항");
		columnNames.createCell(cellIndex++).setCellValue("기초금액");
		columnNames.createCell(cellIndex++).setCellValue("예정가격");
		columnNames.createCell(cellIndex++).setCellValue("투찰금액");
		columnNames.createCell(cellIndex++).setCellValue("1");
		columnNames.createCell(cellIndex++).setCellValue("2");
		columnNames.createCell(cellIndex++).setCellValue("3");
		columnNames.createCell(cellIndex++).setCellValue("4");
		columnNames.createCell(cellIndex++).setCellValue("5");
		columnNames.createCell(cellIndex++).setCellValue("6");
		columnNames.createCell(cellIndex++).setCellValue("7");
		columnNames.createCell(cellIndex++).setCellValue("8");
		columnNames.createCell(cellIndex++).setCellValue("9");
		columnNames.createCell(cellIndex++).setCellValue("10");
		columnNames.createCell(cellIndex++).setCellValue("11");
		columnNames.createCell(cellIndex++).setCellValue("12");
		columnNames.createCell(cellIndex++).setCellValue("13");
		columnNames.createCell(cellIndex++).setCellValue("14");
		columnNames.createCell(cellIndex++).setCellValue("15");
		columnNames.createCell(cellIndex++).setCellValue("1");
		columnNames.createCell(cellIndex++).setCellValue("2");
		columnNames.createCell(cellIndex++).setCellValue("3");
		columnNames.createCell(cellIndex++).setCellValue("4");
		columnNames.createCell(cellIndex++).setCellValue("5");
		columnNames.createCell(cellIndex++).setCellValue("6");
		columnNames.createCell(cellIndex++).setCellValue("7");
		columnNames.createCell(cellIndex++).setCellValue("8");
		columnNames.createCell(cellIndex++).setCellValue("9");
		columnNames.createCell(cellIndex++).setCellValue("10");
		columnNames.createCell(cellIndex++).setCellValue("11");
		columnNames.createCell(cellIndex++).setCellValue("12");
		columnNames.createCell(cellIndex++).setCellValue("13");
		columnNames.createCell(cellIndex++).setCellValue("14");
		columnNames.createCell(cellIndex++).setCellValue("15");
		columnNames.createCell(cellIndex++).setCellValue("참가수");
		columnNames.createCell(cellIndex++).setCellValue("개찰일시(예정)");
		columnNames.createCell(cellIndex++).setCellValue("진행상황");
		columnNames.createCell(cellIndex++).setCellValue("재입찰");
		columnNames.createCell(cellIndex++).setCellValue("집행관");
		columnNames.createCell(cellIndex++).setCellValue("입회관");
		columnNames.createCell(cellIndex++).setCellValue("복수예비가격 작성시각");
		columnNames.createCell(cellIndex++).setCellValue("공고기관");
		columnNames.createCell(cellIndex++).setCellValue("수요기관");
		columnNames.createCell(cellIndex++).setCellValue("입찰방식");
		columnNames.createCell(cellIndex++).setCellValue("계약방식");
		columnNames.createCell(cellIndex++).setCellValue("업무");
		columnNames.createCell(cellIndex++).setCellValue("난이도계수");
		columnNames.createCell(cellIndex++).setCellValue("예가방법");
		columnNames.createCell(cellIndex++).setCellValue("예비가격");
	}
	
	public void toFile() throws IOException {
		String name = "";
		if (negoFlag) { name += "협상건 "; }
		if (org != null) { name += org; }
		if (workType == null) { name += "(전체)"; }
		else { name += "(" + workType + ")"; }
		
		int dupIndex = 2;
		File file = new File(defaultPath);
		FileOutputStream fos = null;
		String filePath;
		if (file.exists()) {
			filePath = defaultPath + name + ".xls";
			file = new File(filePath);
			while (file.exists() && !file.isDirectory()) {
				filePath = defaultPath + name + "-" + dupIndex + ".xls";
				file = new File(filePath);
				dupIndex++;
			}
		}
		else {
			filePath = basePath + name + ".xls";
			file = new File(filePath);
			while (file.exists() && !file.isDirectory()) {
				filePath = basePath + name + "-" + dupIndex + ".xls";
				file = new File(filePath);
				dupIndex++;
			}
		}
		fos = new FileOutputStream(filePath);
		workbook.write(fos);
		fos.close();
	}
	
	public void toExcel() throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		labelColumns();
		
		String sql = "SELECT * FROM narabidinfo WHERE ";
		if (negoFlag) {
			sql += "협상건=1 AND ";
		}
		if (!workType.equals("전체")) {
			sql += "업무=\"" + workType + "\" AND ";
		}
		if (org != null && !org.equals("")) {
			sql += "발주기관=\"" + org + "\" AND ";
		}
		if ( (lowerBound != null) && (upperBound != null) ) {
			sql += "하한수=" + lowerBound + " AND 상한수=" + upperBound + " AND "; 
		}
		sql += "결과완료=1 ";
		
		sql += "UNION SELECT * FROM narabidinfo WHERE ";
		if (negoFlag) {
			sql += "협상건=1 AND ";
		}
		if (!workType.equals("전체")) {
			sql += "업무=\"" + workType + "\" AND ";
		}
		if (org != null && !org.equals("")) {
			sql += "발주기관=\"" + org + "\" AND ";
		}
		if ( (lowerBound != null) && (upperBound != null) ) {
			sql += "하한수=" + lowerBound + " AND 상한수=" + upperBound + " AND "; 
		}
		sql += "예정개찰일시 >= \"" + today + "\" ORDER BY 예정개찰일시, 입찰공고번호;";
		
		System.out.println(sql);
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index);
			row.createCell(cellIndex++).setCellValue(rs.getString("입찰공고번호"));
			if (rs.getString("실제개찰일시") != null) {
				String od = rs.getString("실제개찰일시");
				od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
				row.createCell(cellIndex++).setCellValue(od);
			}
			else if (rs.getString("예정개찰일시") != null) {
				String dd = rs.getString("예정개찰일시");
				dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else {
				row.createCell(cellIndex++).setCellValue("-");
			}
			row.createCell(cellIndex++).setCellValue(rs.getString("면허제한"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("기초예정가격"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("예정가격"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("투찰금액"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("복수1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("복수2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("복수3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("복수4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("복수5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("복수6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("복수7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("복수8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("복수9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("복수10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("복수11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("복수12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("복수13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("복수14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("복수15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getInt("복참1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getInt("복참2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getInt("복참3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getInt("복참4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getInt("복참5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getInt("복참6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getInt("복참7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getInt("복참8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getInt("복참9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getInt("복참10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getInt("복참11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getInt("복참12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getInt("복참13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getInt("복참14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getInt("복참15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("참가자수"));
			if (rs.getString("예정개찰일시") != null) {
				String dd = rs.getString("예정개찰일시");
				dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else if (rs.getString("실제개찰일시") != null) {
				String od = rs.getString("실제개찰일시");
				od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
				row.createCell(cellIndex++).setCellValue(od);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			row.createCell(cellIndex++).setCellValue(rs.getString("진행구분코드"));
			String rebid = rs.getString("재입찰허용여부");
			if (rebid.equals("Y")) {
				String reprice = rs.getString("예비가격재작성여부");
				if (reprice.equals("재입찰시 예비가격을 다시 생성하여 예정가격이 산정됩니다.")) {
					row.createCell(cellIndex++).setCellValue("재생성");
				}
				else if (reprice.equals("재입찰시 기존 예비가격을 사용하여 예정가격이 산정됩니다.")) {
					row.createCell(cellIndex++).setCellValue("기존");
				}
				else {
					row.createCell(cellIndex++).setCellValue("재입찰허용");
				}
			}
			else {
				row.createCell(cellIndex++).setCellValue("없음");
			}
			row.createCell(cellIndex++).setCellValue(rs.getString("집행관"));
			row.createCell(cellIndex++).setCellValue(rs.getString("담당자"));
			if (rs.getString("복수예가작성일시") != null) {
				String dd = rs.getString("복수예가작성일시");
				dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else {
				row.createCell(cellIndex++).setCellValue("-");
			}
			row.createCell(cellIndex++).setCellValue(rs.getString("발주기관"));
			row.createCell(cellIndex++).setCellValue(rs.getString("수요기관"));
			row.createCell(cellIndex++).setCellValue(rs.getString("입찰방식"));
			row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
			row.createCell(cellIndex++).setCellValue(rs.getString("업무"));
			row.createCell(cellIndex++).setCellValue(rs.getString("난이도계수"));
			row.createCell(cellIndex++).setCellValue(rs.getString("예가방법"));
			String rate = rs.getString("하한수") + " ~ " + rs.getString("상한수");                
			if (rate.equals("null ~ null")) rate = "-";
			row.createCell(cellIndex++).setCellValue(rate);
			index++;
		}
		
		adjustColumns();
		
		toFile();
		
		System.out.println("File created.");
	}
}
