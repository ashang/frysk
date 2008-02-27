package frysk.debuginfo;


public class PrintStackOptions {

    private boolean elfOnly;
    
    private int numberOfFrames;
    private boolean printParameters;
    private boolean printScopes;
    private boolean fullpath;
    private boolean printLibrary;
    private boolean virtualFrames;
    
    public PrintStackOptions() {
    }
    
    public void setNumberOfFrames(int numberOfFrames) {
	this.numberOfFrames = numberOfFrames;
    }
    public int numberOfFrames() {
	return numberOfFrames;
    }
    public void setPrintParameters(boolean printParameters) {
	this.printParameters = printParameters;
    }
    public boolean printParameters() {
	return printParameters;
    }
    public void setPrintScopes(boolean printScopes) {
	this.printScopes = printScopes;
    }
    public boolean printScopes() {
	return printScopes;
    }
    public void setPrintFullpath(boolean fullpath) {
	this.fullpath = fullpath;
    }
    public boolean fullpath() {
	return fullpath;
    }

    public void setPrintLibrary(boolean printLibrary) {
	this.printLibrary = printLibrary;
    }

    public boolean printLibrary() {
	return printLibrary;
    }

    public void setPrintVirtualFrames(boolean virtualFrames) {
	this.virtualFrames = virtualFrames;
    }

    public boolean printVirtualFrames() {
	return virtualFrames;
    }

    public void setElfOnly(boolean elfOnly) {
	this.elfOnly = elfOnly;
    }

    public boolean elfOnly() {
	return elfOnly;
    }
    
}
