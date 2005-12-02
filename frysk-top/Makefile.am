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

CLEANFILES =


SUBDIRS = \
	frysk-imports \
	frysk-sys \
	frysk-core \
	frysk-gui

empty=
space=$(empty) $(empty)
SOURCEDIRS = \
	$(patsubst %,$(srcdir)/%,$(SUBDIRS)) \
	$(patsubst %,$(top_builddir)/%,$(SUBDIRS)) \
	$(srcdir)/frysk-imports/jargs/src \
	$(srcdir)/frysk-imports/jline/src
SOURCEPATH = $(subst $(space),:,$(strip $(SOURCEDIRS)))



# Generate JAVADOC documentation.

JAVADOC = gjdoc
API = j2se/1.4.2/docs/api
PACKAGE_LIST = $(API)/package-list
JAVADOC_CLASSPATH = /usr/share/java/libgcj-`$(GCJ) -dumpversion`.jar
.PHONY: javadoc
javadoc: javadoc/$(PACKAGE_LIST) all
	for scope in public private ; do \
		mkdir -p javadoc/$$scope ; \
		CLASSPATH=$(JAVADOC_CLASSPATH) \
		$(JAVADOC) \
		-$$scope \
		-linkoffline \
			http://java.sun.com/$(API) \
			javadoc/$(PACKAGE_LIST) \
		-sourcepath $(SOURCEPATH) \
		-overview $(srcdir)/overview.html \
		-source 1.4 \
		-d javadoc/$$scope \
		-use \
		-linksource \
		-doctitle '<em>frysk</em> - Execution Analysis Technology' \
		-windowtitle 'FRYSK' \
		-group "<em>frysk</em> Packages" 'frysk*' \
		-group "Imported Packages" 'jline*:jargs*:inua*' \
		-subpackages 'frysk:inua:jline:jargs' ; \
	done
javadoc/$(PACKAGE_LIST):
	mkdir -p `dirname javadoc/$(PACKAGE_LIST)`
	wget -O javadoc/$(PACKAGE_LIST) http://java.sun.com/$(PACKAGE_LIST)

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
		javadoc \
		sources.redhat.com:/sourceware/www/sourceware/htdocs/frysk
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

EXTRA_DIST = \
	common/version.in \
	autogen.sh \
	EXCEPTION
