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

package frysk.dom;

import java.util.Iterator;
import java.util.Vector;
import java.math.BigInteger;

import org.jdom.Element;

/**
 * DOMSource represents a source code file within the frysk source window dom
 * 
 * @author ajocksch
 */
public class DOMSource {
	/**
	 * Path to this file
	 */
	public static final String FILEPATH_ATTR = "filepath";
	/**
	 * Name of this file
	 */
	public static final String FILENAME_ATTR = "filename";
	/**
	 * Name of this node in the DOM tree
	 */
	public static final String SOURCE_NODE = "source";
	/**
	 * Whether this source file has already been parsed
	 */
	public static final String IS_PARSED = "parsed";
	// program counter attribute
	public static final String PC_ATTR = "pc";
	// text of the source line
	public static final String TEXT_ATTR = "text";
	
/*
 * public static DOMSource createDOMSource(String filename, String path){
 * Element source = new Element(SOURCE_NODE); source.setAttribute(FILENAME_ATTR,
 * filename); source.setAttribute(FILEPATH_ATTR, path);
 * 
 * return new DOMSource(source); }
 * 
 * public static DOMSource createDOMSource(DOMImage parent, String filename,
 * String path){ Element source = new Element(SOURCE_NODE);
 * source.setAttribute(FILENAME_ATTR, filename);
 * source.setAttribute(FILEPATH_ATTR, path);
 * parent.getElement().addContent(source);
 * 
 * return new DOMSource(source); }
 */
	
	private Element myElement;
	
	/**
	 * Creates a new DOMSource object with the given data as it's Element. data
	 * must be a node with name "source"
	 * 
	 * @param data
	 */
	public DOMSource(Element data){
		this.myElement = data;
	}
	
	/**
	 * @param name
	 *            to set the filename to
	 * @return true = set name worked, false if not
	 */
	
	public void setFileName(String name) {
		this.myElement.setAttribute(FILENAME_ATTR, name);
	}
	
	/**
	 * get the line count for this Source node
	 */
	public int getLineCount() {

		return this.myElement.getChildren(DOMLine.LINE_NODE).size();
	}
	
	/**
	 * @return The name of the file
	 */
	public String getFileName(){
		return this.myElement.getAttributeValue(FILENAME_ATTR);
	}
	
	/**
	 * @param new
	 *            path to set the FILEPATH_ATTR to
	 * @return true if successful, false if not
	 */
	public void setFilePath(String path) {
		this.myElement.setAttribute(FILEPATH_ATTR, path);
	}
	
	/**
	 * @return The path to the file
	 */
	public String getFilePath(){
		return this.myElement.getAttributeValue(FILEPATH_ATTR);
	}
	
	/**
	 * creates a line Element under this source Element
	 * 
	 * @param lineno - line number to add
	 * @param text - text of the line to add
	 * @param is_executable - is this line executable
	 * @param has_break - does this line have a breakpoint
	 * @param offset_index - character offset of this line from the start
	 * 				of the file 
	 * @param pc
	 * @param is_inline - does this line contain an inline function
	 */
	public void addLine(int lineno, String text, boolean is_executable, 
			boolean has_break, int offset_index, BigInteger pc) {
		
		Element sourceLineElement = new Element(DOMLine.LINE_NODE);
		sourceLineElement.setText(text);
		sourceLineElement.setAttribute(DOMLine.NUMBER_ATTR, Integer.toString(lineno));
		sourceLineElement.setAttribute(PC_ATTR, pc.toString());
		sourceLineElement.setAttribute(DOMLine.OFFSET_ATTR, Integer.toString(offset_index));
		sourceLineElement.setAttribute(DOMLine.LENGTH_ATTR, Integer.toString(text.length()));
		sourceLineElement.setAttribute(DOMLine.EXECUTABLE_ATTR, ""+is_executable);
		sourceLineElement.setAttribute(DOMLine.HAS_BREAK_ATTR, ""+has_break);
		this.myElement.addContent(sourceLineElement);
	}
	/**
	 * @return An iterator over all of the lines in this file
	 */
	public Iterator getLines(){
		Iterator iter = 
			this.myElement.getChildren(DOMLine.LINE_NODE).iterator();
		return iter;
	}
	
	/**
	 * Attempts to return the DOMLine corresponding to the given line in the
	 * file. If no tags exist on that line then null is returned.
	 * 
	 * @param num
	 *            The line number to get
	 * @return The DOMLine corresponding to the line, or null if no tags exist
	 *         on that line
	 */
	/*public DOMLine getLine(int num){
		final int lineNum = num;
		
		Iterator iter = this.myElement.getContent(new Filter() {
			static final long serialVersionUID = 1L;			
			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;
				
				if(elem.getName().equals(DOMLine.LINE_NODE) && 
						Integer.parseInt(elem.getAttributeValue(DOMLine.NUMBER_ATTR)) == lineNum)
					return true;
				
				return false;
			}
		}).iterator();
		
		if(!iter.hasNext())
			return null;
		
		DOMLine val = new DOMLine((Element) iter.next());
		
		if(iter.hasNext()){
			// TODO: Throw exception?
			// This should not be happening: duplicate source lines!
		}
		
		return val;
	}  */
	
	/**
	 * Attempts to return the DOMLine corresponding to the given line in the
	 * file. If no tags exist on that line then null is returned.
	 * (This is alternative to the above getLineNum() method, if the
	 * above is determined to not be necessary, delete it and rename this
	 * one to getLine().
	 * 
	 * @param num
	 *            The line number to get
	 * @return The DOMLine corresponding to the line, or null if no tags exist
	 *         on that line
	 */
	public DOMLine getLine(int num) {
		Iterator iter = 
			this.myElement.getChildren(DOMLine.LINE_NODE).iterator();
		while (iter.hasNext()) {
			Element line = (Element) iter.next();
			String lineno = line.getAttributeValue(DOMLine.NUMBER_ATTR);
			if (num == Integer.parseInt(lineno)) {
				DOMLine val = new DOMLine((Element) line);
				return val;
			}
		}
		return null;
	}
	
	
	public void addLine(DOMLine line){
		this.myElement.addContent(line.getElement());
	}
	
	/**
	 * @return An iterator to all the inlined function declarations in this
	 *         source file
	 */
	public Iterator getInlinedFunctions(){
		Iterator iter = this.myElement.getChildren(DOMFunction.FUNCTION_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMFunction((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Adds an inline function to this source. By convention the inline function
	 * declarations are added earlier in the xml schema than the line table
	 * 
	 * @param function
	 *            The inlined function declaration to add
	 */
	public void addInlineFunction(DOMFunction function){
		// Add the functions at the top, lines on the bottom
		this.myElement.addContent(0, function.getElement());
	}
	
	/**
	 * This method should only be used internally from the frysk source window
	 * dom
	 * 
	 * @return The Jdom element at the core of this node
	 */
	protected Element getElement(){
		return this.myElement;
	}
	
	public boolean isParsed(){
		return this.myElement.getAttributeValue(IS_PARSED).equals("true");
	}
	
	public void setParsed(boolean value){
		this.myElement.setAttribute(IS_PARSED, Boolean.toString(value));
	}
}
