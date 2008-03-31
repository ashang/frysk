#!/bin/sh -e
# This file is part of the program FRYSK.
#
# Copyright 2008, Red Hat Inc.
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

if [ $# -lt 1 ]; then
    echo "Usage: $0 <template> -<heading> <xml-man-page> ..."
    exit 1
fi

abs_root_srcdir=$(cd $(dirname $(dirname $0)) && /bin/pwd)

XMLTO=${XMLTO:-xmlto}
template=$1 ; shift
exec > manpages/index.new

sed -n < ${template} \
    -e 's,-- title goes here --,<h1>Frysk Manual Pages</h1>,' \
    -e 's,-- logo goes here --,,' \
    -e 's,"\./,"../,' \
    -e '0,/<!-- start text -->/ p'

# Generate the body for the man pages.

suffix=
for xmlfile in "$@" ; do
    if expr x"${xmlfile}" : "x-" > /dev/null ; then
	cat <<EOF
${suffix}
<h3>`echo "x${xmlfile}" | sed -e 's/x-//'`</h3>
<ul>
EOF
        suffix="</ul>"
    else
	name=`basename $xmlfile .xml`
	n=`sed -n -e 's,.*<manvolnum>\([0-9]\)</manvolnum>.*,\1,p' < $xmlfile`
	echo "Generating man webpage for ${name}.${n}" 1>&2
	sed -e "s;@abs_root_srcdir@;${abs_root_srcdir};g" \
	    < $xmlfile \
	    > manpages/${name}.${n}.tmp
	${XMLTO} -o manpages html manpages/${name}.${n}.tmp
	rm -f manpages/${name}.${n}.tmp
	mv manpages/index.html manpages/${name}.${n}.html 
	
	cat <<EOF
<li><tt><a href="${name}.${n}.html">${name}.${n}</a></tt>
EOF
	sed -n 's/<refpurpose>\(.*\)<\/refpurpose>/\1/ p' $xmlfile
	echo "</li>"
    fi
done
echo "${suffix}"

sed -n < ${template} \
    -e '/<!-- end text -->/,$ p'
