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
Script name:    TestDruid.py
Creation date:  May 2006
Purpose:        Verify creation of Frysk Debug Session Druid
Summary:        Simple, demo/prototype dogtail test script for Frysk
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# Imports
from dogtail import tree
from dogtail.utils import run
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

# The Frysk test objects
from Observer import Observer
from DebugProcess import DebugProcess
from DebugSession import DebugSession

# Support routines for Frysk GUI Dogtail test scripts
from FryskHelpers import *
from fryskTestCase import fryskTestCase

class stressDruid ( fryskTestCase ):

   def setUp(self):

        fryskTestCase.setUp(self)
       
        # Load up Session object
        self.parser = xml.sax.make_parser(  )
        self.handler = FryskHandler.FryskHandler(  )
        self.parser.setContentHandler(self.handler)

        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script.
        self.parser.parse(os.getenv('TestDruid_FILE') )
        self.theSession = self.handler.theDebugSession
        print "self.theSession.getProcesses()1 ",self.theSession.getProcesses(),"\n"
        
        # Create a Frysk session - True = quit the FryskGui after
        # creating the session
        #createBigSession (self.frysk, self.theSession, False)
        createMinimalSession (self.frysk, self.theSession, False)
        print "self.theSession.getProcesses()2 ",self.theSession.getProcesses(),"\n"
        
   def testSessionFile( self ):      
        """test = stressDruid.testSessionFile - Verify that the session object just created and presisted under $HOME/.frysk/Sessions
           matches the test input"""   
  
        # ---------------------
        # Verify that the session object just created and presisted under $HOME/.frysk/Sessions
        # matches the test input

        newlyCreatedSessionFile =  FRYSK_SESSION_FILES + self.theSession.getName()
        self.parser.parse(newlyCreatedSessionFile)
        newlyCreatedSession = self.handler.theDebugSession

        newlyCreatedSessionProcesses = newlyCreatedSession.getProcesses()
        self.theSessionProcesses = self.theSession.getProcesses()
        print "self.theSession.getProcesses()3 ",self.theSession.getProcesses(),"\n"
        
        newlyCreatedSession.setProcessesDict(newlyCreatedSessionProcesses)
        self.theSession.setProcessesDict(self.theSessionProcesses)
        print "self.theSession.getProcesses()4 ",self.theSession.getProcesses(),"\n"
        
        if self.theSession.isequal (newlyCreatedSession):
            self.TestString.compare(self.theLogWriter.fileName + '.testSessionFile()', newlyCreatedSession.getName(), self.theSession.getName() )
            self.assertEqual(newlyCreatedSession.getName(), self.theSession.getName() )            
        else:
            self.fail ('FAIL - the session objects do not match')

   def tearDown(self):    
        # Exit Frysk
        endFrysk (self.startObject)
        self.theLogWriter.log({'INFO' :  'test script: ' + self.theLogWriter.fileName + ' ending'  })
  
 
def suite():
        suite = unittest.TestSuite()
        suite.addTest( stressDruid ( 'testSessionFile' ) )
        return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=2 ).run( suite() )
