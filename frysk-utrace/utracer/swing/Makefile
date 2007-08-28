TARGETS = \
Udb

all::$(TARGETS)

%:%.o
	gcj -o $@ --main=$@ $^

%.o:%.java
	gcj -c $<


Udb: Udb.o RolloverSpinnerListModel.o Utrace.o cni/Utrace.o

Udb.o:				Udb.java
RolloverSpinnerListModel.o:	RolloverSpinnerListModel.java
Utrace.o:			Utrace.java

cni/Utrace.o:			cni/Utrace.cxx cni/Utrace.h
	gcc -o $@ -c $<

cni/Utrace.h:	Utrace.class
	gcjh  Utrace -o ./cni/Utrace.h

Utrace.class:	Utrace.java
	javac Utrace.java

clean:
	rm -f *.o *.class cni/*.o cni/Utrace.h

veryclean: clean
	rm -f $(TARGETS) *~ cni/*~
