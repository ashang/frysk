#!/bin/sh

cat <<EOF

This script fetches all the frysk bugs as XML.  It is very heavy on
the poor server, and to help reduce the load, it sleeps between wgets.

Use with discretion!

EOF

echo -n "Do you want to continue [n]? "
read n
case "$n" in
  y|yes ) ;;
  * ) exit 1 ;;
esac

wget \
    -nv \
    -q \
    -O - \
    'http://sourceware.org/bugzilla/buglist.cgi?product=frysk&ctype=csv' \
    | ( read first_line ; cut -d, -f1 ) | \
while read id ; do
    rm -f xml.tmp
    if test -r $id.xml ; then
	echo "$id already present"
    else
	sleep 2
	wget \
	    -nv \
	    -O $id.tmp \
	    'http://sourceware.org/bugzilla/show_bug.cgi?id='$id'&ctype=xml'
	mv $id.tmp $id.xml
    fi
done
