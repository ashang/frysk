package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: TokenStream.java,v 1.1.1.1 2005/11/25 22:29:28 cagney Exp $
 */

public interface TokenStream {
    public Token nextToken() throws TokenStreamException;
}
