package nara_bid_information;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.UnsupportedLookAndFeelException;

public class Program {
	public static void main(String args[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		@SuppressWarnings("unused")
		MainFrame frame = new MainFrame();
		Resources.initialize();
		
		try {
			Logger logger = Logger.getGlobal();
    		FileHandler fh = new FileHandler("mylog.txt");
    		fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.WARNING);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
}
