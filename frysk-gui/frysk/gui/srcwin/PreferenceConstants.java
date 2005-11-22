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

import org.gnu.gdk.Color;

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
		public static final String COLOR_PREFIX = "inline_";
		public static final Color DEFAULT = new Color(0, 65535, 65535);
	}
	
	/**
	 * Preference names relating to Text Appearance
	 * @author ajocksch
	 *
	 */
	public static class Text{
		public static final String COLOR_PREFIX = "text_";
		public static final Color DEFAULT = new Color(0,0,0);
	}
	
	/**
	 * Preference names relating to executable markers
	 * @author ajocksch
	 *
	 */
	public static class ExecMarks{
		public static final String COLOR_PREFIX = "mark_";
		public static final String SHOW = "showExecMarkers";
		public static final Color DEFAULT = new Color(0,0,0);
	}
	
	/**
	 * Preference names relating to the text background
	 * @author ajocksch
	 *
	 */
	public static class Background{
		public static final String COLOR_PREFIX = "bg_";
		public static final Color DEFAULT = new Color(65535, 65535, 65535);
	}
	
	/**
	 * Preference names relating to the margin background
	 * @author ajocksch
	 *
	 */
	public static class Margin{
		public static final String COLOR_PREFIX = "margin_";
		public static final Color DEFAULT = new Color(65535, 56283, 54741);
	}
	
	/**
	 * Preference names relating to line numbers
	 * @author ajocksch
	 *
	 */
	public static class LineNumbers{
		public static final String COLOR_PREFIX = "lineNum_";
		public static final String SHOW = "showLineNumbers";
		public static final Color DEFAULT = new Color(0,0,0);
	}
	
	/**
	 * Preference names relating to the current line
	 * @author ajocksch
	 *
	 */
	public static class CurrentLine{
		public static final String COLOR_PREFIX = "currentLine_";
		public static final Color DEFAULT = new Color(30000, 65535, 30000);
	}
	
	/**
	 * Preference names relating to function declarations/calls
	 * @author ajocksch
	 *
	 */
	public static class Functions{
		public static final String WEIGHT = "function_weight";
		public static final String ITALICS = "function_italics";
		public static final String COLOR_PREFIX = "function_";
		public static final Color DEFAULT = new Color(65535, 0,0);
	}
	
	/**
	 * Preference names relating to identifiers
	 * @author ajocksch
	 *
	 */
	public static class Variables{
		public static final String WEIGHT = "variable_weight";
		public static final String ITALICS = "variable_italics";
		public static final String COLOR_PREFIX = "var_";
		public static final Color DEFAULT = new Color(0,30000,0);
	}
	
	public static class UnavailableVariables{
		public static final String WEIGHT = "dead_variable_weight";
		public static final String ITALICS = "dead_variable_italics";
		public static final String STRIKETHROUGH = "dead_variable_strikethrough";
		public static final boolean STRIKETHROUGH_DEFAULT = true;
		public static final String COLOR_PREFIX = "dead_var_";
		public static final Color DEFAULT = new Color(30000, 30000, 30000);
	}
	
	/**
	 * Preference names relating to keywords
	 * @author ajocksch
	 *
	 */
	public static class Keywords{
		public static final String WEIGHT = "keyword_weight";
		public static final String ITALICS = "keyword_italics";
		public static final String COLOR_PREFIX = "keyword_";
		public static final Color DEFAULT = new Color(30000, 0, 30000);
	}
	
	/**
	 * Preference names relating to global variables
	 * @author ajocksch
	 *
	 */
	public static class GlobalVariables{
		public static final String COLOR_PREFIX = "global_";
		public static final String WEIGHT = "global_weight";
		public static final Color DEFAULT = new Color(0, 30000, 65535); 
	}
	
	/**
	 * Preference names relating to comments
	 * @author ajocksch
	 *
	 */
	public static class Comments{
		public static final String COLOR_PREFIX = "comment_";
		public static final String WEIGHT = "commment_weight";
		public static final Color DEFAULT = new Color(10000, 30000, 10000);
	}
	
	/**
	 * Preference names relating to classes
	 * @author ajocksch
	 *
	 */
	public static class Classes{
		public static final String COLOR_PREFIX = "class_";
		public static final String WEIGHT = "class_weight";
		public static final String ITALICS = "class_italics";
		public static final Color DEFAULT = new Color(10000, 10000, 10000);
	}
	
	/**
	 * Preference names relating to search results
	 * @author ajocksch
	 *
	 */
	public static class Search{
		public static final String COLOR_PREFIX = "search_";
		public static final Color DEFAULT = new Color(0, 32200, 65535);
	}
	
	/**
	 * Whether or not to show the toolbar
	 */
	public static final String SHOW_TOOLBAR = "showToolbar";
	public static final String LNF_NODE = "lnf";
	public static final String SYNTAX_NODE = "syntax";

}
