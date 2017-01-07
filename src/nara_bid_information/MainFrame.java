package nara_bid_information;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

public class MainFrame extends JFrame {
	Preferences pref;
	
	JMenuBar menuBar;
	JMenu downloadMenu;
	JMenu viewMenu;
	JMenuItem notiDownload, resDownload, basePriceDownload, prePriceDownload, negoDownload, changeDownload;
	JMenuItem monthCheck, dayCheck;
	
	JTabbedPane dataTabs;
	JComponent bidPanel;
	JComponent negoPanel;
	
	public MainFrame() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		super();
		
		loadPreferences();
		
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		
		this.setLayout(new GridLayout(1, 1));
		
		initializeMenu();
		
		initializeTabs();
		
		adjustSize();
		
		this.setTitle("나라장터 입찰정보");
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void loadPreferences() {
		pref = Preferences.userRoot().node(this.getClass().getName());
		
		//Resources.initialize(pref.get("DB_ID", "root"), pref.get("DB_PW", "qldjel123"), 
		//		pref.get("SCHEMA", "bid_db"), pref.get("PATH", "C:/Users/owner/Documents/"));
	}

	public void initializeMenu() {
		menuBar = new JMenuBar();
		
		downloadMenu = new JMenu("다운로드");
		
		notiDownload = new JMenuItem("입찰공고");
		notiDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdaterFrame notiFrame = new UpdaterFrame("공고");
			}
		});
		resDownload = new JMenuItem("개찰결과");
		resDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdaterFrame resFrame = new UpdaterFrame("결과");
			}
		});
		basePriceDownload = new JMenuItem("기초금액");
		basePriceDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdaterFrame baseFrame = new UpdaterFrame("기초금액");
			}
		});
		prePriceDownload = new JMenuItem("복수가격");
		prePriceDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdaterFrame preFrame = new UpdaterFrame("예비가격");
			}
		});
		negoDownload = new JMenuItem("협상에의한건처리");
		changeDownload = new JMenuItem("공고변경/재입찰");
		
		downloadMenu.add(notiDownload);
		downloadMenu.add(resDownload);
		downloadMenu.add(basePriceDownload);
		downloadMenu.add(prePriceDownload);
		downloadMenu.add(negoDownload);
		downloadMenu.add(changeDownload);
		
		viewMenu = new JMenu("조회");
		
		monthCheck = new JMenuItem("월별개찰조회");
		dayCheck = new JMenuItem("일자별개찰조회");
		
		viewMenu.add(monthCheck);
		viewMenu.add(dayCheck);
		
		menuBar.add(downloadMenu);
		menuBar.add(viewMenu);
		
		this.setJMenuBar(menuBar);
	}
	
	public void initializeTabs() {
		dataTabs = new JTabbedPane();
		
		bidPanel = new BidPanel("입찰");
		negoPanel = new BidPanel("협상");
		dataTabs.addTab("입찰정보 조회", bidPanel);
		dataTabs.addTab("협상건 조회", negoPanel);
		this.add(dataTabs);
	}
	
	public void adjustSize() {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		d.setSize(d.getWidth() / 3, 100);
		d = Toolkit.getDefaultToolkit().getScreenSize();
		d.setSize(d.getWidth(), d.getHeight() - 50);
		this.setSize(d);
	}
}
