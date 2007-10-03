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
from FryskHelpers import extractString
from FryskHelpers import removeAfterTab
from FryskHelpers import findProcessNames
from FryskHelpers import createProcessDict
from FryskHelpers import startFrysk
from FryskHelpers import endFrysk
from FryskHelpers import skipDruid
from FryskHelpers import FRYSK_SESSION_FILES

class TestDruid ( unittest.TestCase ):

   def setUp( self ):
       
       # Set up for logging
        self.TestString=dogtail.tc.TCString()
        self.theLogWriter = self.TestString.writer
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' starting'  })

        # Start up Frysk 
        self.FryskBinary = sys.argv[1]
        self.frysk = startFrysk(self.FryskBinary, self.theLogWriter)

        # Load up some sample Observer objects 
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

        theProcessGroups = self.theSession.getProcesses()
        userSelectedProcessGroups = []
        for x in theProcessGroups:
            userSelectedProcessGroups.append(x.getName())

        userSelectedObserverDict = {}
        for x in theProcessGroups:
            tempProcess = x.getName().encode('ascii')
            userSelectedObserverDict[x.getName()] = x.getObservers()
    
        # ---------------------
        # Access the Druid GUI
        theDruid = self.frysk.dialog('Debug Session Druid')

        # And the GUI's 'notebook' of pages
        vbox1 = theDruid.child('dialog-vbox1')
        sessionDruid_sessionNoteBook = vbox1.child('sessionDruid_sessionNoteBook')

        # ---------------------
        # The action buttons are displayed on the bottom of all pages - the
        # specific buttons (Back, Forward, Finish) that are visible or enabled
        # varies with the page - and the current state of the page
        dialogActionArea1 = vbox1.child('dialog-action_area1')
        forwardButton = dialogActionArea1.button('Forward')
        backButton = dialogActionArea1.button('Back')
        finishButton = dialogActionArea1.button('Finish')
        saveButton = dialogActionArea1.button('Save')
        cancelButton = dialogActionArea1.button('Cancel')

        # ---------------------
        # page #1 - vbox42_tab1_session - Select new/old session, specify
        # the debugger - note that as of May 8, 2006, selecting an existing
        # debug session is not implemented in the GUI - so, for now, we'll 
        # always create a new session

        vbox42_tab1_session = sessionDruid_sessionNoteBook.child('vbox42_tab1_session')
        newDebugSession = vbox42_tab1_session.child('sessionDruid_newSessionButton')
        newDebugSession.click()
        newSessionName = vbox42_tab1_session.child(name = 'sessionDruid_sessionName', roleName='text')
        newSessionName.text = self.theSession.getName()
        forwardButton.click()

        # ---------------------
        # page #2 - vbox43_tab2_processGroups - Select process groups to monitor

        vbox43_tab2_processGroups = sessionDruid_sessionNoteBook.child('vbox43_tab2_processGroups')

        # Select the host to monitor - for now, only local host works, so take no action
        hbox61_tab2_host = vbox43_tab2_processGroups.child('hbox61_tab2_host')  
        hbox62_tab2_groupLists = vbox43_tab2_processGroups.child('hbox62_tab2_groupLists')

        # Need to add a test here - the forwardButton is not sensitive until at least 
        # one process group is selected - need to try and catch an exception for:
        # (forwardButton.click()

        # The 'from' list is the list from which processes are selected
        vbox45_tab2_fromGroupList = hbox62_tab2_groupLists.child('vbox45_tab2_fromGroupList')
        processGroups = vbox45_tab2_fromGroupList.child('scrolledwindow33')
        processGroups.grabFocus()
        procWiseTreeView = processGroups.child('sessionDruid_procWiseTreeView')

        # These buttons add/remove processes from the selected ('to') list
        vbox44_tab2_groupListButtons = hbox62_tab2_groupLists.child('vbox44_tab2_groupListButtons')
        addButton = vbox44_tab2_groupListButtons.button('sessionDruid_addProcessGroupButton')
        removeButton = vbox44_tab2_groupListButtons.button('sessionDruid_removeProcessGroupButton')

        # Search the process list for a match with the userSelectedProcessGroups List
        theProcessList = procWiseTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)

        processesToMonitor = findProcessNames (userSelectedProcessGroups, theProcessList)
        for processName in processesToMonitor:
            tempProc = procWiseTreeView.child(processName)
            tempProc.grabFocus()
            tempProc.actions['activate'].do()
            addButton.click()
        # Need to add a test here for the removeButton
        # Need to add a test here for the back button - both before and after process
        # groups are selected

        forwardButton.click()

        # ---------------------
        # page 4 - hbox83_tab4_tagSets - Select tag sets - not really implemented yet

        hbox83_tab4_tagSets = sessionDruid_sessionNoteBook.child('hbox83_tab4_tagSets')
        forwardButton.click()

        # ---------------------
        # page 5 - hbox77_tab5_observers - Select process groups and observers

        hbox77_tab5_observers = sessionDruid_sessionNoteBook.child('hbox77_tab5_observers')
        vbox52_tab5_processGroups = hbox77_tab5_observers.child('vbox52_tab5_processGroups')
        SessionDruid_processObserverTreeView = vbox52_tab5_processGroups.child('SessionDruid_processObserverTreeView')
        theProcessList = SessionDruid_processObserverTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)

        for processName in theProcessList:
            resolvedName = extractString (str(processName), 'name')
            tempProc = SessionDruid_processObserverTreeView.child(resolvedName)
            tempProc.grabFocus()
            tempProc.actions['activate'].do()

            vbox54_tab5_observers = hbox77_tab5_observers.child('vbox54_tab5_observers')
            SessionDruid_observerTreeView = vbox54_tab5_observers.child('SessionDruid_observerTreeView')
            theList = SessionDruid_observerTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)
            theDictionary = createProcessDict (theList)
            userSelectedObservers = userSelectedObserverDict[resolvedName]
    
            for x in userSelectedObservers:
                tempObserver = theDictionary[x]
                tempObserver.actions['toggle'].do()

        forwardButton.click()

        # Close the Druid
        finishButton.click()  

   def testSessionFile( self ):      
        """Verify that the session object just created and presisted under $HOME/.frysk/Sessions
           matches the test input"""   
  
        # ---------------------
        # Verify that the session object just created and presisted under $HOME/.frysk/Sessions
        # matches the test input

        newlyCreatedSessionFile =  FRYSK_SESSION_FILES + self.theSession.getName()
        self.parser.parse(newlyCreatedSessionFile)
        newlyCreatedSession = self.handler.theDebugSession

        newlyCreatedSessionProcesses = newlyCreatedSession.getProcesses()
        self.theSessionProcesses = self.theSession.getProcesses()

        newlyCreatedSession.setProcessesDict(newlyCreatedSessionProcesses)
        self.theSession.setProcessesDict(self.theSessionProcesses)

        if self.theSession.isequal (newlyCreatedSession):
            self.TestString.compare(self.theLogWriter.scriptName + '.testSessionFile()', newlyCreatedSession.getName(), self.theSession.getName() )
            self.assertEqual(newlyCreatedSession.getName(), self.theSession.getName() )            
        else:
            self.fail ('FAIL - the session objects do not match')

   def tearDown( self ):  
       # Exit Frysk
       endFrysk( self.frysk )
       self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  })
 
def suite():
        suite = unittest.TestSuite()
        suite.addTest( unittest.makeSuite( TestDruid ) )
        return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=2 ).run( suite() )
