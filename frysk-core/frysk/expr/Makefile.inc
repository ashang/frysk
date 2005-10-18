# This file is part of the program FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# FRYSK is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
# 
# In addition, as a special exception, Red Hat, Inc. gives You the
# additional right to link the code of FRYSK with code not covered
# under the GNU General Public License ("Non-GPL Code") and to
# distribute linked combinations including the two, subject to the
# limitations in this paragraph. Non-GPL Code permitted under this
# exception must only link to the code of FRYSK through those well
# defined interfaces identified in the file named EXCEPTION found in
# the source code files (the "Approved Interfaces"). The files of
# Non-GPL Code may instantiate templates or use macros or inline
# functions from the Approved Interfaces without causing the
# resulting work to be covered by the GNU General Public
# License. Only Red Hat, Inc. may make changes or additions to the
# list of Approved Interfaces. You must obey the GNU General Public
# License in all respects for all of the FRYSK code and other code
# used in conjunction with FRYSK except the Non-GPL Code covered by
# this exception. If you modify this file, you may extend this
# exception to your version of the file, but you are not obligated to
# do so. If you do not wish to provide this exception without
# modification, you must delete this exception statement from your
# version and license this file solely under the GPL without
# exception.

GEN_SOURCES += frysk/expr/CppParser.java
GEN_SOURCES += frysk/expr/CppLexer.java
GEN_SOURCES += frysk/expr/CppTreeParser.java
GEN_SOURCES += frysk/expr/CppParserTokenTypes.java
GEN_SOURCES += frysk/expr/CppTreeParserTokenTypes.java

BUILT_SOURCES += frysk/expr/CppParser.java
BUILT_SOURCES += frysk/expr/CppTreeParser.java
BUILT_SOURCES += frysk/expr/CppLexer.java
BUILT_SOURCES += frysk/expr/CppParserTokenTypes.java
BUILT_SOURCES += frysk/expr/CppTreeParserTokenTypes.java

CLEANFILES += CppParser.java CppParser.tmp
CLEANFILES += CppTreeParser.java CppTreeParser.tmp
CLEANFILES += CppLexer.java CppLexer.tmp
CLEANFILES += CppParserTokenTypes.java CppParserTokenTypes.tmp
CLEANFILES += CppTreeParserTokenTypes.java CppTreeParserTokenTypes.tmp

frysk/expr/CppLexer.java: frysk/expr/CppParser.java
frysk/expr/CppTreeParser.java: frysk/expr/CppParser.java
frysk/expr/CppParserTokenTypes.java: frysk/expr/CppParser.java
frysk/expr/CppTreeParserTokenTypes.java: frysk/expr/CppParser.java
frysk/expr/CppParser.java: \
		$(srcdir)/frysk/expr/cpp.g \
		$(srcdir)/frysk/expr/CppLexer.sed \
		$(srcdir)/frysk/expr/CppTreeParser.sed \
		$(srcdir)/frysk/expr/CppTreeParserTokenTypes.sed \
		$(srcdir)/frysk/expr/CppParserTokenTypes.sed \
		$(srcdir)/frysk/expr/CppParser.sed
	antlr $(srcdir)/frysk/expr/cpp.g
	mv CppParserTokenTypes.txt frysk/expr/CppParserTokenTypes.txt
	mv CppTreeParserTokenTypes.txt frysk/expr/CppParserTokenTypes.txt

	sed -f $(srcdir)/frysk/expr/CppLexer.sed < CppLexer.java > CppLexer.tmp
	rm CppLexer.java
	mv CppLexer.tmp frysk/expr/CppLexer.java

	sed -f $(srcdir)/frysk/expr/CppParserTokenTypes.sed < CppParserTokenTypes.java > CppParserTokenTypes.tmp
	rm CppParserTokenTypes.java
	mv CppParserTokenTypes.tmp frysk/expr/CppParserTokenTypes.java

	sed -f $(srcdir)/frysk/expr/CppParser.sed < CppParser.java > CppParser.tmp
	mv CppParser.tmp frysk/expr/CppParser.java

	sed -f $(srcdir)/frysk/expr/CppTreeParser.sed < CppTreeParser.java > CppTreeParser.tmp
	rm CppTreeParser.java
	mv CppTreeParser.tmp frysk/expr/CppTreeParser.java

	sed -f $(srcdir)/frysk/expr/CppTreeParserTokenTypes.sed < CppTreeParserTokenTypes.java > CppTreeParserTokenTypes.tmp
	rm CppTreeParserTokenTypes.java
	mv CppTreeParserTokenTypes.tmp frysk/expr/CppTreeParserTokenTypes.java

	echo $(srcdir)
