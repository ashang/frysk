package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: CharStreamIOException.java,v 1.1.1.1 2005/11/25 22:29:28 cagney Exp $
 */

import java.io.IOException;

/**
 * Wrap an IOException in a CharStreamException
 */
public class CharStreamIOException extends CharStreamException {
    public IOException io;

    public CharStreamIOException(IOException io) {
        super(io.getMessage());
        this.io = io;
    }
}
