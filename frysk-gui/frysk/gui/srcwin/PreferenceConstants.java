/**
 * PreferenceConstants serves as a location for various constants 
 */
package frysk.gui.srcwin;

/**
 * @author ajocksch
 *
 */
public class PreferenceConstants {
	
	/**
	 * Preference names relating to Inline Code
	 * @author ajocksch
	 *
	 */
	public static class Inline{
		public static final String B = "inline_b";
		public static final String G = "inline_g";
		public static final String R = "inline_r";
	}
	
	/**
	 * Preference names relating to Text Appearance
	 * @author ajocksch
	 *
	 */
	public static class Text{
		public static final String B = "textB";
		public static final String G = "textG";
		public static final String R = "textR";
	}
	
	/**
	 * Preference names relating to executable markers
	 * @author ajocksch
	 *
	 */
	public static class ExecMarks{
		public static final String B = "markB";
		public static final String G = "markG";
		public static final String R = "markR";
		public static final String SHOW = "showExecMarkers";
	}
	
	/**
	 * Preference names relating to the text background
	 * @author ajocksch
	 *
	 */
	public static class Background{
		public static final String B = "bgB";
		public static final String G = "bgG";
		public static final String R = "bgR";
	}
	
	/**
	 * Preference names relating to the margin background
	 * @author ajocksch
	 *
	 */
	public static class Margin{
		public static final String B = "marginB";
		public static final String G = "marginG";
		public static final String R = "marginR";
	}
	
	/**
	 * Preference names relating to line numbers
	 * @author ajocksch
	 *
	 */
	public static class LineNumbers{
		public static final String B = "lineNumB";
		public static final String G = "lineNumG";
		public static final String R = "lineNumR";
		public static final String SHOW = "showLineNumbers";	
	}
	
	/**
	 * Preference names relating to the current line
	 * @author ajocksch
	 *
	 */
	public static class CurrentLine{
		public static final String R = "currentLineR";
		public static final String G = "currentLineG";
		public static final String B = "currentLineB";
	}
	
	/**
	 * Preference names relating to function declarations/calls
	 * @author ajocksch
	 *
	 */
	public static class Functions{
		public static final String WEIGHT = "function_weight";
		public static final String R = "function_r";
		public static final String G = "function_b";
		public static final String B = "function_g";
	}
	
	/**
	 * Preference names relating to identifiers
	 * @author ajocksch
	 *
	 */
	public static class ID{
		public static final String WEIGHT = "id_weight";
		public static final String B = "id_b";
		public static final String G = "id_g";
		public static final String R = "id_r";
	}
	
	/**
	 * Preference names relating to keywords
	 * @author ajocksch
	 *
	 */
	public static class Keywords{
		public static final String WEIGHT = "keyword_weight";
		public static final String B = "keyword_b";
		public static final String G = "keyword_g";
		public static final String R = "keyword_r";
	}
	
	/**
	 * Preference names relating to global variables
	 * @author ajocksch
	 *
	 */
	public static class GlobalVariables{
		public static final String G = "global_g";
		public static final String R = "global_r";
		public static final String B = "global_b";
		public static final String WEIGHT = "global_weight";
	}
	
	/**
	 * Preference names relating to comments
	 * @author ajocksch
	 *
	 */
	public static class Comments{
		public static final String R = "comment_r";
		public static final String G = "comment_g";
		public static final String B = "comment_b";
		public static final String WEIGHT = "commment_weight";
	}
	
	/**
	 * Preference names relating to classes
	 * @author ajocksch
	 *
	 */
	public static class Classes{
		public static final String R = "class_r";
		public static final String G = "class_g";
		public static final String B = "class_b";
		public static final String WEIGHT = "class_weight";
	}
	
	/**
	 * Preference names relating to search results
	 * @author ajocksch
	 *
	 */
	public static class Search{
		public static final String G = "searchG";
		public static final String B = "searchB";
		public static final String R = "searchR";
	}
	
	// Node names in the preference hierarchy
	
	/**
	 * Whether or not to show the toolbar
	 */
	public static final String SHOW_TOOLBAR = "showToolbar";
	public static final String LNF_NODE = "lnf";
	public static final String SYNTAX_NODE = "syntax";

}
