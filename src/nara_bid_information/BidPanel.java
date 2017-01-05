package nara_bid_information;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

public class BidPanel extends JPanel {
	JTable data;
	ArrayList<SearchOptionPanel> searchPanels;
	
	public BidPanel() {
		super();
		
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
	
	private class SearchOptionPanel extends JPanel {
		JComboBox<String> workDrop;
		JTextField orgInput;
		JButton orgSearch;
		JCheckBox rateCheck;
		JTextField upperInput, lowerInput;
		JButton searchButton, excelButton;
		
		public SearchOptionPanel() {
			super();
			
			workDrop = new JComboBox<String>();
			DefaultComboBoxModel model = new DefaultComboBoxModel(Resources.WORKS);
			workDrop.setModel(model);
			workDrop.setSelectedIndex(3);
			orgInput = new JTextField(15);
			orgSearch = new JButton("검색");
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
			this.add(orgSearch);
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
			public void actionPerformed(ActionEvent e) {
				
			}
		}
		
		private class ExcelListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
	}
}
