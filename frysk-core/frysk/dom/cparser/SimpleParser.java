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
/**
 * An attempt to parser C/C++ code by simply tokenizing then lexing for the symbols we want
 */
package frysk.dom.cparser;

import java.io.IOException;

import frysk.dom.DOMSource;
import frysk.dom.StaticParser;

/**
 * @author ajocksch
 *
 */
public class SimpleParser implements StaticParser {
	private Tokenizer tokenMaker;
	
	private int scopeDepth = 0;
	
	/* (non-Javadoc)
	 * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String, com.redhat.fedora.frysk.gui.srcwin.SourceBuffer)
	 */
	public void parse(DOMSource source) throws IOException {
		this.tokenMaker = new Tokenizer(source.getFilePath() +"/" +  source.getFileName());
		
		while(this.tokenMaker.hasMoreTokens()){
			Token t = this.tokenMaker.nextToken();
			
			if(t.text.equals("//")){
//				Token t2 = t;
//				while(this.tokenMaker.hasMoreTokens() && this.tokenMaker.peek().lineNum == t.lineNum){
//					t2 = this.tokenMaker.nextToken();
//				}
//				buffer.addComment(t.lineNum, t.colNum, t.lineNum, t2.colNum+t2.text.length());
			}
			else if(t.text.equals("/*")){
//				Token t2 = t;
//				while(this.tokenMaker.hasMoreTokens() && !this.tokenMaker.peek().text.equals("*/")){
//					t2 = this.tokenMaker.nextToken();
//				}
//				t2 = this.tokenMaker.nextToken();
//				buffer.addComment(t.lineNum, t.colNum, t2.lineNum, t2.colNum+t2.text.length());
			}
			else if(t.text.equals("int") || t.text.equals("float") || t.text.equals("if") ||
					t.text.equals("for") || t.text.equals("while") || t.text.equals("else") ||
					t.text.equals("long") || t.text.equals("char") || t.text.equals("double") ||
					t.text.equals("class") || t.text.equals("return") || t.text.equals("struct") ||
					t.text.equals("public") || t.text.equals("private") ||
					t.text.equals("protected") || t.text.equals("do")){
//				buffer.addKeyword(t.lineNum, t.colNum, t.text.length());
			}
			else if(t.text.equals("{")){
				this.scopeDepth++;
			}
			else if(t.text.equals("}")){
				this.scopeDepth--;
			}
			else if(Character.isDigit(t.text.charAt(0))){
				// Code for constants
			}
			else{
				Token next = this.tokenMaker.peek();
				if(next == null)
					continue;
				
				if(next.text.equals("(")){
					if(scopeDepth == 0){
//						buffer.addFunction(t.text, t.lineNum, t.colNum, true);
					}
					else{
//						buffer.addFunction(t.text, t.lineNum, t.colNum, false);
					}
				}
				else if(Character.isLetter(t.text.charAt(0))){
//					buffer.addVariable(t.lineNum, t.colNum, t.text.length());
				}
			}
		}
	}
}