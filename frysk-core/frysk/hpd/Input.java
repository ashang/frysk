// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

package frysk.hpd;

import java.util.ArrayList;
import java.util.List;

/**
 * Command class separates and contains different parts of a command:
 * set, action, parameters.  It is immutable.
 */
class Input {

    static class Token {
	final String value;
	final int start;
	final int end;
	Token(String value, int start, int end) {
	    this.value = value;
	    this.start = start;
	    this.end = end;
	}
    }

    private final String fullCommand;
    private final String set;
    private final String action;
    private final List tokens;

    private Input(String fullCommand, String set, String action, List tokens) {
	this.fullCommand = fullCommand;
	this.set = set;
	this.action = action;
	this.tokens = tokens;
    }

    /**
     * The constructor.
     * @param cmd the full preprocessed command in the form defined in
     * HPDF: [p/t-set/prefix] verb object [option ...] [-option
     * [value] ...]
     * @exception ParseException thrown if error are encountered
     * during parsing
     */
    public Input(String cmd) {
	fullCommand = cmd;
	tokens = tokenize(fullCommand);
	action = null;
	if (tokens.size() <= 0) {
	    set = null;
	} else {
	    String tempToken = ((Token)tokens.get(0)).value;
	    // first token is either p/t-set or an action
	    if (tempToken.startsWith("[") && tempToken.endsWith("]")) {
		// if p/t-set
		set = tempToken;
		tokens.remove(0);
	    } else {
		set = null;
	    }
	}
    }
    
    public String getSet() {
	return set;
    }

    public String getAction() {
	return action;
    }

    /**
     * Return the N'th parameter.
     */
    String parameter(int n) {
	return token(n).value;
    }

    /**
     * Return the value of the remaining input as a String[] (i.e.,
     * each token as a separate String).
     */
    String[] stringArrayValue() {
	String[] args = new String[tokens.size()];
	for (int i = 0; i < args.length; i++) {
	    args[i] = token(i).value;
	}
	return args;
    }

    /**
     * Return the value of the remaining input as a simple (raw)
     * string.
     */
    String stringValue() {
	if (size() == 0)
	    return "";
	return fullCommand.substring(token(0).start);
    }

    /**
     * Return the N'th token, or NULL.
     */
    Token token(int n) {
	return (Token)tokens.get(n);
    }

    /**
     * Remove the N'th parameter.
     */
    void remove(int n) {
	tokens.remove(n);
    }

    /**
     * Return the number or size of the parameter list.
     */
    int size() {
	return tokens.size();
    }

    public String toString() {
	return fullCommand;
    }

    /**
     * Accept the current action; advance to the next one.
     */
    Input accept() {
	List newTokens;
	String newAction;
	if (tokens.size() > 0) {
	    newAction = ((Token)tokens.get(0)).value;
	    newTokens = tokens.subList(1, tokens.size());
	} else {
	    newAction = null;
	    newTokens = tokens;
	}
	return new Input(fullCommand, set, newAction, newTokens);
    }

    /**
     * Tokenize a string (probably command) minding quoted statements
     * @return List of string tokens
     */
    private List tokenize(String str) {
	List tokens = new ArrayList();
	boolean needQuote = false;
	boolean needBracket = false;
	boolean needEscapee = false;
	int start = -1;
	StringBuffer token = new StringBuffer();

	for (int i = 0; i < str.length(); i++) {
	    char ch = str.charAt(i);
	    if (needEscapee) {
		token.append(ch);
		needEscapee = false;
	    } else if (ch == '\\') {
		if (start < 0)
		    start = i;
		needEscapee = true;
	    } else if (ch == '\"') {
		if (needQuote) {
		    // Reached the end of a string.
		    needQuote = false;
		} else {
		    // Start a quoted string.
		    needQuote = true;
		    if (start < 0)
			start = i;
		}
	    } else if (ch == '[') {
		if (start < 0)
		    start = i;
		token.append(ch);
		needBracket = true;
	    } else if (str.charAt(i) == ']') {
		token.append(ch);
		needBracket = false;
	    } else if (Character.isWhitespace(ch)) {
		if (needQuote)
		    // Strings retain white space.
		    token.append(ch);
		else if (needBracket)
		    // Sets discard white space; append nothing
		    token.append("");
		else if (start >= 0) {
		    // reached end of token
		    tokens.add(new Token(token.toString(), start, i));
		    token.setLength(0);
		    start = -1;
		}
	    } else {
		if (start < 0)
		    // new token
		    start = i;
		token.append(ch);
	    }
	}
	if (needEscapee)
	    throw new InvalidCommandException("Trailing escape");
	if (needQuote)
	    throw new InvalidCommandException("Unmatched quote.");
	if (needBracket)
	    throw new InvalidCommandException("Unmatched bracket.");
	if (start >= 0) {
	    tokens.add(new Token(token.toString(), start, str.length()));
	}
	return tokens;
    }
}
