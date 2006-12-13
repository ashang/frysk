# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, Red Hat Inc.
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

CLEANFILES =

SUBDIRS = \
	frysk-imports \
	frysk-core \
	frysk-gtk \
	frysk-gui

empty=
space=$(empty) $(empty)

# Note the dir/. is needed, causes the file copy to put the contents
# into the top level directory.

SOURCEDIRS = \
	$(patsubst %,$(srcdir)/%/frysk,$(SUBDIRS)) \
	$(patsubst %,$(top_builddir)/%/frysk,$(SUBDIRS)) \
	$(srcdir)/frysk-imports/inua \
	$(top_builddir)/frysk-imports/inua \
	$(srcdir)/frysk-imports/getopt/src/getopt/gnu \
	$(srcdir)/frysk-imports/jline/src/jline \
	$(srcdir)/frysk-imports/junit/src/junit \
	$(srcdir)/frysk-core/frysk/pkglibdir/. \
	$(srcdir)/frysk-core/frysk/bindir/. \
	$(srcdir)/frysk-gui/frysk/bindir/. \
	$(empty)


# Generate JAVADOC documentation.
JAVADOC = gjdoc
JAVADOC_CLASSPATH = /usr/share/java/libgcj-`$(GCJ) -dumpversion`.jar
.PHONY: javadoc
javadoc: all
	for scope in public private ; do \
		rm -rf javadoc/$$scope javadoc/source/$$scope ; \
		mkdir -p javadoc/$$scope ; \
		mkdir -p javadoc/source/$$scope ; \
		for path in $(SOURCEDIRS) ; do \
			test -d $$path || continue ; \
			d=`dirname $$path` ; \
			b=`basename $$path` ; \
			echo "Copying $$d : $$b" ; \
			( \
				cd $$d ; \
				find $$b \
				-path '*/*dir/*' -prune \
				-o -name '[A-Za-z]*\.java' -print \
				-o -name 'package.html' -print \
				-o -path '*/doc-files/*.jpg' -print \
				| tar cfT - - ; \
			) | ( \
				cd javadoc/source/$$scope ; \
				tar xpf - ; \
			) \
		done ; \
		case $$scope in \
			public ) find javadoc/source/$$scope \
				-name '*Test*' -print \
				-o -name '*Stress*' -print \
				| xargs rm ; \
		esac ; \
		CLASSPATH=$(JAVADOC_CLASSPATH) $(JAVADOC) \
			-$$scope \
			-link http://developer.classpath.org/doc/ \
			-link http://java-gnome.sourceforge.net/docs/javadoc/ \
			-validhtml \
			-overview $(srcdir)/overview.html \
			-source 1.4 \
			-d javadoc/$$scope \
			-use \
			-linksource \
			-doctitle '<em>frysk</em> - Execution Analysis Technology' \
			-windowtitle 'FRYSK' \
			-group "<em>frysk</em> Packages" 'frysk*' \
			-group "Imported Packages" 'inua*:jline*:junit*:gnu*' \
			-sourcepath javadoc/source/$$scope \
			-all \
		; \
	done

# Since javadoc re-generates all the files, changing their
# last-modified time (even when the contents don't change), rsync-a
# must push a mod-time on every file.  Avoid that cost by ignoring the
# mod-time and instead using --checksum; while pulling all the
# checksums is expensive it isn't as expensive as pushing every file's
# mod-time.

.PHONY: upload-javadoc
upload-javadoc:
	rsync \
		--fuzzy \
		--delete-during \
		--ignore-times \
		--perms \
		--checksum \
		--recursive \
		--links \
		--itemize-changes \
		--stats \
		javadoc/public javadoc/private \
		sources.redhat.com:/sourceware/www/sourceware/htdocs/frysk/javadoc
CLEANFILES += javadoc

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

# Add some needed files to the distribution, include $(srcdir) so that
# the wildcard expansion occures there.

EXTRA_DIST =
EXTRA_DIST += \
	$(srcdir)/common/ChangeLog \
	$(srcdir)/common/*.sh \
	$(srcdir)/common/version.in \
	$(srcdir)/common/m4/*.m4 \
	$(srcdir)/bootstrap.sh \
	$(srcdir)/autogen.sh
