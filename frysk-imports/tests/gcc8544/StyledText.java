package gcc8544;

public class StyledText {
    LineCache lineCache;

    public StyledText(int a){
    }

    public LineCache getCache() {
	return lineCache;
    }

    interface LineCache {

	public void calculate(int startLine, int lineCount);

    } // end LineCache 
} // end StyledText
