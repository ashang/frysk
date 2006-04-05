package frysk.gui.srcwin.tags;

public class Tag {
	
	private String fileName;
	private String filePath;
	private int lineNum;
	
	private String lineText; // for sanity checking
	
	/**
	 * Creates a new Tag
	 * @param fileName The name of the file to apply the tag to
	 * @param filePath Absolute path to the file
	 * @param lineNum The line number of the file to put the tag on
	 * @param lineText The text of the line, for error-checking purposes
	 */
	public Tag(String fileName, String filePath, int lineNum, String lineText){
		this.fileName = fileName;
		this.filePath = filePath;
		this.lineNum = lineNum;
		this.lineText = lineText;
	}

	/**
	 * 
	 * @return The name of the file this tag should be applied to
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * 
	 * @return The path to the file this tag should be applied to
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * 
	 * @return The line number that this tag should be placed on
	 */
	public int getLineNum() {
		return lineNum;
	}

	/**
	 * 
	 * @return The text of the line that the tag should be placed on,
	 * used for error checking purposes
	 */
	public String getLineText() {
		return lineText;
	}
	
	/**
	 * 
	 * @return The full path to the file this tag should be added to
	 */
	public String getFullFilePath(){
		return filePath + "/" + fileName;
	}
	
	/**
	 * Two tags are equal iff they point to the same line of the same file, and
	 * the text of the file is the same (i.e. the version of the file is the same)
	 */
	public boolean equals(Object obj){
		if(!(obj instanceof Tag))
			return false;
		
		Tag tag2 = (Tag) obj;
		
		return tag2.getFullFilePath().equals(this.getFullFilePath()) && 
		 	tag2.lineNum == this.lineNum &&
		 	tag2.lineText.equals(this.lineText);
	}
}
