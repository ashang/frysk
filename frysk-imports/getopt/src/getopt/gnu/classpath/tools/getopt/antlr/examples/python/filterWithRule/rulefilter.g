// This file is part of PyANTLR. See LICENSE.txt for license
// details..........Copyright (C) Wolfgang Haefelinger, 2004.
//
// $Id: rulefilter.g,v 1.1.1.1 2005/11/25 22:29:32 cagney Exp $

header "Lexer.__main__" {
// main - create and run lexer from stdin
if __name__ == "__main__":
    import sys
    import antlr
    import rulefilter_l
    
    // create lexer - shall read from stdin
    L = rulefilter_l.Lexer()
    
    try:
        token = L.nextToken()
        while not token.isEOF():
            print token
            token = L.nextToken()
    
    except antlr.TokenStreamException, e:
        print "error: exception caught while lexing:", e
    
    // end of main
}

options {
    language=Python;
}

class rulefilter_l extends Lexer;

options {
	k=2;
	filter=IGNORE;
	charVocabulary = '\3'..'\177';
}

P : "<p>" ;
BR: "<br>" ;

protected
IGNORE
	:	'<' (~'>')* '>' { print "invalid tag: "+ $getText}
	|	( "\r\n" | '\r' | '\n' ) { $newline }
	|	.
	;
