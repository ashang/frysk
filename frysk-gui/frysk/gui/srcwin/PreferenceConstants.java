// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 65535;
		public static final int R_DEFAULT = 65535;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 0;
		public static final int R_DEFAULT = 0;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 0;
		public static final int R_DEFAULT = 0;
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
		public static final int B_DEFAULT = 65535;
		public static final int G_DEFAULT = 65535;
		public static final int R_DEFAULT = 65535;
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
		public static final int B_DEFAULT = 65535;
		public static final int G_DEFAULT = 56283;
		public static final int R_DEFAULT = 54741;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 0;
		public static final int R_DEFAULT = 0;	
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
		public static final int B_DEFAULT = 30000;
		public static final int G_DEFAULT = 65535;
		public static final int R_DEFAULT = 30000;
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
		public static final int B_DEFAULT = 65535;
		public static final int G_DEFAULT = 0;
		public static final int R_DEFAULT = 0;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 30000;
		public static final int R_DEFAULT = 0;
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
		public static final int B_DEFAULT = 30000;
		public static final int G_DEFAULT = 0;
		public static final int R_DEFAULT = 30000;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 30000;
		public static final int R_DEFAULT = 65535;
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
		public static final int B_DEFAULT = 10000;
		public static final int G_DEFAULT = 30000;
		public static final int R_DEFAULT = 10000;
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
		public static final int B_DEFAULT = 10000;
		public static final int G_DEFAULT = 10000;
		public static final int R_DEFAULT = 10000;
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
		public static final int B_DEFAULT = 0;
		public static final int G_DEFAULT = 32200;
		public static final int R_DEFAULT = 65535;
	}
	
	// Node names in the preference hierarchy
	
	/**
	 * Whether or not to show the toolbar
	 */
	public static final String SHOW_TOOLBAR = "showToolbar";
	public static final String LNF_NODE = "lnf";
	public static final String SYNTAX_NODE = "syntax";

}
