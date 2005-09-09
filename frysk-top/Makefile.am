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
	$(patsubst %,$(top_builddir)/%,$(SUBDIRS))
SOURCEPATH = $(subst $(space),:,$(strip $(SOURCEDIRS)))



# Generate JAVADOC documentation.

JAVADOC = gjdoc
API = j2se/1.4.2/docs/api
PACKAGE_LIST = $(API)/package-list
CLASSPATH = /usr/share/java/libgcj-`$(GCJ) -dumpversion`.jar
.PHONY: javadoc
javadoc: javadoc/$(PACKAGE_LIST)
	CLASSPATH=$(CLASSPATH) \
	$(JAVADOC) \
		-linkoffline \
			http://java.sun.com/$(API) \
			javadoc/$(PACKAGE_LIST) \
		-sourcepath $(SOURCEPATH) \
		-package \
		-overview $(srcdir)/overview.html \
		-source 1.4 \
		-d javadoc \
		-use \
		-linksource \
		-subpackages com:util
javadoc/$(PACKAGE_LIST):
	mkdir -p `dirname javadoc/$(PACKAGE_LIST)`
	wget -O javadoc/$(PACKAGE_LIST) http://java.sun.com/$(PACKAGE_LIST)
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
