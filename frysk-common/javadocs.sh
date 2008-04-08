#!/bin/sh -e

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

if test $# -lt 2 ; then
    echo "Usage: $0 <javadoc> <overview> <sourcedirs>" 1>&2
    exit 1
fi

javadoc=$1 ; shift
overview=$1 ; shift
sourcedirs="$@"

for scope in public private ; do
    rm -rf javadoc/$scope javadoc/source/$scope
    mkdir -p javadoc/$scope
    mkdir -p javadoc/source/$scope
    # Copy all the source to a single directory tree.
    for path in ${sourcedirs} ; do
	test -d $path || continue
	d=`dirname $path`
	b=`basename $path`
	echo "Copying $d : $b"
	(
	    cd $d
	    find $b \
		-path '*/*dir/*' -prune \
		-path '*/*tmp/*' -prune \
		-o -name '[A-Za-z]*\.java' -print \
		-o -name 'package.html' -print \
		-o -path '*/doc-files/*.jpg' -print \
		| tar cfT - -
	    ) | (
	    cd javadoc/source/$scope
	    tar xpf -
            )
    done
    # Strip out any Test files from the public sources; so that they
    # don't confuse the package list.
    case $scope in
	public ) find javadoc/source/$scope \
	    -name 'Test[A-Z]*' -print \
	    -o -name 'Stress[A-Z]*' -print \
	    | xargs rm
	    ;;
    esac
    # Generate the javadoc.
    jg=http://developer.gnome.org/doc/API/java-gnome
    ${javadoc} \
	-$scope \
	-link http://developer.classpath.org/doc/ \
	-link ${jg}/glib-java-0.4.0/api/ \
	-link ${jg}/cairo-java-1.0.6/api/ \
	-link ${jg}/libgtk-java-2.10.0/api/ \
	-link ${jg}/libglade-java-2.12.7/api/ \
	-link ${jg}/libgnome-java-2.12.6/api/ \
	-link ${jg}/libgconf-java-2.12.5/api/ \
	-link ${jg}/libvte-java-0.12.2/api/ \
	-overview ${overview} \
	-source 1.4 \
	-d javadoc/$scope \
	-use \
	-linksource \
	-doctitle '<em>frysk</em> - Execution Analysis And Debugging Technology' \
	-windowtitle 'FRYSK' \
	-group "<em>frysk</em> Packages" 'frysk*' \
	-group "Imported Packages" 'inua*:jline*:junit*:gnu*' \
	-group "Native Bindings" 'lib*' \
	-sourcepath javadoc/source/$scope \
	-subpackages frysk:lib:inua:jline:junit:gnu
	
done
