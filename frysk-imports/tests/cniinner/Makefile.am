# The idea is simple, validate that this works before trying to build
# anything else.

all-local: PASS

PASS:
	gcj -I$(srcdir)/.. -d .. -C $(srcdir)/Parent.java
	gcj -I$(srcdir)/.. -d .. -C $(srcdir)/Child.java
	gcjh -I.. cniinner.Parent -o Parent.h
	gcjh -I.. cniinner.Child -o Child.h
	gcjh -I.. 'cniinner.Child$$Nested' -o 'Child$$Nested.h'
	gcj -g -I.. --main=cniinner.Child \
		$(srcdir)/Parent.java \
		$(srcdir)/Child.java \
		$(srcdir)/native.cxx
	./a.out
	touch PASS
