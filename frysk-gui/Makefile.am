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

include common/Makefile.rules
include Makefile.gen

EXTRA_DIST += \
	frysk/bindir/ChangeLog \
	frysk/gui/ChangeLog \
	frysk/gui/common/ChangeLog \
	frysk/gui/srcwin/ChangeLog \
	frysk/gui/monitor/ChangeLog

GEN_SOURCEPATH += ../frysk-gtk/ $(top_srcdir)/../frysk-gtk/
GEN_SOURCEPATH += ../frysk-core/ $(top_srcdir)/../frysk-core/
GEN_SOURCEPATH += ../frysk-imports/ $(top_srcdir)/../frysk-imports/

GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_CLASSPATH += ../frysk-imports/cdtparser.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar 
GEN_CLASSPATH += ../frysk-imports/getopt.jar
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-gtk/tlwidgets/ftk.jar
GEN_CLASSPATH += $(FRYSK_GUI_JARS) 
GEN_CLASSPATH += /usr/lib

GEN_GCJ_LDADD += ../frysk-gtk/libfrysk-gtk.a
GEN_GCJ_LDADD += ../frysk-core/libfrysk-core.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-cdtparser.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-getopt.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jline.a 
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdw/libdw.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libebl/libebl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libelf/libelf.a
if USE_LIBUNWIND
GEN_GCJ_LDADD += ../frysk-imports/libunwind/src/.libs/libunwind-ptrace.a \
                ../frysk-imports/libunwind/src/.libs/libunwind-$(libunwind_cpu).a \
                ../frysk-imports/libunwind-i386/src/.libs/libunwind-x86.a
endif
GEN_GCJ_LDADD += -lstdc++
GEN_GCJ_LDADD += ../frysk-gtk/libfrysk-ftk.a
GEN_GCJ_LDADD += -L../frysk-gtk/EggTrayIcon
GEN_GCJ_LDADD += -L../frysk-gtk/tlwidgets
GEN_GCJ_LDADD += $(FRYSK_GUI_LIBS)
# Stub bfd_getb32 and bfd_getl32 for PPC64.  Unconditionally
# link -lbfd_get just for simplification.
GEN_GCJ_LDADD += -lopcodes -laudit -L../frysk-imports/lib/opcodes -lbfd_get

# Skip the JUnit tests (exit with status 77) when there is no display; bug #3012.
#TESTS_ENVIRONMENT = ( test $$tst != TestRunner || test -n "$$DISPLAY" || exit 77 ) && 
# ... well, for now, skip dogtail tests unconditionally:
TESTS_ENVIRONMENT = ( test $$tst = TestRunner && test -n "$$DISPLAY" || sh -c "exit 77" ) &&

TESTS += frysk/gui/test/dogtail_scripts/frysk_suite.py
EXTRA_DIST += frysk/gui/test/dogtail_scripts/frysk_suite.in
noinst_SCRIPTS += frysk/gui/test/dogtail_scripts/frysk_suite.py
CLEANFILES += frysk/gui/test/dogtail_scripts/frysk_suite.py
frysk/gui/test/dogtail_scripts/frysk_suite.py: frysk/gui/test/dogtail_scripts/frysk_suite.in
	$(SUBST)
	chmod a+x $@

# Fake rules for CONFIG_STATUS_DEPENDENCIES:
$(FRYSK_GUI_JARS):
	: no-op

# Run through the glade files and check to see that they don't contain
# references to glade libraries.  Create a local catalog so that it
# doesn't try to down load the XML definition stuff from the internet.

# This is part of all-local, since, if this fails the build won't fly.

all-local: checked-glade-xml
CLEANFILES += checked-glade-xml
checked-glade-xml: $(glade_DATA) glade.catalog
	export SGML_CATALOG_FILES=glade.catalog ; \
	for f in x $(glade_DATA) ; do \
		test $$f = x && continue; \
		echo checking $(srcdir)/$$f; \
		if $(XMLLINT) --catalogs --nonet --noout --valid \
			$(srcdir)/$$f ; then : ; \
		else \
			echo Malformed glade xml file: $(srcdir)/$$f; \
			exit 1; \
		fi; \
		if grep 'lib="gnome"' $(srcdir)/$$f; then \
			echo Gnome library found in $(srcdir)/$$f;\
			exit 1; \
		fi; \
	done
	touch $@
CLEANFILES += glade.catalog
GLADE_DTD_FILE = file://$(LIBGLADE_PREFIX)/share/xml/libglade/glade-2.0.dtd 
glade.catalog:
	rm -f $@.tmp
	$(XMLCATALOG) --noout -v --create $@.tmp
	$(XMLCATALOG) --noout -v --add rewriteURI \
		http://glade.gnome.org/glade-2.0.dtd \
		$(GLADE_DTD_FILE) \
		$@.tmp
	mv $@.tmp $@

# Handle the *.desktop, *.properties, and icon files.
desktopdir = $(datadir)/applications
dist_desktop_DATA = frysk/gui/frysk.desktop
icondir = $(datadir)/pixmaps
icon_DATA = frysk/gui/imagesdir/48/fryskTrayIcon48.png
propertydir = $(pkgdatadir)
dist_property_DATA = frysk/gui/common/messages.properties

# frysk's core, which doesn't have a UI gets install-tested using
# dejagnu.  Add those tests to 

install-data-local: install-dogtail-scripts-local
uninstall-local: uninstall-dogtail-scripts-local
.PHONY: install-dogtail-scripts-local uninstall-dogtail-scripts-local
install-dogtail-scripts-local:
	set -e ; \
	( \
		cd $(srcdir)/frysk/gui/test ; \
		find dogtail_scripts \
			-name CVS -prune \
			-o -type f -print \
		| tar cTf - - \
	) | ( \
		cd $(DESTDIR)$(pkgdatadir) ; \
		tar xpf - \
	)
	cp -p -f frysk/gui/test/dogtail_scripts/frysk_suite.py \
		$(DESTDIR)$(pkgdatadir)/dogtail_scripts
uninstall-dogtail-scripts-local:
	rm -rf $(DESTDIR)$(pkgdatadir)/dogtail_scripts
