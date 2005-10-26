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
package frysk.gui.srcwin.cparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import frysk.gui.srcwin.SourceBuffer;
import frysk.gui.srcwin.StaticParser;
import frysk.gui.srcwin.Variable;

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
	public void parse(SourceBuffer buffer, String filename) throws IOException {
		this.tokenMaker = new Tokenizer(filename);
		
		while(this.tokenMaker.hasMoreTokens()){
			Token t = this.tokenMaker.nextToken();
			
			if(t.text.equals("//")){
				Token t2 = t;
				while(this.tokenMaker.hasMoreTokens() && this.tokenMaker.peek().lineNum == t.lineNum){
					t2 = this.tokenMaker.nextToken();
				}
				buffer.addComment(t.lineNum, t.colNum, t.lineNum, t2.colNum+t2.text.length());
			}
			else if(t.text.equals("/*")){
				Token t2 = t;
				while(this.tokenMaker.hasMoreTokens() && !this.tokenMaker.peek().text.equals("*/")){
					t2 = this.tokenMaker.nextToken();
				}
				t2 = this.tokenMaker.nextToken();
				buffer.addComment(t.lineNum, t.colNum, t2.lineNum, t2.colNum+t2.text.length());
			}
			else if(t.text.equals("int") || t.text.equals("float") || t.text.equals("if") ||
					t.text.equals("for") || t.text.equals("while") || t.text.equals("else") ||
					t.text.equals("long") || t.text.equals("char") || t.text.equals("double") ||
					t.text.equals("class") || t.text.equals("return") || t.text.equals("struct") ||
					t.text.equals("public") || t.text.equals("private") ||
					t.text.equals("protected") || t.text.equals("do")){
				buffer.addKeyword(t.lineNum, t.colNum, t.text.length());
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
					if(scopeDepth == 0)
						buffer.addFunction(t.text, t.lineNum, t.colNum, true);
					else
						buffer.addFunction(t.text, t.lineNum, t.colNum, false);
				}
				else if(Character.isLetter(t.text.charAt(0))){
					buffer.addVariable(new Variable(t.text, t.lineNum, t.colNum, (scopeDepth == 0)));
				}
			}
		}
	}

}

class Tokenizer{
	private BufferedReader reader;
	private String buffer = "";
	
	// use zero-indexed lines, same as SourceBuffer
	private int line = 0;
	private int offset = 0;
	
	private Token tok = null;
	
	/**
	 * Opens the tokenizer given the current file
	 * @param filename The file to tokenize
	 */
	public Tokenizer(String filename){
		this.buffer = "";
		try{
			this.reader = new BufferedReader(new FileReader(new File(filename)));
			while(reader.ready())
				this.buffer += reader.readLine()+"\n";
		}
		catch(FileNotFoundException e){
			System.out.println("Could not load file "+filename);
			e.printStackTrace();
		}
		catch(IOException e){
			// Deliberately empty
		}
		this.buffer = this.buffer.substring(0,this.buffer.length()-1);
	}

	public void swallow(){
		this.buffer = this.buffer.substring(this.tok.text.length()+this.buffer.indexOf(this.tok.text), this.buffer.length());
		this.tok = null;
	}
	
	public Token peek(){
		String token = "";
		if(this.buffer.equals("")){
			return null;
		}
		
		token = this.findShortestToken(this.buffer);
		
		this.tok = new Token(token, this.line, this.offset);
		this.offset += token.length();
		
		return this.tok;
	}
	
	public Token nextToken(){
		if(tok != null){
			this.buffer = this.buffer.substring(this.tok.text.length()+this.buffer.indexOf(this.tok.text), this.buffer.length());
			Token ret = this.tok;
			this.tok = null;
			return ret;
		}
		
		String token = "";
		if(this.buffer.equals("")){
			return null;
		}
		
		token = this.findShortestToken(this.buffer);
		if(token.equals(""))
			this.buffer = "";
		else
			this.buffer = this.buffer.substring(token.length()+this.buffer.indexOf(token), this.buffer.length());
		
		Token ret = new Token(token, this.line, this.offset);
		this.offset += token.length();
		
		return ret;
	}
	
	public boolean hasMoreTokens(){
		if(!this.buffer.equals(""))
			return true;
		else{
			boolean foundChar = false;
			for(int i = 0; i < this.buffer.length(); i++){
				if(!Character.isWhitespace(this.buffer.charAt(i))){
					foundChar = true;
					break;
				}
			}
			
			return foundChar;
		}
	}
	
	private String findShortestToken(String target){
		
		for(int i = 0; i < target.length(); i++){
			char c = target.charAt(i);
			if(Character.isWhitespace(c)){
				if(i == 0){
					target = target.substring(1);
					if(c == '\n'){
						this.line++;
						this.offset = 0;
					}
					else{
						this.offset++;
					}
					i = -1;
					continue;
				}
				else{
					return target.substring(0,i);
				}
			}
			
			if(c == ';' || c == '{' || c== '}' || c == '(' ||
					c == ')' || c == '/' || c == '*' || c == '+' ||
					c == '-' || c == '%' || c == '=' || c == '<' ||
					c == '>'){
				
				if(i == 0){
					if(c == '*' && i < target.length() - 1 && target.charAt(i+1) == '/')
						return target.substring(0,2);
					
					if((c == '+' || c == '%' || c == '*' || c == '-' || c== '/' ||
							c == '<' || c == '=' || c == '>') && i < target.length() - 1 &&
							target.charAt(i+1) == '=')
						return target.substring(0,2);
				
					if(c == '/' && i < target.length() - 1 && i < target.length() - 1 && 
							(target.charAt(i+1) == '/' || target.charAt(i+1) == '*'))
						return target.substring(0,2);

					return target.substring(0,1);
				}
				else
					return target.substring(0,i);
			}
		}
		
		return target;
	}
}

class Token{
	public String text;
	public int lineNum;
	public int colNum;
	
	public Token(String text, int line, int col){
		this.text = text;
		this.lineNum = line;
		this.colNum = col;
	}
	
	public String toString(){
		return this.text+"(line "+this.lineNum+", offset "+colNum+")";
	}
}
