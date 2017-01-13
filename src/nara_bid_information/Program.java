package nara_bid_information;

import javax.swing.UnsupportedLookAndFeelException;

public class Program {
	public static void main(String args[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		@SuppressWarnings("unused")
		MainFrame frame = new MainFrame();
		Resources.initialize();
	}
}
