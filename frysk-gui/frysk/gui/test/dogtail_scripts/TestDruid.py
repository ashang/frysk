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
Creation date:  April 2006
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

# Locate the application
frysk = tree.root.application ('java-gnome')

# ---------------------
# Read the test input - need to make the file name a CLI arg for program
parser = xml.sax.make_parser(  )
handler = FryskHandler.FryskHandler(  )
parser.setContentHandler(handler)
parser.parse(sys.argv[1])
theSession = handler.theDebugSession

theProcessGroups = theSession.getProcesses()
userSelectedProcessGroups = []
for x in theProcessGroups:
    userSelectedProcessGroups.append(x.getName())

userSelectedObserverDict = {}
for x in theProcessGroups:
    tempProcess = x.getName()
    userSelectedObserverDict[x.getName()] = x.getObservers()
    print x.getObservers()

# ---------------------
# Access the Druid GUI
theDruid = frysk.dialog('Debug Session Druid')

# And the GUI's 'notebook' of (6) pages
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
NewDebugSession = vbox42_tab1_session.child('New debug Session')
NewDebugSession.click()

# Select a debugger - for now, let's use /usr/bin/gdb - this needs to be 
# modified to support specifying other debugger
hbox60_tab1_debugger = sessionDruid_sessionNoteBook.child('hbox60_tab1_debugger')
sessionDruid_execChooser= hbox60_tab1_debugger.child('sessionDruid_execChooser')
debugSelectorButton = sessionDruid_execChooser.child(roleName = 'push button')
#debugSelectorButton.click()

# Use the file selector dialog to select /usr/bin/gdb
#fileSelectDialog = frysk.dialog('Select A File')
#shortCuts = fileSelectDialog.child('Shortcuts', roleName = 'table')
#fileSystem = shortCuts.child('File System', roleName = 'table cell')
#fileSystem.actions['activate'].do()
#fileTable = fileSelectDialog.child('Files', roleName = 'table')
#usr = fileTable.child('usr')
#usr.actions['activate'].do()
#bin = fileTable.child('bin')
#bin.actions['activate'].do()

# The following search is proving problematic - the time that it takes to 
# search for gdb in the /usr/bin directory via the GUI is so great that the
# script can time out - we can't just execute: gdb = fileTable.child('gdb')
#gdbFoundFlag = False
#while (not gdbFoundFlag):
#    try:
#        gdb = fileTable.child('gdb')
#        gdbFoundFlag = True
#    except:
#        print 'still looking for gdb'
#
#gdb.grabFocus()
#gdb.actions['activate'].do()
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
# page 3 - vbox50_tab3_termMonitor - Select process groups to monitor for termination
# this actually assigns a task termination observer to the process - open question as
# to whether this will remain unchanged in the GUI - as the user is also able to 
# assign a termination observer to the process groups on page 5 - for now, we'll
# assign a termination observer to every process group

vbox50_tab3_termMonitor = sessionDruid_sessionNoteBook.child('vbox50_tab3_termMonitor')
processTable = vbox50_tab3_termMonitor.child('sessionDruid_unexpectedExitTreeView', roleName = 'table')

theList = processTable.findChildren(predicate.GenericPredicate(roleName='table cell'), False)
theDictionary = createProcessDict (theList)

for x in userSelectedProcessGroups:
    tempProcess = theDictionary[x]
    tempProcess.actions['toggle'].do()

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

# ---------------------
# page 6 - tab6_sessionName - Select name for new session
vbox58_tab6_setSessionName = sessionDruid_sessionNoteBook.child('vbox58_tab6_setSessionName')

# Need to add a test here for the back button - both before and after the session
# name is selected - not sensitive yet - finishButton.click()
sessionName = vbox58_tab6_setSessionName.child('SessionDruid_sessionName')
sessionName.actions['activate'].do()
sessionName.__setattr__('text',theSession.getName())
finishButton.click()

