package nara_bid_information;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
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
		
		this.setTitle("�������� ��������");
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void loadPreferences() {
		pref = Preferences.userRoot().node(this.getClass().getName());
		
		Resources.initialize(pref.get("DB_ID", "root"), pref.get("DB_PW", "qldjel123"), 
				pref.get("SCHEMA", "bid_db"), pref.get("PATH", "C:/Users/owner/Documents/"));
	}

	public void initializeMenu() {
		menuBar = new JMenuBar();
		
		downloadMenu = new JMenu("�ٿ�ε�");
		
		notiDownload = new JMenuItem("��������");
		resDownload = new JMenuItem("�������");
		basePriceDownload = new JMenuItem("���ʱݾ�");
		prePriceDownload = new JMenuItem("��������");
		negoDownload = new JMenuItem("�������Ѱ�ó��");
		changeDownload = new JMenuItem("������/������");
		
		downloadMenu.add(notiDownload);
		downloadMenu.add(resDownload);
		downloadMenu.add(basePriceDownload);
		downloadMenu.add(prePriceDownload);
		downloadMenu.add(negoDownload);
		downloadMenu.add(changeDownload);
		
		viewMenu = new JMenu("��ȸ");
		
		monthCheck = new JMenuItem("����������ȸ");
		dayCheck = new JMenuItem("���ں�������ȸ");
		
		viewMenu.add(monthCheck);
		viewMenu.add(dayCheck);
		
		menuBar.add(downloadMenu);
		menuBar.add(viewMenu);
		
		this.setJMenuBar(menuBar);
	}
	
	public void initializeTabs() {
		dataTabs = new JTabbedPane();
		
		bidPanel = new BidPanel();
		dataTabs.addTab("������ ��ȸ", bidPanel);
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
