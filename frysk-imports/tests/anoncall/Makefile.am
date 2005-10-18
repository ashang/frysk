# The idea is simple, validate that this works before trying to build
# anything else.

# Passes with gcc 4.0.1.

DIST_SOURCES = $(srcdir)/Anon.java 

all-local: TEST
TEST:
	gcj -g --main=Anon $(srcdir)/Anon.java -o Anon
	if ./Anon ; then touch PASS ; else touch FAIL ; fi
	touch TEST

clean-local:
	rm -f Anon Anon*.class FAIL PASS TEST

