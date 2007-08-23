TARGETS = \
Udb

all::$(TARGETS)

%:%.o
	gcj -o $@ --main=$@ $^

%.o:%.java
	gcj -c $<

clean:
	rm -f *.o *.class

veryclean: clean
	rm -f $(TARGETS) *~

Udb: Udb.o RolloverSpinnerListModel.o
