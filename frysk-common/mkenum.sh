#!/bin/sh

# From the VENUS project.  Copyright 2004, 2005, 2006, Andrew Cagney
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

line=dummy
comment=
get_token ()
{
    # EOF.
    if test x"${line}" = x ; then
	false
	return
    fi
    # Break down the previously read token.
    read token value print <<EOF
${line}
EOF
    # Find the next token.
    comment=
    while true ; do
	if read line ; then
	    if expr "${line}" : '#' > /dev/null ; then
		comment="${comment}
${line}"
	    elif test x"${line}" = x \
		|| expr "${line}" : ' *$' > /dev/null ; then
		:
	    else
		break
	    fi
	else
	    break
	fi
    done
}

print_comment ()
{
    if test x"${comment}" != x ; then
	echo "$1/**"
	echo "${comment}" | sed -e "1 d" -e "s,^#,$1 *,"
	echo "$1 */"
    fi
}

print_member ()
{
    # INDENTATION PATH NAME VALUE [print] 
    local sp=$1
    local path=$2
    local op=`expr "$3" : '\([\&:]*\).*'`
    local fullname=$2.$3
    local name=`expr "$3" : '[^a-zA-Z0-9_]*\([a-zA-Z0-9_]*\)[^a-zA-Z0-9_]*'`
    case "$name" in
	public|private|protected|import|boolean|float) name=_${name} ;;
        [0-9]*) name=_${name} ;;
    esac
    local mask=`expr "$3" : '[^\&]*[\&]*\(.*\)'`
    local value=$4
    local print
    if test -z "$5" ; then
	print=`echo "${name}" | sed -e 's/_/ /g'`
    else
	print="$5"
    fi
    print_comment "${sp}  "
    if expr $4 : '[0-9]' > /dev/null ; then
	echo "${sp}  static public final int _${name} = ${value};"
    else
	echo "${sp}  static public final int _${name} = _${value};"
    fi

    if test x"${print}" = x- ; then
	echo "${sp}  static public final $class $name = ${value};"
    else
	echo "${sp}  static public final $class $name = new $class (${value}, \"${fullname}\", \"${print}\", \"${name}\");"
    fi

    if test -z "${op}" ; then
	if test x"$print" != x- ; then
	    map="${map}
${sp}    map.put (${name}.string, ${name});"
	    valueOf="${valueOf}
${sp}    case _${name}: return ${name};"
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

    local map="${sp}    map = new java.util.HashMap ();"
    local valueOf=""

    print_comment "${sp}"
    echo "${sp}${scope} class ${class}"
    echo "  implements Comparable"
    echo "${sp}{"
    cat <<EOF
${sp}  private final String string;
${sp}  private final int value;
${sp}  private final String print;
${sp}  private final String name;
${sp}  private $class (int value, String string, String print, String name)
${sp}  {
${sp}    this.string = string;
${sp}    this.value = value;
${sp}    this.print = print;
${sp}    this.name = name;
${sp}  }
${sp}  /** Return the qualified name of the enum.  */
${sp}  public String toString ()
${sp}  {
${sp}    return string;
${sp}  }
${sp}  /** Return a printable version of the enum.  */
${sp}  public String toPrint ()
${sp}  {
${sp}    return print;
${sp}  }
${sp}  /** Return the name of just the enum.  */
${sp}  public String toName ()
${sp}  {
${sp}    return name;
${sp}  }
${sp}  public boolean equals (Object o)
${sp}  {
${sp}    if (o instanceof ${class})
${sp}      return ((${class}) o).value == this.value;
${sp}    else
${sp}      return false;
${sp}  }
${sp}  public int hashCode ()
${sp}  {
${sp}    return value;
${sp}  }
${sp}  public int compareTo (Object o)
${sp}  {
${sp}    ${class} rhs = (${class}) o;
${sp}    return rhs.value - this.value;
${sp}  }
EOF
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
    cat <<EOF
${sp}
${sp}  /** Create a HashMap containing all the ${class} elements.  */
${sp}  private static java.util.Map getMap ()
${sp}  {
${sp}    java.util.Map map;
${map}
${sp}    return map;
${sp}  }
${sp}  private static java.util.Map map = getMap ();
${sp}
${sp}  /** Return the ${class} object that matches the string.  */
${sp}  public static $class valueOf (String string)
${sp}  {
${sp}    return (${class})map.get (string);
${sp}  }
${sp}
${sp}  /** Return the ${class} object that matches the integer.  */
${sp}  public static $class valueOf (long i)
${sp}  {
${sp}    switch ((int)i) {${valueOf}
${sp}    default: return null;
${sp}    }
${sp}  }
${sp}
${sp}  /** Return an array of all the ${class} elements.  */
${sp}  public static ${class}[] values ()
${sp}  {
${sp}    return (${class}[]) map.values ().toArray (new ${class}[0]);
${sp}  }
${sp}
${sp}  /**
${sp}   * Returns the full underscore delimited name of the
${sp}   * field corresponding to the value I.
${sp}   */
${sp}  static public String toString (long i)
${sp}  {
${sp}    ${class} c = valueOf (i);
${sp}    if (c == null)
${sp}      return "${_path}_0x" + Long.toHexString (i);
${sp}    else
${sp}      return c.toString ();
${sp}  }
${sp}
${sp}  /**
${sp}   * Returns the printable (or user readable) name for the
${sp}   * field corresponding to the value I.
${sp}   */
${sp}  static public String toPrintString (long i)
${sp}  {
${sp}    ${class} c = valueOf (i);
${sp}    if (c == null)
${sp}      return "${_path}_0x" + Long.toHexString (i);
${sp}    else
${sp}      return c.toPrint ();
${sp}  }
${sp}
${sp}  /**
${sp}   * Returns the printable (or user readable) name for the
${sp}   * field corresponding to the value I, or DEF is there
${sp}   * is no such field.
${sp}   */
${sp}  static public String toPrintString (long i, String def)
${sp}  {
${sp}    ${class} c = valueOf (i);
${sp}    if (c == null)
${sp}      return def;
${sp}    else
${sp}      return c.toPrint ();
${sp}  }
${sp}}
EOF
}

# The command line argument is the name of the file to create; but the
# actual java is written to stdout.

package=`dirname $1 | tr '[/]' '[.]'`
info Package $package
echo "package $package;"
echo ""

# Prime the look-a-head pump.
get_token

class=`basename $1 .java`
parse_class "" "public" ${class} ${class}

if get_token ; then
    fatal "Garbage at end of file"
fi
