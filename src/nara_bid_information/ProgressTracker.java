package nara_bid_information;

public interface ProgressTracker {
	public void updateProgress();
	
	public void updateProgress(int i);
	
	public void restart();
	
	public void finish();
}
