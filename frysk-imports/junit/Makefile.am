# This file is part of the program FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# FRYSK is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
# 
# In addition, as a special exception, Red Hat, Inc. gives You the
# additional right to link the code of FRYSK with code not covered
# under the GNU General Public License ("Non-GPL Code") and to
# distribute linked combinations including the two, subject to the
# limitations in this paragraph. Non-GPL Code permitted under this
# exception must only link to the code of FRYSK through those well
# defined interfaces identified in the file named EXCEPTION found in
# the source code files (the "Approved Interfaces"). The files of
# Non-GPL Code may instantiate templates or use macros or inline
# functions from the Approved Interfaces without causing the
# resulting work to be covered by the GNU General Public
# License. Only Red Hat, Inc. may make changes or additions to the
# list of Approved Interfaces. You must obey the GNU General Public
# License in all respects for all of the FRYSK code and other code
# used in conjunction with FRYSK except the Non-GPL Code covered by
# this exception. If you modify this file, you may extend this
# exception to your version of the file, but you are not obligated to
# do so. If you do not wish to provide this exception without
# modification, you must delete this exception statement from your
# version and license this file solely under the GPL without
# exception.

# The target we want to build.
noinst_DATA = junit.jar

# Make a jar file.
junit.jar: classdir
	cd classdir ; \
	find * -name '*.class' -print | $(JAR) -@ -cf $@.tmp
	mv classdir/$@.tmp $@

classdir:
	rm -rf classdir tmpdir
	mkdir -p tmpdir
	classdir=`cd tmpdir && pwd` ; \
	cd $(srcdir)/src ; \
	$(GCJ) -C -I -O -g -d $$classdir @../file.list
	mv tmpdir classdir

CLEANFILES = junit.jar
clean-local:
	rm -rf classdir # directory

EXTRA_DIST =
EXTRA_DIST += file.list
EXTRA_DIST += src/junit/swingui/ProgressBar.java
EXTRA_DIST += src/junit/swingui/StatusLine.java
EXTRA_DIST += src/junit/swingui/TestRunContext.java
EXTRA_DIST += src/junit/swingui/AboutDialog.java
EXTRA_DIST += src/junit/swingui/TestSuitePanel.java
EXTRA_DIST += src/junit/swingui/TestRunView.java
EXTRA_DIST += src/junit/swingui/TestHierarchyRunView.java
EXTRA_DIST += src/junit/swingui/TestSelector.java
EXTRA_DIST += src/junit/swingui/FailureRunView.java
EXTRA_DIST += src/junit/swingui/TestTreeModel.java
EXTRA_DIST += src/junit/swingui/TestRunner.java
EXTRA_DIST += src/junit/swingui/CounterPanel.java
EXTRA_DIST += src/junit/swingui/DefaultFailureDetailView.java
EXTRA_DIST += src/junit/runner/ReloadingTestSuiteLoader.java
EXTRA_DIST += src/junit/runner/Sorter.java
EXTRA_DIST += src/junit/runner/StandardTestSuiteLoader.java
EXTRA_DIST += src/junit/runner/ClassPathTestCollector.java
EXTRA_DIST += src/junit/runner/TestCollector.java
EXTRA_DIST += src/junit/runner/TestCaseClassLoader.java
EXTRA_DIST += src/junit/runner/FailureDetailView.java
EXTRA_DIST += src/junit/runner/TestSuiteLoader.java
EXTRA_DIST += src/junit/runner/TestRunListener.java
EXTRA_DIST += src/junit/runner/Version.java
EXTRA_DIST += src/junit/runner/BaseTestRunner.java
EXTRA_DIST += src/junit/runner/SimpleTestCollector.java
EXTRA_DIST += src/junit/runner/LoadingTestCollector.java
EXTRA_DIST += src/junit/awtui/ProgressBar.java
EXTRA_DIST += src/junit/awtui/AboutDialog.java
EXTRA_DIST += src/junit/awtui/Logo.java
EXTRA_DIST += src/junit/awtui/TestRunner.java
EXTRA_DIST += src/junit/framework/AssertionFailedError.java
EXTRA_DIST += src/junit/framework/TestCase.java
EXTRA_DIST += src/junit/framework/Protectable.java
EXTRA_DIST += src/junit/framework/TestFailure.java
EXTRA_DIST += src/junit/framework/TestResult.java
EXTRA_DIST += src/junit/framework/TestListener.java
EXTRA_DIST += src/junit/framework/TestSuite.java
EXTRA_DIST += src/junit/framework/ComparisonFailure.java
EXTRA_DIST += src/junit/framework/Test.java
EXTRA_DIST += src/junit/framework/Assert.java
EXTRA_DIST += src/junit/textui/ResultPrinter.java
EXTRA_DIST += src/junit/textui/TestRunner.java
EXTRA_DIST += src/junit/extensions/TestSetup.java
EXTRA_DIST += src/junit/extensions/TestDecorator.java
EXTRA_DIST += src/junit/extensions/ExceptionTestCase.java
EXTRA_DIST += src/junit/extensions/ActiveTestSuite.java
EXTRA_DIST += src/junit/extensions/RepeatedTest.java
