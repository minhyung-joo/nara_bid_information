package nara_bid_information;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	Preferences pref;
	
	JMenuBar menuBar;
	JMenu downloadMenu, viewMenu, optionMenu;
	JMenuItem notiDownload, resDownload, basePriceDownload, prePriceDownload, negoDownload, changeDownload;
	JMenuItem monthCheck, dayCheck, settings;
	
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
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image icon = toolkit.getImage("nara.png");
		this.setIconImage(icon);
		this.setTitle("�������� ��������");
		this.setExtendedState(Frame.MAXIMIZED_BOTH);
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void loadPreferences() {
		//pref = Preferences.userRoot().node(this.getClass().getName());
		
		//Resources.initialize(pref.get("DB_ID", "root"), pref.get("DB_PW", "qldjel123"), 
		//		pref.get("SCHEMA", "bid_db"), pref.get("PATH", "C:/Users/owner/Documents/"));
	}

	public void initializeMenu() {
		menuBar = new JMenuBar();
		
		downloadMenu = new JMenu("�ٿ�ε�");
		
		notiDownload = new JMenuItem("��������");
		notiDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unused")
				UpdaterFrame notiFrame = new UpdaterFrame("����", null);
			}
		});
		resDownload = new JMenuItem("�������");
		resDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unused")
				UpdaterFrame resFrame = new UpdaterFrame("���", null);
			}
		});
		basePriceDownload = new JMenuItem("���ʱݾ�");
		basePriceDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Calendar today = Calendar.getInstance();
				today.add(Calendar.DAY_OF_MONTH, 1);
				Date anchorDate = today.getTime();
				
				@SuppressWarnings("unused")
				UpdaterFrame baseFrame = new UpdaterFrame("���ʱݾ�", anchorDate);
			}
		});
		prePriceDownload = new JMenuItem("���񰡰�");
		prePriceDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				Date anchorDate = null;
				try {
					anchorDate = formatter.parse(Resources.START_DATE);
				} catch (ParseException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
				
				@SuppressWarnings("unused")
				UpdaterFrame preFrame = new UpdaterFrame("���񰡰�", anchorDate);
			}
		});
		/*
		negoDownload = new JMenuItem("�������Ѱ�ó��");
		negoDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				Date anchorDate = null;
				try {
					anchorDate = formatter.parse(Resources.START_DATE);
				} catch (ParseException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
				
				@SuppressWarnings("unused")
				UpdaterFrame negoFrame = new UpdaterFrame("����", anchorDate);
			}
		});
		*/
		changeDownload = new JMenuItem("������/������");
		changeDownload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				Date anchorDate = null;
				try {
					anchorDate = formatter.parse(Resources.START_DATE);
				} catch (ParseException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
				
				@SuppressWarnings("unused")
				UpdaterFrame rebidFrame = new UpdaterFrame("������", anchorDate);
			}
		});
		
		downloadMenu.add(notiDownload);
		downloadMenu.add(resDownload);
		downloadMenu.add(basePriceDownload);
		downloadMenu.add(prePriceDownload);
		//downloadMenu.add(negoDownload);
		downloadMenu.add(changeDownload);
		
		viewMenu = new JMenu("��ȸ");
		
		monthCheck = new JMenuItem("����������ȸ");
		monthCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					@SuppressWarnings("unused")
					PeriodicUpdater monthUpdater = new PeriodicUpdater(null, null, "����");
				} catch (ParseException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
			}
		});
		dayCheck = new JMenuItem("���ں�������ȸ");
		dayCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					@SuppressWarnings("unused")
					PeriodicUpdater dayUpdater = new PeriodicUpdater(null, null, "���ں�");
				} catch (ParseException e1) {
					Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
					e1.printStackTrace();
				}
			}
		});
		
		viewMenu.add(monthCheck);
		viewMenu.add(dayCheck);
		
		optionMenu = new JMenu("����");
		
		settings = new JMenuItem("����");
		settings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unused")
				OptionFrame dayUpdater = new OptionFrame();
			}
		});
		
		optionMenu.add(settings);
		
		menuBar.add(downloadMenu);
		menuBar.add(viewMenu);
		menuBar.add(optionMenu);
		
		this.setJMenuBar(menuBar);
	}
	
	public void initializeTabs() {
		dataTabs = new JTabbedPane();
		
		bidPanel = new BidPanel("����");
		negoPanel = new BidPanel("����");
		dataTabs.addTab("�������� ��ȸ", bidPanel);
		dataTabs.addTab("����� ��ȸ", negoPanel);
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
