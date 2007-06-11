#!/bin/awk

# This script spits out SED lines that edit out warnings in generated
# code from antlr.  It needs to be run iteratively as often warnings
# are masked.


function get_prob_field(field) {
    return gensub(/^(.*\.java):([0-9]+): (error|warning): (.*)$/, \
		  "\\" field, "")
}

{ file = "" }

# A GCJ warning, from ECJ, looks like:
# <file>:<line>: error: <prob>
# ... <code> ...
#     ^^^^^^

/^.*\.java:[0-9]+: (error|warning): .*$/ {
    file = get_prob_field(1)
    line = get_prob_field(2)
    prob = get_prob_field(4)
    getline
    code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
}

# An ECJ  warning looks like:
# <num>. WARNING in <file>
#  (at line <line>)
# ... <code> ...
#     ^^^^^^
# <prob>

/^[[:digit:]]+\. WARNING in .*\.java$/ {
    file = $4
    getline
    line = gensub(/)/, "", "", $3)
    getline
    code = gensub(/^[[:space:]]*(.*)[[:space:]]*$/, "\\1", "")
    getline
    getline
    prob = $0
}

# Did the warning get recognized?
{ if (file == "") next }

{
    base = gensub(/.*\/([[:alnum:]]*)\.java/, "\\1", "", file)
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
