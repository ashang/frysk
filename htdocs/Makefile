HTML = \
	index.html \
	build/index.html \
	documentation/index.html \
	getinvolved/index.html \
	people/index.html \
	questions/index.html \
	usecases/index.html \
	workflow/index.html \
	zilla/index.html \
	todo/index.html \
	bugs/index.html \
	bugzilla/index.html \
	$(ZZZ)
all: $(HTML:.html=.new)
.PHONY: all

clean:
	rm -f *.new */*.new
.PHONY: clean

BUILD_NEW = @\
	d=`dirname ./$@` ; \
	case $$d in \
	  . ) dots= ; ;; \
	  * ) dots=../ ;; \
	esac ; \
	b=`basename $@ .new` ; \
	h=$$d/$$b.html ; \
	rm -f $@.tmp ; \
	echo "Verfying $$h:" ; \
	ok=: ; \
	for b in title logo text ; do \
	  for a in start end ; do \
	    if grep -e "-- $$a $$b --" $$h ; then \
	      : ; \
	    else \
	      echo "$$h: missing -- $$a $$b --" ; \
	      ok=false ; \
	    fi ; \
	  done ; \
	done ; \
	$$ok ; \
	echo "Building $@:" ; \
	( \
	  dotdot=.. ; \
	  sed -e '/<title>/,$$ d' template.html ; \
	  sed -n -e '/<title>/ p' $$h ; \
	  sed -e '1,/<\/title>/ d' -e '/-- start title --/,$$ d' template.html ; \
	  sed -n -e '/-- start title --/,/-- end title --/ p' $$h ; \
	  sed -e '1,/-- end title --/ d' -e '/-- start logo --/,$$ d' template.html ; \
	  sed -n -e '/-- start logo --/,/-- end logo --/ p' $$h ; \
	  sed -e '1,/-- end logo --/ d' -e '/-- start text --/,$$ d' template.html ; \
	  sed -n -e '/-- start text --/,/-- end text --/ p' $$h ; \
	  sed -e '1,/-- end text --/ d' template.html ; \
	) | sed \
		-e 's,<a.*="'$$d'/index.html">\(.*\)</a>,\1,' \
		-e 's,="./\(.*\)index.html",="'$$dots'\1",' \
		-e 's,="./,="'$$dots',' \
	> $@.tmp ; \
	mv $@.tmp $@

index.new: template.html Makefile
	$(BUILD_NEW)
%/index.new: template.html Makefile
	$(BUILD_NEW)

diff compare: $(HTML:.html=.new)
	for f in $(HTML) ; do \
	  new=`dirname $$f`/`basename $$f .html`.new ; \
	  echo $$f ; \
	  diff -u $$f $$new || : ; \
	done
.PHONY: diff compare

emacs:
	for f in $(HTML) ; do \
	  echo $$f ; \
	  emacs $$f ; \
	done

install: $(HTML:.html=.new)
	for f in $(HTML) ; do \
	  new=`dirname $$f`/`basename $$f .html`.new ; \
	  cp $$new $$f ; \
	done
.PHONY: install
