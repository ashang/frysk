all: \
	index.html \
	build/index.html \
	documentation/index.html \
	getinvolved/index.html \
	links/index.html \
	people/index.html \
	questions/index.html \
	usecases/index.html \
	workflow/index.html
.PHONY: all

clean:
	rm -f *.new */*.new
	rm -f *.old */*.old
.PHONY: clean

REBUILD = @\
	test -r $@.old || cp $@ $@.old ; \
	rm -f $@.new ; \
	echo "Verfying $@.old:" ; \
	for b in title logo text ; do \
	  for a in start end ; do \
	    if grep -e "-- $$a $$b --" $@.old ; then \
	      : ; \
	    else \
	      echo "$@.old: missing -- $$a $$b --" ; \
	      exit 1 ; \
	    fi ; \
	  done ; \
	done ; \
	echo "Building $@.new:" ; \
	( \
	  sed -n '1,/-- start title --/ p' template.html ; \
	  sed -e '1,/-- start title --/ d' -e '/-- end title --/,$$ d' $@.old ; \
	  sed -n '/-- end title --/,/-- start logo --/ p' template.html ; \
	  sed -e '1,/-- start logo --/ d' -e '/-- end logo --/,$$ d' $@.old ; \
	  sed -n '/-- end logo --/,/-- start text --/ p' template.html ; \
	  sed -e '1,/-- start text --/ d' -e '/-- end text --/,$$ d' $@.old ; \
	  sed -n '/-- end text --/,$$ p' template.html ; \
	) > $@.new ; \
	cp $@.new $@ ; \
	:

index.html: template.html Makefile
	$(REBUILD)
%/index.html: template.html Makefile
	$(REBUILD)
