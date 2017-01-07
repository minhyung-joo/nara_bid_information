package nara_bid_information;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class UpdaterFrame extends JFrame implements ProgressTracker {
	ExecutorService executor;
	OpenAPIReader reader;
	ProgressTracker reference;
	
	DatePicker startDate, endDate;
	JCheckBox autoCheck;
	JLabel lastRun, startTime, endTime, progress;
	JButton manualProcess;
	JTextArea interval;
	String prevItem, curItem, totalItem;
	
	Timer autoProcessor;
	boolean running;
	
	String type;
	
	public UpdaterFrame(String type) {
		super();
		
		reference = this;
		this.type = type;
		autoProcessor = new Timer();
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel upperPanel = new JPanel();
		prevItem = "";
		lastRun = new JLabel("이전처리건수: " + prevItem);
		autoCheck = new JCheckBox("자동");
		autoCheck.addActionListener(new AutoListener());
		upperPanel.add(lastRun);
		upperPanel.add(autoCheck);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
		JPanel startRow = new JPanel();
		startDate = new JDatePicker(Calendar.getInstance().getTime());
		startDate.setTextfieldColumns(12);
		startDate.setTextEditable(true);
		startRow.add(new JLabel("시작일시: "));
		startRow.add((JComponent) startDate);
		JPanel endRow = new JPanel();
		endDate = new JDatePicker(Calendar.getInstance().getTime());
		endDate.setTextfieldColumns(12);
		endDate.setTextEditable(true);
		endRow.add(new JLabel("종료일시: "));
		endRow.add((JComponent) endDate);
		JPanel intervalRow = new JPanel();
		interval = new JTextArea(1, 5);
		interval.setText("60");
		intervalRow.add(new JLabel("조회간격: "));
		intervalRow.add(interval);
		JPanel recordStartPanel = new JPanel();
		startTime = new JLabel("처리시작: ");
		recordStartPanel.add(startTime);
		JPanel recordEndPanel = new JPanel();
		endTime = new JLabel("처리종료: ");
		recordEndPanel.add(endTime);
		JPanel buttonPanel = new JPanel();
		manualProcess = new JButton("수동처리");
		manualProcess.addActionListener(new ManualListener());
		buttonPanel.add(manualProcess);
		centerPanel.add(startRow);
		centerPanel.add(endRow);
		centerPanel.add(intervalRow);
		centerPanel.add(recordStartPanel);
		centerPanel.add(recordEndPanel);
		centerPanel.add(buttonPanel);
		
		JPanel bottomPanel = new JPanel();
		curItem = "0";
		totalItem = "0";
		progress = new JLabel(curItem + "/" + totalItem);
		bottomPanel.add(progress);
		
		mainPanel.add(upperPanel, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
		
		this.add(mainPanel);
		this.setSize(250, 300);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
		
		running = false;
	}
	
	private class ManualListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!running) {
				running = true;
				
				Calendar startCalendar = Calendar.getInstance();
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				startTime.setText("처리시작: " + dateFormatter.format(startCalendar.getTime()));
				endTime.setText("처리종료: ");
				
				SimpleDateFormat dateFormatter2 = new SimpleDateFormat("yyyyMMdd");
				String sd = dateFormatter2.format(startDate.getModel().getValue());
				String ed = dateFormatter2.format(endDate.getModel().getValue());
				
				reader = new OpenAPIReader(sd, ed, type, reference);
				try {
					executor = Executors.newFixedThreadPool(1);
					executor.submit(reader);
				} catch (Exception e1) {
					e1.printStackTrace();
					finish();
				}
			}
		}
	}
	
	private class AutoListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (autoCheck.isSelected()) {
				long delay = Long.parseLong(interval.getText()) * 1000;
				
				autoProcessor.schedule(new TimerTask() {
					public void run() {
						if (!running) manualProcess.doClick();
					}
				}, delay);
				
				if (!running) manualProcess.doClick();
			}
			else if (!autoCheck.isSelected()) {
				autoProcessor.cancel();
			}
		}
	}
	
	public void updateProgress() {
		int total = reader.getTotal();
		int cur = Integer.parseInt(curItem);
		cur++;
		curItem = "" + cur;
		totalItem = "" + total;
		progress.setText(curItem + "/" + totalItem);
	}

	public void finish() {
		running = false;
		prevItem = curItem;
		lastRun.setText("이전처리건수: " + prevItem);
		
		Calendar endCalendar = Calendar.getInstance();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		endTime.setText("처리종료: " + dateFormatter.format(endCalendar.getTime()));
	}
}
