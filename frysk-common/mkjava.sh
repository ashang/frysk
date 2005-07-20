#!/bin/sh

# From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
# Licenced under the terms of the Eclipse Public Licence.
# Licenced under the terms of the GNU CLASSPATH Licence.

if test $# -ne 1 ; then
    echo "Usage: $0 <java/file/name>"
    exit 1
fi

# File syntax is:

fatal ()
{
    echo 1>&2 "$@"
    exit 1
}

info ()
{
    echo 1>&2 "$@"
}

get_token ()
{
    token=
    while true ; do
	if read token value print ; then
	    if test x"$token" != x && expr "$token" : '[^#]' > /dev/null ; then
		break
	    fi
	else
	    false
	    return
	fi
    done
}

print_member ()
{
    # INDENTATION PATH NAME VALUE [print] 
    local sp=$1
    local path=$2
    local fullname=`echo $2.$3 | sed -e 's,\.,_,g' -e 's,&.*,,'`
    local op=`expr "$3" : '\([\&:]*\).*'`
    local name=`expr "$3" : '[^a-zA-Z0-9_]*\([a-zA-Z0-9_]*\)[^a-zA-Z0-9_]*'`
    local mask=`expr "$3" : '[^\&]*[\&]*\(.*\)'`
    local value=$4
    if test -z "$5" ; then
	print=`echo "${name}" | sed -e 's/_/ /g'`
    else
	print="$5"
    fi
    case "$name" in
	public|private|protected|import|boolean|float) name=_${name} ;;
        [0-9]*) name=_${name} ;;
    esac
    echo "${sp}  static public final int $name = ${value};"
    if test -z "${op}" ; then
	if test ! -z "${mask}" ; then
	    toString="${sp}    if (i != ${name} && (i & ${mask}) == ${name}) return \"${fullname}+\" + (i & ~${mask});
${toString}"
	    toShortString="${sp}    if (i != ${name} && (i & ${mask}) == ${name}) return \"${name}+\" + (i & ~${mask});
${toShortString}"
	fi
	if test x"$print" != x- ; then
	    toString="${toString}
${sp}    case ${name}: return \"${fullname}\";"
	    toShortString="${toShortString}
${sp}    case ${name}: return \"${name}\";"
	    toPrintString="${toPrintString}
${sp}    case ${name}: return \"${print}\";"
	fi
    fi
}

parse_class ()
{
    local sp=$1
    local scope=$2
    local path=$3
    local class=$4

    info "  Class" $path

    local toString="${sp}    switch ((int) i) {"
    local toShortString="${sp}    switch ((int) i) {"
    local toPrintString="${sp}    switch ((int) i) {"

    echo "${sp}${scope} class ${class}"
    echo "${sp}{"
    while get_token ; do
	case "$token" in
	    @class )
                parse_class \
		    "${sp}  " \
		    "public static" \
		    ${path}.${value} \
		    ${value}
		;;
	    '.' )
	        break
		;;
	    *\-* )
		name=`expr "${token}" : '\([^0-9]*\)[0-9]*-[^0-9]*[0-9]*'`
		base=`expr "${token}" : '[^0-9]*\([0-9]*\)-[^0-9]*[0-9]*'`
		bound=`expr "${token}" : '[^0-9]*[0-9]*-[^0-9]*\([0-9]*\)'`
	        lo=`expr "${value}" : '\([0-9x]*\)-[0-9x]*'`
	        hi=`expr "${value}" : '[0-9x]*-\([0-9x]*\)'`
		num=${base}
		# Only generate print for the first name
		while test ${num} -le ${bound} ; do
		    print_member \
			"${sp}" \
			"${path}" \
			"${name}${num}" \
			"${lo} + ${num}" \
			"${name}${num}"
		    num=`expr ${num} + 1`
		done
	        ;;
	    *\|*)
	        for name in `echo "${token}" | sed -e 's/|/ /g'`; do
		    print_member \
			"${sp}" \
			"${path}" \
			"${name}" \
			"${value}" \
			"${print}"
                    # Only print the first member
		    print=-
		done
	        ;;
	    * )
		print_member \
		    "${sp}" \
		    "${path}" \
		    "${token}" \
		    "${value}" \
		    "${print}"
		;;
	esac
    done
    _path=`echo "${path}" | sed -e 's,\.,_,'`
    echo "${sp}"
    echo "${sp}  static public String toString (long i)"
    echo "${sp}  {"
    echo "${toString}"
    echo "${sp}    default: return \"${_path}_0x\" + Long.toHexString (i);"
    echo "${sp}    }"
    echo "${sp}  }"
    echo "${sp}"
    echo "${sp}  static public String toShortString (long i)"
    echo "${sp}  {"
    echo "${toShortString}"
    echo "${sp}    default: return \"${_path}_0x\" + Long.toHexString (i);"
    echo "${sp}    }"
    echo "${sp}  }"
    echo "${sp}"
    echo "${sp}  static public String toPrintString (long i)"
    echo "${sp}  {"
    echo "${toPrintString}"
    echo "${sp}    default: return \"${_path}_0x\" + Long.toHexString (i);"
    echo "${sp}    }"
    echo "${sp}  }"
    echo "${sp}}"
}

create_class_file ()
{
    local package=$1
    local class=$2
    local file
    local dir

    if test "$package" = "" ; then
	fatal "Missing @package"
    fi

    file=`echo $package.${class} | sed -e 's,\.,/,g'`
    dir=`dirname $file`
    mkdir -p $dir
    info File $file.java

    exec > $file.new
    echo "package $package;"
    echo ""
    parse_class "" "public" ${class} ${class}
    exec 1>&2

    if test -r $file.java && cmp $file.new $file.java > /dev/null ; then
	:
    else
	mv $file.new $file.java
    fi
}


# The command line argument is the name of the file to create; but the
# actual java is written to stdout.

package=`dirname $1 | tr '[/]' '[.]'`
info Package $package
echo "package $package;"
echo ""

class=`basename $1 .java`
parse_class "" "public" ${class} ${class}

if get_token ; then
    fatal "Garbage at end of file"
fi
