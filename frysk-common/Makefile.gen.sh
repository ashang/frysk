#!/bin/sh -eu

if test $# -eq 0 ; then
    echo "Usage: $0 directory ..." 1>&2
    exit 1
fi
dirs="$*"

# Generate the list of source files

echo Creating Makefile.gen from directories ${dirs} ...

rm -f Makefile.gen

# Accumulate all the sources, each file type with its own category.

print_header ()
{
    cat <<EOF >> Makefile.gen

# Re-generate using "make autogen"
# $@
EOF
    echo "$@"
}

print ()
{
    echo "$@" >> Makefile.gen
}


print_header List of sub-directories.
print 'GEN_DIRS =' ${dirs}

# Traverse the source tree creating a list (in GEN_SOURCES) of all the
# source files (exclude Test files as they are handled later).

print "GEN_SOURCES ="
cat <<EOF | while read suffix ; do
.java
.cxx
.mkjava
.shjava
EOF
    print_header "... GEN_SOURCES += *${suffix}"
    print "GEN_SOURCES += \\"
    find ${dirs} \
	-name '*Test*' -prune -o \
	-name "*${suffix}" -print \
	| while read file ; do
	    d=`dirname ${file}`
	    b=`basename ${file} ${suffix}`
	    case "${file}" in
		*.java )
		    test -r "${d}/${b}.mkjava" && continue
		    test -r "${d}/${b}.shjava" && continue
		    ;;
	    esac
	    print "	${file} \\"
        done
    print '	$(ZZZ)'
done
print ""


# Traverse the source tree creating a list (in GEN_SOURCES) of all the
# .classes files that will be built (exclude Test files as they are
# handled later).

print "GEN_BUILT_CLASSES ="
cat <<EOF | while read suffix ; do
java
mkjava
shjava
EOF
    print_header "... GEN_BUILT_CLASSES += *.${suffix}"
    print "GEN_BUILT_CLASSES += \\"
    find ${dirs} \
	-name "*.${suffix}" -print \
	| while read file ; do
	    d=`dirname ${file}`
	    b=`basename ${file} .${suffix}`
	    print "	${d}/${b}.classes \\"
        done
    print '	$(ZZZ)'
done
print ""

# Traverse the source tree creating a list of all the built .java
# files (created from either a .mkjava or a .shjava file).  These need
# to be generated explicitly has otherwize automake gets confused (it
# seems to require files generated using special rules to be listed
# explicitly).

cat <<EOF | while read suffix ; do
mkjava
shjava
EOF
    NAME=`echo ${suffix} | tr '[a-z]' '[A-Z]'`
    print_header "... GEN_${NAME} = *.${suffix}=.java"
    print "GEN_BUILT_${NAME} = \\"
    find ${dirs} -name "*.${suffix}" -print | while read file ; do
        print "	`echo ${file} | sed -e "s/.${suffix}/.java/"` \\"
    done
    print '	$(ZZZ)'
done
print 'BUILT_SOURCES += $(GEN_BUILT_MKJAVA) $(GEN_BUILT_SHJAVA)'
print 'CLEANFILES += $(GEN_BUILT_MKJAVA) $(GEN_BUILT_SHJAVA)'
print ''


# Grep the cni/*.cxx files forming a list of any includes.  Assume
# these are all generated from .class files.  The list can be pruned a
# little since, given Class$Nested and Class, generating Class.h will
# automatically generate the inner Class$Nested class.

print_header "... GEN_BUILT_H  += *.cxx=.h"
print "GEN_BUILT_H = \\"
find ${dirs} -name 'cni' -print | while read d ; do
    find $d -name '*.cxx' -print
done \
    | xargs grep '#include ".*.h"' \
    | sed -e 's/^.*#include "//' -e 's/.h".*$//' -e 's/$.*//' \
    | sort -u \
    | while read c ; do
    test -r $c.java && print "	$c.h \\"
done
print '	$(ZZZ)'
print 'BUILT_SOURCES += $(GEN_BUILT_H)'
print 'CLEANFILES += $(GEN_BUILT_H)'



# Form a list of all the test cases that need to be built.  For any
# java file.  If there's a corresponding cni/*.cxx file add that in,
# ditto for a LibTest.java file.

print_header "... TESTS += Test*.java"
find ${dirs} \
    -name 'Test*.java' -print \
    | while read file ; do
    test=`dirname ${file}`/`basename ${file} .java`
    test_=`echo ${test} | tr '[/]' '[_]'`
    print ""
    files=${file}
    cxx=`dirname ${file}`/cni/`basename ${file} .java`.cxx
    test -r ${cxx} && files="${files} ${cxx}"
    lib=`dirname ${file}`/LibTest.java
    test -r ${lib} && files="${files} ${lib}"
    print "${test_}_SOURCES= ${files}"
    print "${test_}_LINK = \${GCJLINK}"
    print "TESTS += ${test}"
    print "noinst_PROGRAMS += ${test}"
done
