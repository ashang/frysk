#!/bin/awk

# This script spits out SED lines that edit out warnings in generated
# code from antlr.  It needs to be run iteratively as often warnings
# are masked.

# A GCC warning, from GCJ, looks like:
# <file>:<line>: error: <what>
# ... <line> ...
#     ^^^^^^

# A GCJ warning looks like:
# ? ? ?
# ... <line> ...

function sed_comment(code) {
    return "s," code ",// " code ","
}

function get_prob_field(field) {
    return gensub(/^(.*\.java):([0-9]+): (error|warning): (.*)$/, \
		  "\\" field, "")
}

# Scan for compiler warnings
{
    if (/^.*\.java:[0-9]+: (error|warning): .*$/) {
	file = get_prob_field(1)
	line = get_prob_field(2)
	prob = get_prob_field(4)
	base = gensub(/.*\/([[:alnum:]]*)\.java/, "\\1", "", file)
	getline
	code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
	if (DEBUG) {
	    print "file=" file >> "/dev/stderr"
	    print "line=" line >> "/dev/stderr"
	    print "prob=" prob >> "/dev/stderr"
	    print "base=" base >> "/dev/stderr"
	    print "code=" code >> "/dev/stderr"
	}
    } else {
	next
    }
}

{
    sed = ""
}

prob ~ /Unnecessary semicolon/ {
    if (code ~ /};/) {
	sed = "s,};,},"
    } else if (code ~ /;;$/) {
	sed = "s,;;,;,"
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
    } else if (code ~ /Token _token = null;/) {
	sed = sed_comment(code)
    } else if (code ~ /Token theRetToken=null;/) {
	sed = sed_comment(code)
    }
}

prob ~ /.* cannot be resolved/ {
    if (code ~ /[_[:alnum:]]*_AST = /) {
	sed = "s,\\([a-zA-Z0-9]*_AST =\\),/* \\1 */,"
    } else if (code ~ /theRetToken=_returnToken;/) {
	sed = sed_comment(code);
    }
}

{
    if (sed != "") {
	# print base ": " line " " sed
	print line " " sed >> base ".antlr-warnings"
    }
    else {
	print "# " file ":" line ": " prob ": " code >> base ".antlr-warnings"
    }
}
