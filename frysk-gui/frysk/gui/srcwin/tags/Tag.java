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

package frysk.gui.srcwin.tags;

import org.jdom.DataConversionException;
import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.SaveableXXX;

public class Tag extends GuiObject implements SaveableXXX {
	
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
		super(fileName,filePath);
		this.fileName = fileName;
		this.filePath = filePath;
		this.lineNum = lineNum;
		this.lineText = lineText;
		doSaveObject();
	}
	
	public Tag()
	{
		super();
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

	/**
	 * Saves the tagset to a given element.
	 * @param Element XML node from manager
	 */
	public void save(Element node) {
		super.save(node);
		// Tag
		//this.fileName = super.getName();
		node.setAttribute("filename", this.fileName);
		node.setAttribute("filepath", this.filePath);
		node.setAttribute("linenum", ""+this.lineNum);
		node.setAttribute("linetext", this.lineText);
	}

	/**
	 * Loads the tag from a given element.
	 * @param Element XML node from manager
	 */
	public void load(Element node) {
		super.load(node);
		//actions
		
		this.fileName = node.getAttribute("filename").getValue();
		this.filePath = node.getAttribute("filepath").getValue();
		try {
			this.lineNum = node.getAttribute("linenum").getIntValue();
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		this.lineText = node.getAttribute("linetext").getValue();
	}


}
