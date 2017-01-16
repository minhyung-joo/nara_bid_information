package nara_bid_information;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

@SuppressWarnings("serial")
public class PeriodicUpdater extends JFrame implements ProgressTracker {
	ProgressTracker reference;
	
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	ExecutorService es;
	ArrayList<OpenAPIReader> readers;
	ArrayList<Boolean> states;
	Timer auto;
	
	JCheckBox autoCheck;
	DatePicker startDate, endDate;
	JTextArea reps;
	JTable table;
	JLabel startTime, endTime;
	JButton manualProcess;
	
	String type;
	boolean running;
	ArrayList<String> dates;
	
	public PeriodicUpdater(String sd, String ed, String type) throws ParseException {
		super(type);
		
		reference = this;
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		readers = new ArrayList<OpenAPIReader>();
		states = new ArrayList<Boolean>();
		
		auto = new Timer();
		
		this.type = type;
		running = false;
		dates = new ArrayList<String>();
		
		autoCheck = new JCheckBox("자동");
		autoCheck.addActionListener(new AutoListener());
		
		if (sd != null && ed != null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date start = format.parse(sd);
			Date end = format.parse(ed);
			startDate = new JDatePicker(start);
			endDate = new JDatePicker(end);
		}
		else {
			if (type.equals("월별")) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				Date anchorDate = formatter.parse(Resources.START_DATE);
				startDate = new JDatePicker(anchorDate);
			}
			else startDate = new JDatePicker(Calendar.getInstance().getTime());
			endDate = new JDatePicker(Calendar.getInstance().getTime());
		}
		startDate.setTextfieldColumns(12);
		startDate.setTextEditable(true);
		endDate.setTextfieldColumns(12);
		endDate.setTextEditable(true);
		reps = new JTextArea(1, 5);
		reps.setText("60");
		JPanel recordStartPanel = new JPanel();
		startTime = new JLabel("처리시작: ");
		recordStartPanel.add(startTime);
		JPanel recordEndPanel = new JPanel();
		endTime = new JLabel("처리종료: ");
		recordEndPanel.add(endTime);
		JPanel buttonPanel = new JPanel();
		manualProcess = new JButton("수동처리");
		manualProcess.addActionListener(new UpdateListener());
		buttonPanel.add(manualProcess);
		
		JPanel datePanel = new JPanel();
		datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.PAGE_AXIS));
		
		JPanel sdPanel = new JPanel();
		sdPanel.add(new JLabel("시작일자 : "));
		sdPanel.add((JComponent) startDate);
		JPanel edPanel = new JPanel();
		edPanel.add(new JLabel("종료일자 : "));
		edPanel.add((JComponent) endDate);
		JPanel rePanel = new JPanel();
		rePanel.add(new JLabel("조회간격 : "));
		rePanel.add(reps);
		rePanel.add(new JLabel("초"));
		
		datePanel.add(sdPanel);
		datePanel.add(edPanel);
		datePanel.add(rePanel);
		datePanel.add(recordStartPanel);
		datePanel.add(recordEndPanel);
		datePanel.add(buttonPanel);
		
		table = new JTable(new DefaultTableModel(Resources.UPDATER_COLUMNS, 0));
		JScrollPane scroll = new JScrollPane(table);
		
		panel.add(autoCheck, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(datePanel, BorderLayout.SOUTH);
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    }
		});
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image icon = toolkit.getImage("nara.png");
		this.setIconImage(icon);
		this.add(panel);
		this.setSize(300, 600);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private class UpdateListener implements ActionListener {
		public ArrayList<String> queryByDay(String sd, String ed) throws ParseException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar sdate = Calendar.getInstance(); 
			sdate.setTime(sdf.parse(sd));
			Calendar edate = Calendar.getInstance();
			edate.setTime(sdf.parse(ed));
			ArrayList<String> dates = new ArrayList<String>();
			do {
				dates.add(sdf.format(sdate.getTime()));
				sdate.add(Calendar.DAY_OF_MONTH, 1);
				if (sdate.equals(edate)) {
					dates.add(sdf.format(sdate.getTime()));
				}
			} while (edate.after(sdate) && dates.size() < 31);
			return dates;
		}
		
		public ArrayList<String> queryByMonth(String sd, String ed) throws ParseException {
			SimpleDateFormat forq = new SimpleDateFormat("yyyy-MM");
			Calendar sdate = Calendar.getInstance(); 
			sdate.setTime(forq.parse(sd));
			Calendar edate = Calendar.getInstance();
			edate.setTime(forq.parse(ed));
			ArrayList<String> dates = new ArrayList<String>();
			do {
				dates.add(forq.format(sdate.getTime()));
				sdate.add(Calendar.MONTH, 1);
				if (sdate.equals(edate)) {
					dates.add(forq.format(sdate.getTime()));
				}
			} while (edate.after(sdate));
			return dates;
		}
		
		public void actionPerformed(ActionEvent e) {
			if (!running) {
				Calendar startCalendar = Calendar.getInstance();
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				startTime.setText("처리시작: " + dateFormatter.format(startCalendar.getTime()));
				endTime.setText("처리종료: ");
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				DefaultTableModel tm = (DefaultTableModel) table.getModel();
				String sd = "";
				String ed = "";
				es = Executors.newFixedThreadPool(100);
				
				if ((startDate.getModel().getValue() == null) || (endDate.getModel().getValue() == null)) {					
					JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
					return;
				}
				else {
					sd = sdf.format(startDate.getModel().getValue());
					ed = sdf.format(endDate.getModel().getValue());
				}
				
				try {
					if (type.equals("월별")) dates = queryByMonth(sd, ed);
					if (type.equals("일자별")) dates = queryByDay(sd, ed);
					
					Class.forName("com.mysql.jdbc.Driver");
					con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
					st = con.createStatement();
					rs = null;
					
					tm.setRowCount(0); // Empty the table.
					for (String sm : dates) {
						int dbcount = 0;
						int svcount = 0;
						
						OpenAPIReader reader = null;
						
						String em = "";
						
						if (type.equals("월별")) {
							Calendar sc = Calendar.getInstance();
							sm += "-01";
							sc.setTime(sdf.parse(sm));
							sc.add(Calendar.MONTH, 1);
							sc.add(Calendar.DAY_OF_MONTH, -1);
							em = sdf.format(sc.getTime());
							reader = new OpenAPIReader(sm, em, "결과", reference);
							//rs = st.executeQuery("SELECT COUNT(*) FROM narabidinfo WHERE 실제개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 결과완료=1;");
							rs = st.executeQuery("SELECT counter FROM naracounter WHERE openDate BETWEEN \"" + sm + "\" AND \"" + em + "\";");
						}
						else if (type.equals("일자별")) {
							reader = new OpenAPIReader(sm, sm, "결과", reference);
							//rs = st.executeQuery("SELECT COUNT(*) FROM narabidinfo WHERE 실제개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + sm + " 23:59:59\" AND 결과완료=1;");
							rs = st.executeQuery("SELECT counter FROM naracounter WHERE openDate=\"" + sm + "\"");
						}
						
						while (rs.next()) {
							dbcount += rs.getInt(1);
						}
						svcount = reader.checkTotal();
						if (type.equals("월별")) sm = sm.substring(0, sm.length()-3);
						int diff = svcount - dbcount;
						tm.addRow(new Object[] { sm, svcount, dbcount, diff });
						
						if (diff > 0) {
							if (type.equals("월별")) sm += "-01";
							readers.add(reader);
							es.submit(reader);
							states.add(false);
						}
						if (diff < 0) {
							reader.setOption("차수");
							readers.add(reader);
							es.submit(reader);
							states.add(false);
						}
					}
					
					if (states.isEmpty()) {
						finish();
						return;
					}
					else {
						running = true;
					}
				} catch (ClassNotFoundException | SQLException | ParseException | IOException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
			}
			else {
				System.out.println("Can't start now!");
			}
		}
	}
	
	private class AutoListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if ( (!running) && autoCheck.isSelected() ) {
				manualProcess.doClick();
			}
		}
	}

	public void process() {
		manualProcess.doClick();
	}
	
	public void updateProgress() {
		
	}

	public void finish() {
		if ( running && !states.isEmpty() ) {
			states.remove(0);
			
			if (states.isEmpty()) {
				System.out.println("Gracefully finished");
				
				Calendar endCalendar = Calendar.getInstance();
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				endTime.setText("처리종료: " + dateFormatter.format(endCalendar.getTime()));
				
				readers.clear();
				running = false;
			}
		}
		else {
			System.out.println("WTF finished");
			
			Calendar endCalendar = Calendar.getInstance();
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			endTime.setText("처리종료: " + dateFormatter.format(endCalendar.getTime()));
			readers.clear();
			running = false;
		}
	}
}

