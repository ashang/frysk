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
package frysk.gui.srcwin.dom;

import java.math.BigInteger;
import java.util.StringTokenizer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class DOMTestGUIBuilder {

	private static Element root = new Element("Frysk_test_DOM");

	private static Document data = new Document(root);

	private static DOMFrysk dom = new DOMFrysk(data);
	
	/**************************************************************************
	 * Images info
	 *************************************************************************/
	
	private static String[] images = { "test6" };
	
	private static String[] CCPATH = { "/home/abc" };
	
	private static String[] source_path = { "/home/foo" };
	
	/**************************************************************************
	 * Functions info
	 *************************************************************************/
	
//	private static String[] func_text = { "public:",
//		"inline int min(int y, int z);",
//		"};" };
	
	private static String[] func_name = { "min" };
	
	//starting and ending line numbers(zero-based) of the inline functions
	private static int[] func_start_line = { 3 };
	
	private static int[] func_end_line = { 4 };
	
	private static int[] func_start_offset = { 0 };
	
	private static int[] func_end_offset = { 0 };
	
	/**************************************************************************
	 * Sources info
	 *************************************************************************/
	
	private static String[] sources = { "test6.cpp",
										"test5.cpp",
										"test4.cpp",
										"test3.cpp"};
	
	private static String[] sources_paths = { "/home/source",
		"/home/source",
		"/home/source",
		"/home/source"};
	
	/**************************************************************************
	 * Line info
	 *************************************************************************/
	
	private static String[] test_prog = { 
		"#include \"common.h\"\n",
		"// this is a test program for the DOM\n",
		"class bar\n",
		"{\n",
		"public:\n",
		"  inline int min(int y, int z);\n",
		"};\n",
		"\n",
		"// main program\n",
		"int bleh()\n",
		"{\n",
		"  bar a;\n",
		"  int i = a.min(1,2);\n",
		"  int j = a.min(3,4);\n",
		"  return 0;\n",
		"}\n" };
	
	private static BigInteger no_bytes = BigInteger.valueOf(4);
	
	private static BigInteger pc = BigInteger.valueOf(25842);
	
	private static int no_lines = test_prog.length;
	
	private static int[] line_length = new int[no_lines];
	
	private static int[] line_start_offset = new int[no_lines];
	
	private static boolean[] is_inline = new boolean[no_lines];
	
	private static boolean[] is_executable = new boolean[no_lines];
	
	/**************************************************************************
	 * tags info
	 *************************************************************************/
	
	private static String[] tags_keywords = { 	"class",
												"public",
												"int",
												"main",
												"return",
												"inline" };
	
	private static String[] tags_variables = {	"y",
												"z",
												"a",
												"i",
												"j" };
	
	private static String[] tags_classes = { "bar" };
	
	public static void main(String[] args) {
		buildDom(true);
		System.out.println("\n\n");
		printDOM();
	}

	/**
	 * @param makeTags TODO
	 * 
	 */
	private static void buildDom(boolean makeTags) {
		testDOMFrysk();
		testDOMImage();
		testDOMFunction();
		testDOMsource();
		if(makeTags)
			testDOMLine();
		testDOMInlineInstance();
	}
	
	public static DOMFrysk makeTestDOM(){
		buildDom(false);
		return dom;
	}

	public static void testDOMFrysk() {

		pc = new BigInteger("25");
		dom.setPC(pc);
		for (int i=0; i < images.length; i++) {
			dom.addImage(images[i], CCPATH[i], source_path[i]);
		}
		dom.addPID(256);

	}
	
	/**************************************************************************
	 * DOMImage
	 * 
	 *************************************************************************/
	public static void testDOMImage() {
		
		// add the source lines
		DOMImage testDOMImage = dom.getImage(images[0]);
		for (int i=0; i < sources.length; i++) {
			testDOMImage.addSource(sources[i], sources_paths[i]);
		}
		
		// Calculate starting and ending character offsets
		for (int ii = 0; ii < func_name.length; ii++) {
			for (int jj = 0; jj < test_prog.length; jj++) {
				if (jj < func_start_line[ii]) {
					func_start_offset[ii] = func_start_offset[ii] + 
					   test_prog[jj].length();
				}
				if (jj <= func_end_offset[ii]) {
					func_end_offset[ii] = func_end_offset[ii] +
						test_prog[jj].length();
				}
			}
		}
		// add the inline functions
//		for (int j=0; j < func_name.length; j++) {
//			testDOMImage.addFunction(func_name[j], func_text, 
//					func_start_offset[j], func_end_offset[j]);
//		}
	}
	
	/**************************************************************************
	 * DOMFunction class methods
	 **************************************************************************/
	public static void testDOMFunction() {
	}

	/**************************************************************************
	 * test the DOMSource class methods
	 *************************************************************************/
	public static void testDOMsource() {
		
		final DOMImage testDOMImage = dom.getImage(images[0]);
		
		final DOMSource testDOMSource = testDOMImage.getSource(sources[0]);
		
		int start_offset = 0;
		// calculate the starting character offsets for each line
		for (int i = 0; i < no_lines; i++) {
			line_length[i] = test_prog[i].length();
			line_start_offset[i] = start_offset;
			start_offset = start_offset + line_length[i];
			// see if this is a comment line
			if (test_prog[i].startsWith("//")) {
				is_executable[i] = false;
			} else {
				is_executable[i] = true;
			}
			// see if there is an inline function in there
			for (int j=0; j < func_name.length; j++) {
				if (test_prog[i].lastIndexOf(func_name[j]) <= 0) {
					is_inline[i] = false;
				} else {
					// if the function name is in-between the definition, skip
					if ((i < func_start_line[j]) &&
						(i > func_end_line[j])) {
							is_inline[i] = true;
					} else {
						is_inline[i] = false;
					}
				} 
			}
		}
		// add the lines to the source
		for (int ctr = 0; ctr < test_prog.length; ctr++) {
			testDOMSource.addLine(ctr + 1, test_prog[ctr], is_executable[ctr],
					false, line_start_offset[ctr], pc);
			pc = pc.add(no_bytes);
		}
	}

	/**************************************************************************
	 * DOMLine class
	 *************************************************************************/
	public static void testDOMLine() {
		
		final DOMImage testDOMImage = dom.getImage(images[0]);
		
		final DOMSource testDOMSource = testDOMImage.getSource(sources[0]);
		
		// add the lines to the source node
		for (int i = 0; i < test_prog.length; i++) {
			if (is_inline[i]) {
				DOMLine testDOMLine = testDOMSource.getLine(i);
				testDOMLine.addInlineInst(func_name[0], func_start_line[0], 
						func_end_line[0], 1);
			}
		}
		
		// now add the tags
		for (int i = 0; i < test_prog.length; i++) {
			if (test_prog[i].startsWith("//")) continue;
			DOMLine testDOMLine = testDOMSource.getLine(i+1);
			StringTokenizer st = new StringTokenizer(test_prog[i],";=(),: \n");
			String token;
			int line_index = 0;
			// parse the line for tags
			while (st.hasMoreTokens()) {
				token = st.nextToken();
				// look for any keywords in the list
				for (int j = 0; j < tags_keywords.length; j++) {
					if (token.equals(tags_keywords[j])) {
						line_index = test_prog[i].indexOf(token,line_index);
						testDOMLine.addTag("keyword", token, line_index);
						line_index = line_index + token.length();
					}
				}
				// look for any variables in the list
				for (int k = 0; k < tags_variables.length; k++) {
					if (token.equals(tags_variables[k])) {
						line_index = test_prog[i].indexOf(token,line_index);
						testDOMLine.addTag("local_var", token, line_index);
						line_index = line_index + token.length();
					}
				}
				// look for classes
				for(int m = 0; m < tags_classes.length; m++) {
					if (token.equals(tags_classes[m])){
						line_index = test_prog[i].indexOf(token,line_index);
						testDOMLine.addTag("class_decl", token, line_index);
						line_index = line_index + token.length();
					}
				}
				// look for inline instances
				for (int l = 0; l < func_name.length; l++) {
					if (token.equals(func_name[l]) && (i > func_end_line[l])) {
						line_index = test_prog[i].indexOf(token,line_index);
						testDOMLine.addInlineInst(token, line_index, 
								token.length(), 1);
					}
				}
			}
		}
	}
	
	/**
	 * Test the DOMInlineInstance class
	 *
	 */
	
	public static void testDOMInlineInstance() {
		
	}

	/**
	 * Print out the DOM in XML format
	 */
	public static void printDOM() {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(data, System.out);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
