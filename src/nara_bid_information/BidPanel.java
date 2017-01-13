package nara_bid_information;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("serial")
public class BidPanel extends JPanel {
	JTable data;
	ArrayList<SearchOptionPanel> searchPanels;
	String bidType;
	
	public BidPanel(String bidType) {
		super();
		
		this.bidType = bidType;
		
		this.setLayout(new BorderLayout());
		
		initializeTable();
		
		initializeSearchPanel();
	}
	
	public void initializeTable() {
		DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
		rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
		data = new JTable(new DefaultTableModel(Resources.COLUMNS, 0));
		for (int i = 0; i < data.getColumnCount(); i++) {
			data.getColumn(Resources.COLUMNS[i]).setCellRenderer(rightRender);
		}
		data.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
		        data.scrollRectToVisible(data.getCellRect(data.getRowCount() - 1, 0, true));
		    }
		});
		data.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
		data.setIntercellSpacing(new Dimension(1, 1));
		data.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		JScrollPane scroll = new JScrollPane(data);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(scroll, BorderLayout.CENTER);
	}
	
	public void initializeSearchPanel() {
		searchPanels = new ArrayList<SearchOptionPanel>();
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
		for (int i = 0; i < 10; i++) {
			SearchOptionPanel sop = new SearchOptionPanel();
			bottomPanel.add(sop);
			searchPanels.add(sop);
		}
		
		JScrollPane bottomScroll = new JScrollPane(bottomPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		bottomScroll.setPreferredSize(new Dimension(this.getWidth(), 400));
		
		this.add(bottomScroll, BorderLayout.SOUTH);
	}
	
	public void adjustColumns() {
		final TableColumnModel columnModel = data.getColumnModel();
		
		for (int i = 0; i < 21; i++) {
			int width = 50;
			for (int j = 0; j < data.getRowCount(); j++) {
				TableCellRenderer renderer = data.getCellRenderer(j, i);
				Component comp = data.prepareRenderer(renderer, j, i);
				width = Math.max(comp.getPreferredSize().width + 1, width);
			}
			DefaultTableCellRenderer leftRender = new DefaultTableCellRenderer();
			leftRender.setHorizontalAlignment(SwingConstants.LEFT);
			if ((i < 4) || (i > 9)) {
				columnModel.getColumn(i).setCellRenderer(leftRender);
			}
			
			if (i == 3) width = 50;
			if ( (i > 11) && (width > 100) ) width = 100;
			if (width > 150) width = 150;
			
			columnModel.getColumn(i).setPreferredWidth(width);
		}
	}
	
	private class SearchOptionPanel extends JPanel {
		JComboBox<String> workDrop;
		JTextField orgInput;
		JCheckBox rateCheck;
		JTextField upperInput, lowerInput;
		JButton searchButton, excelButton;
		
		public SearchOptionPanel() {
			super();
			
			workDrop = new JComboBox<String>();
			DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(Resources.WORKS);
			workDrop.setModel(model);
			workDrop.setSelectedIndex(3);
			orgInput = new JTextField(15);
			rateCheck = new JCheckBox();
			upperInput = new JTextField(4);
			upperInput.setText("0.00");
			lowerInput = new JTextField(4);
			lowerInput.setText("-3.00");
			searchButton = new JButton("검색");
			searchButton.addActionListener(new SearchListener());
			excelButton = new JButton("엑셀저장");
			excelButton.addActionListener(new ExcelListener());
			
			this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			this.add(new JLabel("구분 : "));
			this.add(workDrop);
			JLabel o = new JLabel("발주기관 : ");
			o.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
			this.add(o);
			this.add(orgInput);
			JLabel r = new JLabel("사정률 ");
			r.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
			this.add(r);
			this.add(rateCheck);
			this.add(upperInput);
			this.add(new JLabel(" ~ "));
			this.add(lowerInput);
			this.add(searchButton);
			this.add(excelButton);
		}
		
		private class SearchListener implements ActionListener {
			public String processNumber(String number) {
				if (number == null) number = "";
				if (!number.equals("") && !(number.equals("0") || number.equals("0.00"))) {
					double amount = Double.parseDouble(number);
					DecimalFormat formatter = new DecimalFormat("#,###");
					number = formatter.format(amount);
				}
				else number = "-";
				
				return number;
			}
			
			public void actionPerformed(ActionEvent e) {
				try {
					Class.forName("com.mysql.jdbc.Driver");
					Connection con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
					java.sql.Statement st = con.createStatement();
					ResultSet rs = null;
					
					String org = orgInput.getText();
					String type = workDrop.getSelectedItem().toString();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					String today = sdf.format(new Date()) + " 00:00:00";
					
					String sql = "SELECT 입찰공고번호, 실제개찰일시, 면허제한, 기초예정가격, 예정가격, 투찰금액, 복수1, 복수15, "
							+ "참가자수, 예정개찰일시, 진행구분코드, 재입찰허용여부, 집행관, 담당자, 발주기관, 수요기관, 입찰방식, 계약방법, "
							+ "예비가격재작성여부, 난이도계수, 상한 FROM narabidinfo WHERE ";
					if (bidType.equals("협상")) sql += "LEFT(계약방법, 2)=\"수의\" AND ";
					if (!org.equals("")) sql += "발주기관=\"" + org + "\" AND ";
					if (!type.equals("전체")) sql += "업무=\"" + type + "\" AND ";
					if (rateCheck.isSelected()) {
						String lowerBound = Resources.parseRate(lowerInput.getText());
						String upperBound = Resources.parseRate(upperInput.getText());
						
						sql += "상한=\"" + upperBound + "\" AND 하한=\"" + lowerBound + "\" AND ";
					}
					sql += "결과완료=1 ";
					
					sql += "UNION SELECT 입찰공고번호, 실제개찰일시, 면허제한, 기초예정가격, 예정가격, 투찰금액, 복수1, 복수15, "
							+ "참가자수, 예정개찰일시, 진행구분코드, 재입찰허용여부, 집행관, 담당자, 발주기관, 수요기관, 입찰방식, 계약방법, "
							+ "예비가격재작성여부, 난이도계수, 상한 FROM narabidinfo WHERE ";
					if (!org.equals("")) sql += "발주기관=\"" + org + "\" AND ";
					sql += "예정개찰일시 >= \"" + today + "\" ORDER BY 예정개찰일시, 입찰공고번호;";
					
					rs = st.executeQuery(sql);
					
					DefaultTableModel m = (DefaultTableModel) data.getModel();
					m.setRowCount(0);
					int index = 1;
					while (rs.next()) {
						String bidno = rs.getString("입찰공고번호");
						String openDate = "-";
						if (rs.getString("실제개찰일시") != null) {
							openDate = rs.getString("실제개찰일시");
							openDate = openDate.substring(2, 4) + openDate.substring(5, 7)
							+ openDate.substring(8, 10) + " " + openDate.substring(11, 16);
						}
						else if (rs.getString("예정개찰일시") != null) {
							openDate = rs.getString("예정개찰일시");
							openDate = openDate.substring(2, 4) + openDate.substring(5, 7)
							+ openDate.substring(8, 10) + " " + openDate.substring(11, 16);
						}
						String license = rs.getString("면허제한");
						String basePrice = processNumber(rs.getString("기초예정가격"));						
						String expPrice = processNumber(rs.getString("예정가격"));
						String bidPrice = processNumber(rs.getString("투찰금액"));
						String dupPrice1 = processNumber(rs.getString("복수1"));
						String dupPrice15 = processNumber(rs.getString("복수15"));
						String comp = processNumber(rs.getString("참가자수"));
						String planDate = "-";
						if (rs.getString("예정개찰일시") != null) {
							planDate = rs.getString("예정개찰일시");
							planDate = planDate.substring(2, 4) + planDate.substring(5, 7)
							+ planDate.substring(8, 10) + " " + planDate.substring(11, 16);
						}
						else if (rs.getString("실제개찰일시") != null) {
							planDate = rs.getString("실제개찰일시");
							planDate = planDate.substring(2, 4) + planDate.substring(5, 7)
							+ planDate.substring(8, 10) + " " + planDate.substring(11, 16);
						}
						String result = rs.getString("진행구분코드");
						String rebid = rs.getString("재입찰허용여부");
						if (rebid.equals("Y")) {
							String reprice = rs.getString("예비가격재작성여부");
							if (reprice.equals("재입찰시 예비가격을 다시 생성하여 예정가격이 산정됩니다.")) {
								rebid = "재생성";
							}
							else if (reprice.equals("재입찰시 기존 예비가격을 사용하여 예정가격이 산정됩니다.")) {
								rebid = "기존";
							}
							else {
								rebid = "재입찰허용";
							}
						}
						else {
							rebid = "없음";
						}
						String exec = rs.getString("집행관");
						String obs = rs.getString("담당자");
						String notiOrg = rs.getString("발주기관");
						String demOrg = rs.getString("수요기관");
						String bidType = rs.getString("입찰방식");
						String compType = rs.getString("계약방법");
						String level = rs.getString("난이도계수");
						String upper = rs.getString("상한");
						
						m.addRow(new Object[] { index, bidno, openDate, license, basePrice, expPrice, bidPrice, dupPrice1, dupPrice15,
								comp, planDate, result, rebid, exec, obs, notiOrg, demOrg, bidType, compType, level, upper });
						index++;
					}
					adjustColumns();
					
					con.close();
				} catch (ClassNotFoundException | SQLException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		private class ExcelListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				if (!orgInput.getText().equals("")) {
					ExcelWriter writer = new ExcelWriter(orgInput.getText(), workDrop.getSelectedItem().toString());
					try {
						writer.toExcel();
					} catch (ClassNotFoundException | SQLException | IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
}
