## Process this file with automake to create Makefile.in
##
## Copyright (C) 1996-2001, 2002, 2004, 2005, 2008 Red Hat, Inc.
## This file is part of Red Hat elfutils.
##
## Red Hat elfutils is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by the
## Free Software Foundation; version 2 of the License.
##
## Red Hat elfutils is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
## General Public License for more details.
##
## You should have received a copy of the GNU General Public License along
## with Red Hat elfutils; if not, write to the Free Software Foundation,
## Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.
##
## Red Hat elfutils is an included package of the Open Invention Network.
## An included package of the Open Invention Network is a package for which
## Open Invention Network licensees cross-license their patents.  No patent
## license is granted, either expressly or impliedly, by designation as an
## included package.  Should you wish to participate in the Open Invention
## Network licensing program, please visit www.openinventionnetwork.com
## <http://www.openinventionnetwork.com>.
##
DEFS = -D_GNU_SOURCE -DHAVE_CONFIG_H
if MUDFLAP
AM_CFLAGS = -fmudflap
else
AM_CFLAGS =
endif
AM_CFLAGS += -fpic -Wall -Wshadow -Werror -Wunused -Wextra $($(*F)_CFLAGS)
INCLUDES = -I$(srcdir)/../libelf -I..

noinst_LIBRARIES = libeu.a

libeu_a_SOURCES = xstrndup.c xmalloc.c next_prime.c \
		  crc32.c crc32_file.c md5.c sha1.c

noinst_HEADERS = fixedsizehash.h system.h dynamicsizehash.h list.h md5.h \
		 sha1.h
EXTRA_DIST = dynamicsizehash.c

if !GPROF
xmalloc_CFLAGS = -ffunction-sections
endif
