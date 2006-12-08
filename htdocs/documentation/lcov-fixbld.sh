# Do not build fryski.  It does not link with --coverage
for i in frysk-core/Makefile frysk-gui/Makefile
do
cp $i $i.sv
ex $i  <<END
g/^GCJFLAGS = / s/-O//
g/^GCJFLAGS = / s/$/ --coverage -O0
/^fryski.*EXE/,/^fryski.*EXE/+2s/^/# /
/noinst_PROGRAMS/ s/fryski..EXEEXT.//
w
q
END
done

# Fix frysk-gui link lines for --coverage
ex frysk-gui/Makefile <<EOF
/^frysk.gui.FryskGui..EXEEXT./,/^frysk.gui.FryskGui..EXEEXT./+2 s/_LINK.*\$/& --coverage
/^frysk.bindir.frysk..EXEEXT./,/^frysk.bindir.frysk..EXEEXT./+2 s/_LINK.*\$/& --coverage
/^TestRunner..EXEEXT./,/^TestRunner..EXEEXT./+2 s/GCJLINK.*\$/& --coverage
/^frysk.gui.DummySourceWindow\$(EXEEXT)/,/^frysk.gui.DummySourceWindow\$(EXEEXT)/+2 s/_LINK.*\$/& --coverage
w
q
EOF


