# This file is part of FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# FRYSK is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

GEN_SOURCES += frysk/expr/CppParser.java
GEN_SOURCES += frysk/expr/CppLexer.java
GEN_SOURCES += frysk/expr/CppParserTokenTypes.java

BUILT_SOURCES += frysk/expr/CppParser.java
BUILT_SOURCES += frysk/expr/CppLexer.java
BUILT_SOURCES += frysk/expr/CppParserTokenTypes.java

CLEANFILES += CppParser.java CppParser.tmp
CLEANFILES += CppLexer.java CppLexer.tmp
CLEANFILES += CppParserTokenTypes.java CppParserTokenTypes.tmp

frysk/expr/CppLexer.java: frysk/expr/CppParser.java
frysk/expr/CppParserTokenTypes.java: frysk/expr/CppParser.java
frysk/expr/CppParser.java: $(srcdir)/frysk/expr/cpp.g
	antlr $(srcdir)/frysk/expr/cpp.g
	mv CppParserTokenTypes.java frysk/expr/CppParserTokenTypes.java
	mv CppParserTokenTypes.txt frysk/expr/CppParserTokenTypes.txt
	sed < CppLexer.java > CppLexer.tmp \
		-e '/import antlr.ANTLRException;/d' \
		-e '/import antlr.CharScanner;/d' \
		-e '/import antlr.MismatchedCharException;/d' \
		-e '/import antlr.SemanticException;/d' \
		-e '/import antlr.CommonToken;/d'
	rm CppLexer.java
	mv CppLexer.tmp frysk/expr/CppLexer.java
	sed < CppParser.java > CppParser.tmp \
		-e '/import antlr.TokenStreamIOException;/d' \
		-e '/import antlr.ANTLRException;/d' \
		-e '/import antlr.LLkParser;/d' \
		-e '/import antlr.MismatchedTokenException;/d' \
		-e '/import antlr.SemanticException;/d' \
		-e '/import java.util.Hashtable;/d' \
		-e `expr 1510 + 6`' s/};/}/'
	mv CppParser.tmp frysk/expr/CppParser.java
	echo $(srcdir)
