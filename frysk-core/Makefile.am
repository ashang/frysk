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


include common/Makefile.rules
include Makefile.gen


frysk-core.jar: $(GEN_BUILT_CLASSES)
	cat $^ /dev/null \
		| sed -e '/^#/d' -e 's,\.,/,g' -e 's,$$,.class,' \
		| fastjar -@ -cf $@
noinst_PROGRAMS += frysk-core.jar

LDADD += libfrysk-core.a
libfrysk_core_a_SOURCES = $(GEN_SOURCES)
noinst_LIBRARIES += libfrysk-core.a

libfrysk-core.so: libfrysk-core.a
noinst_PROGRAMS += libfrysk-core.so

frysk-core.db: libfrysk-core.so frysk-core.jar
	gcj-dbtool -n $@.tmp
	gcj-dbtool -a $@.tmp frysk-core.jar libfrysk-core.so
	mv $@.tmp $@
noinst_PROGRAMS += frysk-core.db




GEN_SOURCES += com/redhat/fedora/frysk/expr/CppParser.java
GEN_SOURCES += com/redhat/fedora/frysk/expr/CppLexer.java
GEN_SOURCES += com/redhat/fedora/frysk/expr/CppParserTokenTypes.java

BUILT_SOURCES += com/redhat/fedora/frysk/expr/CppParser.java \
  com/redhat/fedora/frysk/expr/CppLexer.java \
  com/redhat/fedora/frysk/expr/CppParserTokenTypes.java

CLEANFILES+= com/redhat/fedora/frysk/expr/CppParser.java \
	com/redhat/fedora/frysk/expr/CppLexer.java	\
	com/redhat/fedora/frysk/expr/CppParserTokenTypes.java	\
	com/redhat/fedora/frysk/expr/CppParserTokenTypes.txt


com/redhat/fedora/frysk/expr/CppLexer.java com/redhat/fedora/frysk/expr/CppParserTokenTypes.java: com/redhat/fedora/frysk/expr/CppParser.java 

com/redhat/fedora/frysk/expr/CppParser.java: $(srcdir)/com/redhat/fedora/frysk/expr/cpp.g
	antlr -o com/redhat/fedora/frysk/expr $(srcdir)/com/redhat/fedora/frysk/expr/cpp.g
	echo $(srcdir)

GEN_CLASSPATH += ../frysk-sys
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
LDADD += ../frysk-sys/libfrysk-sys.a
LDADD += ../frysk-imports/libjline.a
LDADD += ../frysk-imports/libantlr.a
LDADD += ../frysk-imports/libjunit.a

# XXX: This needs to be make less-architecture independant.

prog_x86isa_x86regs_SOURCES = prog/x86isa/x86regs.S
noinst_PROGRAMS += prog/x86isa/x86regs
