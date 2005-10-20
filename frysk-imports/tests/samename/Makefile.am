EXTRA_DIST = SameName.java 

all-local: FAIL
FAIL:
	gcj -g --main=SameName $(srcdir)/SameName.java -o SameName
	if ./SameName ; then touch PASS ; else touch FAIL ; fi
	test -r FAIL

clean-local:
	rm -f SameName SameName*.class FAIL PASS TEST
