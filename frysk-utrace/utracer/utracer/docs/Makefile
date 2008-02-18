DOCBOOKMAN = docbook2man
DOCBOOKPDF = docbook2pdf
DOCBOOKTXT = docbook2txt

TARGET: allpages

BODIES = utracer_open_body.sgml \
	 utracer_close_body.sgml \
	 utracer_sync_body.sgml \
	 utracer_attach_body.sgml \
	 utracer_detach_body.sgml \
	 utracer_run_body.sgml \
	 utracer_check_pid_body.sgml

DERIVED_PAGES = utracer_wait.2 utracer_quiesce.2

%.7:%.sgml
	$(DOCBOOKMAN) $<

%.2:%.sgml
	$(DOCBOOKMAN) $<

pages: utracer.sgml $(BODIES)
	$(DOCBOOKMAN) $<

allpages: pages $(DERIVED_PAGES)

utracer.pdf: utracer.sgml $(BODIES)
	$(DOCBOOKPDF) $<

pdf: utracer.pdf

html: utracer.html

utracer.html: utracer.sgml $(BODIES)
	docbook2html -u utracer.sgml

utracer.txt: utracer.sgml $(BODIES)
	$(DOCBOOKTXT) $<

txt: utracer.txt

utracer_open.2:		utracer_open.sgml	utracer_open_body.sgml
utracer_close.2:	utracer_close.sgml	utracer_close_body.sgml
utracer_sync.2:		utracer_sync.sgml	utracer_sync_body.sgml
utracer_attach.2:	utracer_attach.sgml	utracer_attach_body.sgml
utracer_detach.2:	utracer_detach.sgml	utracer_detach_body.sgml
utracer_run.2:		utracer_run.sgml	utracer_run_body.sgml
utracer_check_pid.2:	utracer_check_pid.sgml	utracer_check_pid_body.sgml
utracer.7:		utracer_intro.sgml	utracer_intro_body.sgml

utracer_wait.2: utracer_sync.2
	echo ".so $<" > $@

utracer_quiesce.2: utracer_run.2
	echo ".so $<" > $@

clean:
	rm -f *.2 *.7 *.pdf *.txt manpage.* 
