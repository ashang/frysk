#!/bin/sh

READELF=readelf
readelf=./prog/readelf/readelf

TIME ()
{
    # On PPC both I/O and faults give back zero
    /usr/bin/time \
	--format=' real %e user %U sys %S faults %F/%R switch %c waits %w' \
	"$@"
}

if test $# -eq 0 ; then
    cat <<EOF
-e
--headers
-g
--section-groups
-h
--file-header
-l
--program-headers
--segments
-S
--section-headers
--sections
-s
--syms
--symbols
--debug-dump=abbrev
--debug-dump=frames-interp
--debug-dump=frames
--debug-dump=info
--debug-dump=line
--debug-dump=pubnames
--debug-dump=ranges
--debug-dump=aranges
--debug-dump=str
--debug-dump=Ranges
--debug-dump=loc
--debug-dump=macro
EOF
else
    for o in "$@" ; do
	printf '%s\n' $o
    done
fi | while read opt ; do

    dir=tmp/$opt
    mkdir -p ${dir}

    echo "Option: ${opt}"
    TIME ${readelf} --wide ${opt} ${readelf} | sed -e 's/@.*$//' > /tmp/$$.gdi
    TIME ${READELF} --wide ${opt} ${readelf} | sed -e 's/@.*$//' > /tmp/$$.gnu
    mv /tmp/$$.gdi $dir/gdi
    mv /tmp/$$.gnu $dir/gnu
    if cmp $dir/gdi $dir/gnu; then
	:
    else
	diff -u $dir/gdi $dir/gnu | head -20
	exit 1
    fi
done
