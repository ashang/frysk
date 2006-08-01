#! /usr/bin/env python
# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, Red Hat Inc.
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

'''
Script name:    TestEventViewerMarkers.py
Script author:  npremji
Creation date:  July 2006
Purpose:        Verify eventviewer markers can be seen.
Summary:        Simple, demo/prototype dogtail test script for Fryske
'''

__author__ = 'Nurdin Premji <npremji@redhat.com>'

# Imports
from dogtail import tree
from dogtail import predicate

# Setup to parse test input data (XML file)
import xml.sax
import FryskHandler
import sys
import os

# Set up for logging
import dogtail.tc

# Set up for unit test framework
import unittest

# Test support functions
from FryskHelpers import *

class TestCredits (unittest.TestCase):

    def setUp(self):
        
        # Set up for logging
        self.TestString=dogtail.tc.TCString()
        self.theLogWriter = self.TestString.writer
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' starting'  })

        # Start up Frysk 
        self.FryskBinary = sys.argv[1]
        self.frysk = startFrysk(self.FryskBinary, self.theLogWriter)
        
        # Load up Session object
        self.parser = xml.sax.make_parser(  )
        self.handler = FryskHandler.FryskHandler(  )
        self.parser.setContentHandler(self.handler)
       
        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. As of June 8, 2006, there's a problem with 
        # the test suite - either Frysk or Dogtail gets confused and attempts
        # to run tests before other tests have completed - short-term workaround
        # is to comment out these lines, run the tests separately, and read
        # the datafiles from the CLI       
        self.parser.parse(sys.argv[2])
        #inputFile = os.environ.get('TestDruid_FILE')
        #self.parser.parse(inputFile)

        self.theSession = self.handler.theDebugSession

        # Create a Frysk session - True = quit the FryskGui after
        # creating the session
        createMinimalSession (self.frysk, self.theSession, False)
        
    def tearDown(self):    
        # Exit Frysk
        endFrysk(self.frysk)
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  })


    def testEVMarkers(self):  
        monitor = self.frysk.child(MONITOR)
        nautilus = self.frysk.child('nautilus')
        nautilus.grabFocus()
        statusWidget = monitor.child('statusWidget')

        #Ensure that there are the proper number of monitors.
        #Terminating
        terminatingObserver = statusWidget.child('Terminating Observer')
        #Exec
        execObserver = statusWidget.child('Exec Observer')
        #Fork
        forkObserver = statusWidget.child('Fork Observer')
       
def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(TestCredits))
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner(verbosity=2).run(suite())

