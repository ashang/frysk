#installed
#   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar
#
#    LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib

#local
#   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar:/usr/local/share/java/gtk2.8-2.8.0.jar:../../ftk.jar
   CLASSPATH=.:.:/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre/lib/rt.jar::../../../../../frysk-build/frysk-imports/ftk.jar

#   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:../..
   LD_LIBRARY_PATH=/usr/local/qt/lib:/usr/local/lib:../../../../../frysk-build/frysk-imports/tlwidgets

all::run

StripchartTest.class: StripchartTest.java
	CLASSPATH=$(CLASSPATH) gcj -C $<

run: StripchartTest.class
	LD_LIBRARY_PATH=$(LD_LIBRARY_PATH) gij -cp $(CLASSPATH) StripchartTest

clean:
	rm -f *.class
