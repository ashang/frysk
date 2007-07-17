#!/bin/sh

READELF=readelf
readelf=./inua/util/readelf

TIME ()
{
    # On PPC both I/O and faults give back zero
    /usr/bin/time \
	--format=" real %e user %U sys %S faults %F/%R switch %c waits %w $1" \
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

    opts="${opt} --wide ${readelf}"
    echo "Options: ${opts}"
    cat <<EOF | while read file command ; do
gdi ${readelf}
gnu ${READELF}
EOF
        TIME ${command} ${opts} | case "${opt}" in
#	      --debug-dump=frame* )
#	          # XXX: Discard the .eh_frame section, contains really weird
#	          # auxiliary information.
#	          sed -e '/The section .eh_frame contains:/,/The section .debug_frame contains:/ d'
#	          ;;
	      * ) sed -e 's/@.*$//' ;
              esac  > /tmp/$$.${file}
	mv /tmp/$$.${file} $dir/${file}
    done
    if cmp $dir/gdi $dir/gnu; then
	:
    else
	diff -u $dir/gdi $dir/gnu | head -20
	exit 1
    fi
done
