# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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


SUBDIRS = \
	frysk-imports \
	frysk-sys \
	frysk-core \
	frysk-gtk \
	frysk-gui

# Note the dir/. is needed, causes the file copy to put the contents
# into the top level directory.

SOURCEDIRS = \
	$(patsubst %,$(srcdir)/%/frysk,$(SUBDIRS)) \
	$(patsubst %,$(top_builddir)/%/frysk,$(SUBDIRS)) \
	$(srcdir)/frysk-sys/lib \
	$(top_builddir)/frysk-sys/lib \
	$(srcdir)/frysk-sys/inua \
	$(top_builddir)/frysk-sys/inua \
	$(srcdir)/frysk-imports/getopt/src/getopt/gnu \
	$(srcdir)/frysk-imports/jline/src/jline \
	$(srcdir)/frysk-imports/junit/src/junit \
	$(srcdir)/frysk-core/frysk/pkglibdir/. \
	$(srcdir)/frysk-core/frysk/bindir/. \
	$(srcdir)/frysk-gui/frysk/bindir/. \
	$(empty)


# Generate html manpages.  Use same xml sources, but generate html.
# html is always put in index.html in output dir, so rename.  Use
# template file and just put in title, logo and a list of utils +
# href.  Replace ./ with ../ relative references (manpages is one dir
# down).

TEMPLATE = $(srcdir)/htdocs/template.html
.PHONY: manpages
manpages manpages/index.html:
	rm -rf manpages
	mkdir -p manpages
	XMLTO=$(XMLTO) \
	sh $(srcdir)/frysk-common/manpages.sh \
		$(TEMPLATE) \
		-"Overview" \
		frysk-common/frysk.xml \
		-"Command Line Utilities" \
		frysk-core/frysk/bindir/*.xml \
		-"GNOME Interface" \
		frysk-gui/frysk/bindir/*.xml \
		-"Test framework" \
		*/frysk/pkglibdir/*.xml
	mv manpages/index.new manpages/index.html

# Generate JAVADOC documentation.
JAVADOC = javadoc
JAVADOC_CLASSPATH = /usr/share/java/libgcj-`$(GCJ) -dumpversion`.jar
CLEANFILES += javadoc
.PHONY: javadoc
javadoc javadoc/private/index.html:
	CLASSPATH=$(JAVADOC_CLASSPATH):$(subst $(space),:,$(FRYSK_GNOME_JARS)) \
	sh $(srcdir)/frysk-common/javadocs.sh \
		$(JAVADOC) \
		$(srcdir)/overview.html \
		$(SOURCEDIRS)

UPLOAD_FLAGS =	--fuzzy \
		--delete-during \
		--ignore-times \
		--recursive \
		--links \
		--itemize-changes \
		--stats

# Since the documentation generation process re-generates all the
# files, changing their last-modified time (even when the contents
# don't change), rsync-a is forced to upload a mod-time on every file.
# Avoid that cost by ignoring the mod-time and instead using
# --checksum; while pulling all the checksums is expensive it isn't as
# expensive as pushing every file's mod-time.

UPLOAD_FLAGS += --checksum

# Explicitly specify the file permissions using --chmod.  This ensures
# that uploaded files are group writable so that others can do
# updates; and directories are SGID so that group membership
# propogates down (fixed in later rsync releases).  Also explicitly
# specify --perms so that the --chmod permission is used, and not
# overwriden by the remote remote UMASK (this can also lead to
# permission denied warnings when trying to set the permission of
# directories you don't own, just live with that).

UPLOAD_FLAGS += --chmod ug=rwX,o=rX,Dg+s --perms

UPLOAD = echo "Don't worry too much about directory permission warnings" ; \
	 rsync $(UPLOAD_FLAGS)

.PHONY: upload-javadoc
upload-javadoc: javadoc/private/index.html
	$(UPLOAD) \
		javadoc/public javadoc/private \
		sourceware.org:/sourceware/www/sourceware/htdocs/frysk/javadoc

.PHONY: upload-manpages
upload-manpages: manpages/index.html
	$(UPLOAD) \
		manpages/ \
		sourceware.org:/sourceware/www/sourceware/htdocs/frysk/manpages

.PHONY: upload-docs
upload-docs: upload-manpages upload-javadoc
	true

# Generate DOXYGEN documentation.

DOXYFILE = doxygen/Doxyfile
.PHONY: doxygen
doxygen: $(DOXYFILE)
	doxygen $(DOXYFILE)
$(DOXYFILE): Doxyfile.in
	mkdir -p `dirname $@`
	sed -e 's,@SRCDIR@,$(SOURCEDIRS),g' \
		< $< > $@.tmp
	mv $@.tmp $@
CLEANFILES += doxygen

# Create an eclipse directory that matches the build tree.

.PHONY: eclipse
eclipse: $(ECLIPSE_CONFIG)
BUILT_SOURCES += $(ECLIPSE_CONFIG)
ECLIPSE_CONFIG =
ECLIPSE_CONFIG += eclipse/.project
ECLIPSE_CONFIG += eclipse/.classpath
eclipse/.%: frysk-top/%.in
	mkdir -p `dirname $@`
	$(SUBST)
	chmod u+w $@
ECLIPSE_CONFIG += eclipse/.externalToolBuilders/make-all.launch
eclipse/.externalToolBuilders/%.launch: frysk-top/%.launch.in
	mkdir -p `dirname $@`
	$(SUBST)
	chmod u+w $@
ECLIPSE_CONFIG += eclipse/.settings/org.eclipse.jdt.core.prefs
ECLIPSE_CONFIG += eclipse/.settings/org.eclipse.jdt.ui.prefs
eclipse/.settings/%.prefs: frysk-top/%.prefs.in
	mkdir -p `dirname $@`
	$(SUBST)
	chmod u+w $@

man_MANS += frysk-common/frysk.7
CLEANFILES += frysk-common/frysk.7
CLEANFILES += frysk-common/frysk.xml
frysk-common/frysk.7: frysk-common/frysk.xml
	mkdir -p `dirname $@`
	$(XMLTO) -o `dirname $@` man $<
