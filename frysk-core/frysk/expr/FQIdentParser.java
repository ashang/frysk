// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.expr;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import antlr.CharScanner;
import antlr.CharStreamException;
import antlr.MismatchedCharException;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;
import antlr.InputBuffer;
import antlr.CharBuffer;

/**
 * Funky HPD #-syntax doesn't map very well to LL-k type parser (for
 * constant 'k').  When written directly in antlr, we obviously get
 * lots of lexical ambiguities.  We work around that by doing
 * arbitrary manual look-ahead and just parsing the tokens ourselves.
 * FQIdentParser is where that parsing takes place.  Besides
 * supporting antlr lexical analyzer, the class can be used standalone
 * to parse arbitrary strings.
 */
public class FQIdentParser {

    private int i;
    private String fqinit;
    private final CharScanner scanner;
    private final boolean allowDynamic;
    private final boolean allowGlobs;
    private final boolean expectMoreTokens;

    private final Pattern symbolPattern;
    private final Pattern globPattern;

    /**
     * @param allowDynamic Whether the [pid.tid#frame] portion of the
     *        FQ syntax makes sense in given context.  For example it
     *        doesn't for ftrace, but in general does for hpd.
     *
     * @param allowGlobs Whether globs should be allowed.  This
     *        changes syntax of symbol portion of FQ identifier, which
     *        becomes essentially unrestricted.  Note that is globs
     *        are allowed, simple expressions as e.g. "a*b" are no
     *        longer parsed as three tokens, but become one glob
     *        symbol name.
     *
     * @param expectMoreTokens Whether whitespace terminates
     *        lookahead.  When no more tokens are expected, it
     *        doesn't.
     */
    FQIdentParser(CharScanner scanner,
		  boolean allowDynamic,
		  boolean allowGlobs,
		  boolean expectMoreTokens,
		  boolean allowPeriodInSymbol) {

	this.scanner = scanner;
	this.allowDynamic = allowDynamic;
	this.allowGlobs = allowGlobs;
	this.expectMoreTokens = expectMoreTokens;

	// This pattern deliberately doesn't check for initial letter.
	// Relevant code checks this explicitly.  This way, if user makes
	// a mistake and writes e.g. something#123+b, we recognize "123"
	// as a typo, while leaving out the part after a "+", which is
	// certainly irrelevant.
	String symbolRe = "[a-zA-Z0-9_$" + (allowPeriodInSymbol
					    ? "." : "") + "]+";
	this.symbolPattern = Pattern.compile(symbolRe);
	this.globPattern
	    = Pattern.compile("(\\[(\\^?\\][^\\]]*" +  // handles []abc] and [^]abc]
			      "|\\^[^\\]]+" +          // handles [^abc]
			      "|[^^\\]][^\\]]*" +      // handles [abc], and [ab^c] (cases where ^ isn't an operator)
			      "|\\^?\\[:[^:]+:\\]"+    // handles [[:abc:]] and [^[:abc:]]
			      ")\\]|" + symbolRe + "|\\*)+");
    }

    private char fqLA(int i) throws CharStreamException {
	if (i >= fqinit.length())
	    return scanner.LA(i - fqinit.length() + 1);
	else
	    return fqinit.charAt(i);
    }

    private void fqmatch(String s) throws MismatchedCharException, CharStreamException {
	while (fqinit.length() > 0) {
	    char c = s.charAt(0);
	    char d = fqinit.charAt(0);
	    if (c != d)
		throw new MismatchedCharException(d, c, false, scanner);
	    s = s.substring(1);
	    fqinit = fqinit.substring(1);
	}
	scanner.match(s);
    }

    private String maybeParsePrefix(char start, char end, String context)
	throws RecognitionException, CharStreamException
    {
	char c = fqLA(0);
        if (c != start)
	    return null;

	StringBuffer matched = new StringBuffer();
	matched.append(c);
	i++;
	while (true) {
	    c = fqLA(i++);
	    matched.append(c);
	    if ((expectMoreTokens && Character.isWhitespace(c))
		|| c == CharScanner.EOF_CHAR)
		throw new RecognitionException("Nonterminated " + context
					       + " `" + matched
					       + "' in fully qualified notation.");
	    else if (c == end)
		break;
	}

	if (matched.length() <= 2)
	    throw new RecognitionException("Empty " + context
					   + " `" + matched
					   + "' in fully qualified notation.");

	return matched.toString();
    }

    public static boolean isGlobChar(char c) {
	return c == '*' || c == '?' || c == '['
	    || c == ']' || c == '^' || c == ':'
	    || c == '-';
    }

    public static boolean isWildcardPattern(String str) {
	// man 7 glob: """A string is a wildcard pattern if it
	//  contains one of the characters "?", "*" or "["."""
	return str.indexOf('*') != -1
	    || str.indexOf('?') != -1
	    || str.indexOf('[') != -1;
    }

    /**
     * @param initial Portion of the character stream that is part of
     *        the identifier, but was already consumed by lexer.
     */
    public FQIdentToken parse(String initial)
        throws RecognitionException, CharStreamException, TokenStreamException
    {
	fqinit = initial;
	i = 0;

        String matched = "";
        String part;

        String partDso = null;
        String partFile = null;
        String partProc = null;
        String partLine = null;
        String partProcessId = null;
        String partThreadId = null;
        String partFrameNum = null;

        char c;

        // Automaton state is composed of following sub-states:
        final int FILE = 1;
        final int LINE = 2;
        final int SYMB = 4;
        int allowed = LINE | SYMB;
        int state = allowed;

        // Parse optional [pid.tid#frame] part of FQ identifier.  The
        // spec mentions that if that part is given, the rest of the
        // qualification (except the symbol name) is superfluous, but
        // does allow the identifier to be fully qualified anyway.

	if (allowDynamic) {
	    part = maybeParsePrefix('[', ']', "dynamic context");
	    if (part != null) {
		matched += part;
		Matcher m = Pattern.compile("\\[[0-9]+\\.[0-9]+#[0-9]+\\]").matcher(part);
		if (!m.matches())
		    return null;

		int hash = part.indexOf('#');
		int dot = part.indexOf('.');
		partProcessId = part.substring(1, dot);
		partThreadId = part.substring(dot + 1, hash);
		partFrameNum = part.substring(hash + 1, part.length() - 1);
	    }
	}

	part = maybeParsePrefix('#', '#', "DSO part");
	if (part != null) {
	    matched += part;
	    partDso = part.substring(1, part.length() - 1);
	}

        // Parse the rest of the identifier qualification,
        // i.e. {file#|}{line#|proc#|}{plt:|}symbol{@version|}
	part = "";
        loop: while(true) {
            c = fqLA(i++);
            if ((expectMoreTokens && Character.isWhitespace(c))
		|| c == CharScanner.EOF_CHAR)
                break;

            matched += c;
            part += c;
            switch (c) {
                case '.': {
                    state |= FILE;
                    state &= ~SYMB;
                    break;
                }

                case '#': {
                    if (partLine == null && partProc == null
                        && partProcessId == null) {

                        if ((state & FILE) != 0 && partFile == null)
                            partFile = part.substring(0, part.length() - 1);
                        else if ((state & LINE) != 0)
                            partLine = part.substring(0, part.length() - 1);
                        else if ((state & SYMB) != 0) {
                            partProc = part.substring(0, part.length() - 1);
                            if (!Character.isJavaIdentifierStart(partProc.charAt(0)))
                                throw new RecognitionException("Procedure part (`" + partProc + "') in fully "
                                                               + "qualified notation has to be valid identifier.");
                        } else
                            // This # could belong to the next symbol.
                            // Break out and try to match the initial sequence.
                            break loop;
                    } else
                        throw new RecognitionException("Unexpected `#' after line or proc name was defined.");

                    state = allowed & SYMB;
                    if (partLine == null && partProc == null)
                        state |= allowed & LINE;
                    part = "";
                    break;
                }

                default: {
                    if (!(c >= '0' && c <= '9')) {
                        state &= ~LINE;

                        if (!(Character.isJavaIdentifierStart(c)
                              || c == '@'
                              || (c == ':' && part.length() == 4
                                  && part.equals("plt:"))
			      || (allowGlobs && isGlobChar(c)))) {

                            // Break out early if we are already
                            // just waiting for symbol.
                            if (partLine != null || partProc != null
                                || partProcessId != null)
                                break loop;
                            else
                                state &= ~SYMB;
                        }
                    }
                }
            }
        }

        // ((state & SYMB) == 0) here means that we've parsed more
        // than a symbol name, in hope it would turn out to be a
        // file name (e.g. hello-world.c#symbol as a symbol
        // reference vs. hello-world.c as an expression involving
        // subtraction and struct access).  In following, we take
        // care not to consume anything that's not an identifier.
        // E.g. when the user types "a+b", we want to match
        // only identifier "a".

        boolean wantPlt = false;
        if (part.startsWith("plt:")) {
            wantPlt = true;
            part = part.substring(4);
        }

        int v = part.indexOf('@');
        String version = null;
        if (v >= 0) {
            version = part.substring(v + 1);
            part = part.substring(0, v);
        }

	Matcher m = (allowGlobs ? globPattern : symbolPattern).matcher(part);
	if (m.lookingAt()) {
	    int diff = part.length() - m.end();
	    if (diff > 0) {
		matched = matched.substring(0, matched.length() - diff);
		part = part.substring(0, m.end());
	    }
	} else
	    throw new RecognitionException("Expected "
					   + (allowGlobs ? "glob" : "symbol name")
					   + ", got `" + part + "'.");

	c = part.charAt(0);
	if (!(Character.isJavaIdentifierStart(c)
	      || (allowGlobs && isGlobChar(c))))
	    throw new RecognitionException("Invalid symbol"
					   + (allowGlobs ? " glob" : "" )
					   + " `" + part + "'.");

        FQIdentToken tok = new FQIdentToken(CExprParserTokenTypes.IDENT, matched);
        tok.dso = partDso;
        tok.file = partFile;
        tok.line = partLine;
        tok.proc = partProc;
        tok.symbol = part;
        tok.version = version;
        tok.wantPlt = wantPlt;
        tok.processId = partProcessId;
        tok.threadId = partThreadId;
        tok.frameNumber = partFrameNum;
        tok.setLine(scanner.getLine());
	tok.globs = allowGlobs;

        fqmatch(matched);
        tok.setColumn(scanner.getColumn() - matched.length());

        return tok;
    }

    public static class FQIdentException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FQIdentException(String s) {
            super(s);
        }
    }

    public static class ExtraGarbageException extends FQIdentException {
        private static final long serialVersionUID = 1L;
        public ExtraGarbageException(String garbage) {
            super(garbage);
        }
    }

    public static class InvalidTokenException extends FQIdentException {
        private static final long serialVersionUID = 1L;
        public InvalidTokenException(String token) {
            super(token);
        }
    }

    public static FQIdentifier
    parseFQIdentifier(String str,
		      boolean allowDynamic,
		      boolean allowGlobs,
		      boolean expectMoreTokens,
		      boolean allowPeriodInSymbol)
        throws ExtraGarbageException, InvalidTokenException
    {
        try {
	    InputBuffer ib = new CharBuffer(new StringReader(str));
	    CharScanner scanner = new CharScanner(ib) {
		    public Token nextToken() throws TokenStreamException {
			return null;
		    }
		};
	    FQIdentParser parser
		= new FQIdentParser(scanner, allowDynamic,
				    allowGlobs, expectMoreTokens,
				    allowPeriodInSymbol);
	    FQIdentToken tok = parser.parse("");

	    if (scanner.LA(1) != CharScanner.EOF_CHAR)
                throw new ExtraGarbageException(scanner.getText());

	    return new FQIdentifier(tok);

        } catch (TokenStreamException exc) {
            throw new InvalidTokenException(str);
        } catch (RecognitionException exc) {
	    throw new InvalidTokenException(str);
	} catch (CharStreamException exc) {
	    throw new InvalidTokenException(str);
	}
    }

    public static FQIdentifier parseFtraceIdentifier(String str)
        throws ExtraGarbageException, InvalidTokenException
    {
	return parseFQIdentifier(str, false, true, false, true);
    }
}
