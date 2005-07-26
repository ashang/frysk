#!/bin/sh -eu

for d in */autogen.sh ; do
    ( cd `dirname $d` && ./autogen.sh )
done

# Generate everything (always run with --add-missing).

echo "Running aclocal ..."
aclocal

echo "Running autoconf ..."
autoconf

echo "Running automake ..."
automake --add-missing
