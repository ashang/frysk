# This file is part of the program FRYSK.
#
# Copyright 2005, 2007, 2008, Red Hat Inc.
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


include common/Makefile.rules
include Makefile.gen

GEN_SOURCEPATH += ../frysk-imports/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-imports/
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
GEN_CLASSPATH += ../frysk-imports/getopt.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_CLASSPATH += ../frysk-imports/cdtparser.jar 
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-getopt.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-cdtparser.a 
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libdw/libdw.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libebl/libebl.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libelf/libelf.a
if USE_LIBUNWIND
GEN_GCJ_LDADD_LIST += ../frysk-imports/libunwind-i386/src/.libs/libunwind-x86.a \
                 ../frysk-imports/libunwind-x86_64/src/.libs/libunwind-x86_64.a \
		 ../frysk-imports/libunwind-ppc32/src/.libs/libunwind-ppc32.a \
                 ../frysk-imports/libunwind-ppc64/src/.libs/libunwind-ppc64.a
endif
GEN_GCJ_LDADD_LIST += -lstdc++ -laudit

# Force a few dependencies
lib/unwind/cni/Unwind%.cxx: lib/unwind/Unwind%.java lib/unwind/cni/UnwindH.hxx
lib/unwind/Unwind%.java: lib/unwind/Unwind.java



# Quick hack to get a test JNI program up-and-running; as a package is
# re-implemented in JNI, add it to the below.

noinst_DATA += JniRunner.jar JniRunner
CLEANFILES += JniRunner
JniRunner: | frysk-sys.jar libfrysk-sys-jni.so
	rm -f $@.tmp
	echo "#!/bin/sh"					>> $@.tmp
	echo "export LD_PRELOAD=libstdc++.so.6"			>> $@.tmp
	echo "# hack to hopefully find the right libstdc++.so"	>> $@.tmp
	echo "export LD_LIBRARY_PATH=.:/usr/lib64:/usr/lib"	>> $@.tmp
	echo "export CLASSPATH=\\"				>> $@.tmp
	echo "$(CLASSPATH):frysk-sys.jar:JniRunner.jar"		>> $@.tmp
	echo "if test \$$# -eq 0 ; then"			>> $@.tmp
	echo "  $(JAVA) JniRunner \\"				>> $@.tmp
	echo "    lib.stdcpp \\"				>> $@.tmp
	echo "    ;"						>> $@.tmp
	echo "else"						>> $@.tmp
	echo "  $(JAVA) JniRunner \"\$$@\""			>> $@.tmp
	echo "fi"						>> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

CLEANFILES += JniRunner.class JniRunner.jar
JniRunner.jar: JniRunner.java
	rm -f $@.tmp
	$(JAVAC) `echo $(JAVACFLAGS) | sed -e 's/-classpath /-classpath frysk-sys.jar:/'` JniRunner.java
	$(JAR) cf $@ JniRunner.class
	rm JniRunner.class
CLEANFILES += JniRunner.java
JniRunner.java: common/TestRunner.java-in
	rm -f $@.tmp
	( $(SUBST_SED) < $< | sed -e 's/TestRunner/JniRunner/' -e '/^}/d' ; \
	  echo "    static {" ; \
	  echo "        System.loadLibrary(\"frysk-sys-jni\");" ; \
	  echo "    }" ; \
	  echo "}" ; \
	) > $@.tmp
	mv $@.tmp $@
