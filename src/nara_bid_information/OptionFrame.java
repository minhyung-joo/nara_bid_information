package nara_bid_information;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public class OptionFrame extends JFrame {
	
	JLabel topLabel;
	JTextArea dbid;
	JPasswordField dbpw;
	JTextArea basePath;
	JTextArea schema;
	JTextArea serverKey;
	JButton confirm;
	
	public OptionFrame() {
		super("설정");
		
		topLabel = new JLabel("환경설정");
		topLabel.setHorizontalAlignment(SwingConstants.CENTER);
		topLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		
		dbid = new JTextArea(1, 10);
		dbid.setText(Resources.DB_ID);
		dbid.setBorder(BorderFactory.createLineBorder(Color.black));
		dbpw = new JPasswordField(10);
		dbpw.setText(Resources.DB_PW);
		dbpw.setBorder(BorderFactory.createLineBorder(Color.black));
		schema = new JTextArea(1, 10);
		schema.setText(Resources.SCHEMA);
		schema.setBorder(BorderFactory.createLineBorder(Color.black));
		basePath = new JTextArea(1, 10);
		basePath.setText(Resources.BASE_PATH);
		basePath.setBorder(BorderFactory.createLineBorder(Color.black));
		serverKey = new JTextArea(1, 10);
		serverKey.setText(Resources.SERVER_KEY);
		serverKey.setBorder(BorderFactory.createLineBorder(Color.black));
		confirm = new JButton("확인");
		confirm.addActionListener(new ConfirmListener());
		
		JPanel idPanel = new JPanel();
		idPanel.add(new JLabel("MySQL ID : "));
		idPanel.add(dbid);
		JPanel pwPanel = new JPanel();
		pwPanel.add(new JLabel("MySQL PW : "));
		pwPanel.add(dbpw);
		JPanel schemaPanel = new JPanel();
		schemaPanel.add(new JLabel("DB Schema : "));
		schemaPanel.add(schema);
		JPanel pathPanel = new JPanel();
		pathPanel.add(new JLabel("저장폴더 : "));
		pathPanel.add(basePath);
		JPanel keyPanel = new JPanel();
		keyPanel.add(new JLabel("서버키 : "));
		keyPanel.add(serverKey);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
		centerPanel.add(idPanel);
		centerPanel.add(pwPanel);
		centerPanel.add(schemaPanel);
		centerPanel.add(pathPanel);
		centerPanel.add(keyPanel);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(confirm);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(topLabel, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
		
		this.setSize(550, 300);
		this.setResizable(false);
		this.add(mainPanel);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private class ConfirmListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Resources.setValues(dbid.getText(), new String(dbpw.getPassword()), schema.getText(), basePath.getText(), serverKey.getText());
			closeFrame();
		}
	}
	
	public void closeFrame() {
		this.dispose();
	}
}
