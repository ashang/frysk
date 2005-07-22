#!/bin/sh -eu

# Generate everything (always run with --add-missing).

sh -eu ./common/Makefile.gen.sh com

echo "Running aclocal ..."
aclocal

echo "Running autoconf ..."
autoconf

echo "Running automake ..."
automake --add-missing
