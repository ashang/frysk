# The idea is simple, validate that this works before trying to build
# anything else.

all-local: FAIL

FAIL:
	gcj -g --main=Anon $(srcdir)/Anon.java -o Anon
	if ./Anon ; then touch PASS ; else touch FAIL ; fi
	test -r FAIL

clean-local:
	rm Anon Anon*.class
