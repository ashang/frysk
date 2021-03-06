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
from dogtail.config import config

# Setup to parse test input data (XML file)
import xml.sax
import FryskHandler
import sys
import os

import re

# Set up for logging
import dogtail.tc

# Set up for unit test framework
import unittest

# Test support functions
from FryskHelpers import *
from fryskTestCase import fryskTestCase

class eventViewerAccessibilityInfo (fryskTestCase):

    def setUp(self):
        fryskTestCase.setUp(self);
        
        # Load up Session object
        self.parser = xml.sax.make_parser(  )
        self.handler = FryskHandler.FryskHandler(  )
        self.parser.setContentHandler(self.handler)

        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. 
        self.parser.parse(os.getenv('TestDruid_FILE') )
        self.theSession = self.handler.theDebugSession

        # Create a Frysk session - True = quit the FryskGui after
        # creating the session
        createMinimalSession (self.frysk, self.theSession, False, False)
        
    def tearDown(self):    
        pass
        
    def testEVAccessibilityInfo(self):  
        """test = eventViewerAccessibilityInfotestEVAccessibilityInfo - Test that EV provides A11y info""" 
        
#        if brokenTest(3282):
#            return
                             
        
        monitor = self.frysk.child(MONITOR)

        #procBox = self.frysk.child(description = 'funit-childTimeLinesVBox')
        #procBox = self.frysk.findChild(predicate.GenericPredicate(name = name, roleName = roleName, description= description, label = label), recursive = recursive, debugName=debugName))
#       procBox.blink(10);

        timeLines =  self.frysk.findChildren(predicate.GenericPredicate(description = 'TimeLine'))
        timeLine = timeLines[0]
        #timeLine = procBox.children[0]
#       timeLine.blink(10);
         
          
        if (timeLine == 0):
            self.fail ( 'Error - TimeLine cannot be found' )
            
        statusWidget = monitor.child('statusWidget')
        
        #Ensure that there are the proper number of monitors.

        # Positive test
        if len(timeLine.children) == 0:
            self.fail ( 'Error - TimeLine does not provide A11y info for events' )
          
       
def suite():
    suite = unittest.TestSuite()
    suite.addTest(eventViewerAccessibilityInfo('testEVAccessibilityInfo'))
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner(verbosity=2).run(suite())

