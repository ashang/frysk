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
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-getopt.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libasm/libasm.a
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


# Depend on these runtimes.
JNI_LIBRARY_LIST += -lstdc++
JNI_LIBRARY_LIST += -laudit
# Bundle in the local elfutils code.
JNI_ARCHIVE_LIST += ../frysk-imports/elfutils/libasm/libasm_pic.a
JNI_ARCHIVE_LIST += ../frysk-imports/elfutils/libdwfl/libdwfl_pic.a
JNI_ARCHIVE_LIST += ../frysk-imports/elfutils/libdw/libdw_pic.a
JNI_ARCHIVE_LIST += ../frysk-imports/elfutils/libelf/libelf_pic.a
JNI_ARCHIVE_LIST += ../frysk-imports/elfutils/libebl/libebl.a
# Bundle in the local libunwind code.
JNI_OBJECT_LIST += ../frysk-imports/libunwind-i386/src/{,dwarf/,mi/,x86/}.libs/*.o
JNI_OBJECT_LIST += ../frysk-imports/libunwind-x86_64/src/{,dwarf/,mi/,x86_64/}.libs/*.o
JNI_OBJECT_LIST += ../frysk-imports/libunwind-ppc32/src/{,dwarf/,mi/,ppc32/}.libs/*.o
JNI_OBJECT_LIST += ../frysk-imports/libunwind-ppc64/src/{,dwarf/,mi/,ppc64/}.libs/*.o

# jnixx's auto-detect algorithm misses hidden class references,
# explicitly add them here.
JNIXX_CLASSES += frysk.sys.ProcessIdentifierFactory
JNIXX_CLASSES += java.lang.ArrayIndexOutOfBoundsException
JNIXX_CLASSES += frysk.sys.Errno$$Esrch
JNIXX_CLASSES += lib.dwfl.DwAttributeNotFoundException
JNIXX_CLASSES += java.lang.Long
JNIXX_CLASSES += lib.dwfl.ElfException
JNIXX_CLASSES += lib.unwind.UnwindRegistersPPC32
JNIXX_CLASSES += lib.unwind.UnwindRegistersPPC64
JNIXX_CLASSES += lib.unwind.UnwindRegistersX86
JNIXX_CLASSES += lib.unwind.UnwindRegistersX8664
