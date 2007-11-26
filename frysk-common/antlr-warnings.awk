#!/bin/awk

# This file is part of the program FRYSK.
#
# Copyright 2007, Red Hat Inc.
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

# This script spits out SED lines that edit out warnings in generated
# code from antlr.  It needs to be run iteratively as often warnings
# are masked.


function get_prob_field(field) {
    return gensub(/^(.*\.java):([0-9]+): (error|warning): (.*)$/, \
		  "\\" field, "")
}

{
    if ($0 ~ /^.*\.java:[0-9]+: (warning|error): .*$/) {
# A GCJ warning, from ECJ, looks like:
# <file>:<line>: (warning|error): <prob>
# ... <code> ...
#     ^^^^^^
	file = get_prob_field(1)
	line = get_prob_field(2)
	prob = get_prob_field(4)
	getline
	code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
    } else if ($0 ~ /^[[:digit:]]+\. (WARNING|ERROR) in .*\.java$/) {
# An ECJ  warning looks like:
# <num>. (WARNING|ERROR) in <file>
#  (at line <line>)
# ... <code> ...
#     ^^^^^^
# <prob>
	file = $4
	getline
	line = gensub(/)/, "", "", $3)
	getline
	code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
	getline
	getline
	prob = $0
    } else if ($0 ~ /^[[:digit:]]+\. (WARNING|ERROR) in .*\.java \(at line .*\)$/) {
# An ECJ  warning also looks like:
# <num>. (WARNING|ERROR) in <file> (at line <line>)
# ... <code> ...
#     ^^^^^^
# <prob>
        java = ECJ
	file = $4
	line = gensub(/)/, "", "", $7)
	getline
	code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
	getline
	getline
	prob = $0
    } else {
	next
    }
    base = gensub(/^(|.*\/)([[:alnum:]]*)\.java/, "\\2", "", file)
    sed = ""
    if (DEBUG) {
        print "file=" file >> "/dev/stderr"
        print "line=" line >> "/dev/stderr"
        print "prob=" prob >> "/dev/stderr"
        print "base=" base >> "/dev/stderr"
	print "code=" code >> "/dev/stderr"
    }
}

function sed_comment(code) {
    return "s," code ",// " code ","
}

prob ~ /Unnecessary semicolon/ || prob ~ /An empty declaration is a deprecated feature/ {
    if (code ~ /};/) {
	sed = "s,};,} // ;,"
    } else if (code ~ /;;$/) {
	sed = "s,;;,; // ;,"
    }
}

prob ~ /The import .* is never used/ {
    sed = sed_comment(code)
}

prob ~ /The local variable .* is never read/ {
    if (code ~ /^AST tmp[[:digit:]]+_AST_in = \(AST)_t;$/) {
	sed = sed_comment(code)
    } else if (code ~ /^int _saveIndex;$/) {
	sed = sed_comment(code)
    } else if (code ~ /^AST [_[:alnum:]]*_AST = null;/) {
	sed = sed_comment(code)
    } else if (code ~ /^AST [_[:alnum:]]*_AST_in = \(_t == ASTNULL) \? null : \(AST)_t;/) {
	sed = sed_comment(code)
    } else if (code ~ /Token *[_[:alnum:]]* = null;/) {
	sed = sed_comment(code)
    } else if (code ~ /Token _token = null;/) {
	sed = sed_comment(code)
    } else if (code ~ /Token theRetToken=null;/) {
	sed = sed_comment(code)
    }
}

prob ~ /.* cannot be resolved/ {
    if (code ~ /[_[:alnum:]]*_AST = /) {
	sed = "s,^,//,"
    } else if (code ~ /[_[:alnum:]]*_AST_in = /) {
	sed = "s,^,//,"
    } else if (code ~ /theRetToken=_returnToken;/) {
	sed = sed_comment(code);
    } else if (code ~ /colon = LT.*;/) {
	sed = sed_comment(code);
    }
}

{
    if (sed != "") {
        if (DEBUG) print base ": " line " " sed >> "/dev/stderr"
	print line " " sed >> base ".antlr-fixes"
    }
    else {
	print "# " file ":" line ": " prob ": " code >> base ".antlr-fixes"
    }
}
