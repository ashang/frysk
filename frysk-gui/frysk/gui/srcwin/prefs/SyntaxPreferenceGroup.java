package frysk.gui.srcwin.prefs;

import frysk.gui.common.prefs.PreferenceGroup;

public class SyntaxPreferenceGroup extends PreferenceGroup {

	public static final String FUNCTIONS = "Functions";
	public static final String VARIABLES = "Variables";
	public static final String OPTIMIZED = "Optimized Variables";
	public static final String OUT_OF_SCOPE = "Out of Scope Variables";
	public static final String KEYWORDS = "Keywords";
	public static final String GLOBALS = "Global Variables";
	public static final String CLASSES = "Classes";
	public static final String COMMENTS = "Comments";
	public static final String NAMESPACE = "Namespaces";
	public static final String INCLUDES = "Includes";

	public SyntaxPreferenceGroup(String name, int tabNum) {
		super(name, tabNum);
	}

}
