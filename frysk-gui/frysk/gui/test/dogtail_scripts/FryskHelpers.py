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
Script name:    FryskHelpers.py
Creation date:  April 2006
Purpose:        Frysk GUI Tests
Summary:        Support routines used by tests for Frysk SessionDebug GUI
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# To support starting Frysk tests
from dogtail import tree
from dogtail.utils import run
from dogtail import predicate

# Needed to remove Frysk from Gnome Panel
import subprocess

# Set up for logging
import dogtail.tc

# Other imports
import os
import commands
import sys
import time

# The Frysk test objects
from Observer import Observer
from DebugProcess import DebugProcess
from DebugSession import DebugSession

# Constants
FRYSK_PROCESS_NAME = 'FryskGui'
#FRYSK_BINARY_NAME = '/home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui'

# Frysk app name - note 'java-gnome' (sourceware.org/bugzilla #2591) 
FRYSK_APP_NAME = 'java-gnome'

# Used as a lookup table to match the key/type in XML files to value/GUI string
FRYSK_OBSERVER_TYPES = {'frysk.gui.monitor.observers.TaskForkedObserver':'Fork Observer', 
                        'frysk.gui.monitor.observers.TaskExecObserver':'Exec Observer', 
                        'frysk.gui.monitor.observers.TaskTerminatingObserver':'Task Terminating Observer', 
                        'frysk.gui.monitor.observers.TaskSyscallObserver':'Syscall Observer', 
                        'frysk.gui.monitor.observers.TaskCloneObserver':'TaskCloneObserver' }

# Location of Frysk persistent data files for Sessions
FRYSK_SESSION_FILES = os.environ['HOME'] + "/.frysk/Sessions/"
FRYSK_OBSERVER_FILES = os.environ['HOME'] + "/.frysk/Observers/"

# ---------------------
def extractString ( rawInput, assignedTo ):
    """ Function to extract value of string in param #1 assigned            
        to variable in param #2, enclosed in single quotes - for example:
        param #1 = "Node roleName='table cell' name='abc', description=''
        param #2 = "name"
        Function will return value of "abc"
    """
    assignedTo = str( assignedTo + "='" )
    startPos = int( rawInput.find( assignedTo, 0, len( rawInput ) ) + len( assignedTo ) )
    endPos = int( rawInput.find( "'", startPos, len( rawInput ) ) )
    finishedString = rawInput[startPos:endPos]
    #print "DEBUG - input = " + rawInput
    #print "DEBUG - output= " + finishedString
    return finishedString

# ---------------------
def removeAfterTab ( inputString ):
    """ Function to remove characters after a TAB ('\t') from a string
        This function is needed for the Frysk GUI automation tests as the 
        process names that inlcude a PID also include a TAB - for example:
        xterm\t2548, FryskGui\t2656
    """
    tabPos = inputString.find( '\t', 0, len( inputString ) )
    if ( tabPos > 0 ):
        returnString = inputString[:tabPos]
    else:
        returnString = inputString
    return returnString

# ---------------------
def findProcessNames ( searchForNames, listToSearch ):
    """ Function to examine the list of processes as viewed by Frysk inc. PIDs -
        to find the process names (w/o PIDs) as defined by user input to the test 
        script. This function returns a list of process names and PIDs for the
        list of process names that are supplied to the test script
        param #1 - the process name List (w/o PIDs) supplied to the test script
        param #2 - the process name List (inc PIDs) as generated by Frysk
        Function returns a list containing the process names supplied in param #1
        with PIDs as viewed by Frysk
    """
    returnList = []
    for searchName in searchForNames:
       for FryskProcessName in listToSearch:
           strFryskProcessName = str( FryskProcessName )
           targetName = extractString ( strFryskProcessName, 'name' )
           tempName = removeAfterTab( targetName )
           if ( searchName == tempName ):
               returnList.append( targetName )
    return returnList

# ---------------------
def createProcessDict ( inputList ):
    """ Need to deal with GUI inc. items generated at run time - these
        are table cells with the process names and toggle buttons - the
        problem is that the toggle buttons have no names - but, they are
        in sequence with the named process named - so, create two lists
        the keys = the process names and the values = the toggle boxes,
        then create a dictionary from the keys+values and toggle the
        selected boxes
        param #1 - the list returned by a call to:
           <someTable>.findChildren(predicate.GenericPredicate(roleName='table cell'), False)
        Function returns a Dictionary with keys = toggle names (strings) 
        and values = GUI toggle-able objects
    """
    thekeys = []
    thevalues = []
    toggleFlag=True

    # set the toggleFlag for odd numbered items (even = check boxes, odd = values)
    for i in range( len( inputList ) ):
        if ( toggleFlag == True ):
            toggleFlag = False
            temp = inputList[i]
            #print str(i) + "odd=" + str(inputList[i])
            thevalues.append( inputList[i] )
        elif ( toggleFlag == False ):
            #print str(i) + "even=" + str(inputList[i])
            unfinishedString = str( inputList[i] )
            finishedString = extractString ( unfinishedString, 'name' )
            thekeys.append( finishedString )
            toggleFlag = True
    theDictionary = dict( zip( thekeys, thevalues ) )
    return theDictionary

# ---------------------
def startFrysk ( FryskBinary, logWriter ):
    """ Start up the Frysk GUI
        Function returns an object that points to the Frysk GUI
    """
    
    # First, make sure that AT-SPI is enabled
    AT_SPI_output = commands.getoutput('gconftool-2 -g /desktop/gnome/interface/accessibility')
    if AT_SPI_output != 'true':
        print '\n***AT SPI is not enabled - exiting now***'
        print '***Verify with this command: gconftool-2 -g /desktop/gnome/interface/accessibility ***'
        logWriter.writeResult({'SEVERE' :  '***AT SPI is not enabled - exiting now***'  })
        logWriter.writeResult({'SEVERE' :  '***Verify with this command: gconftool-2 -g /desktop/gnome/interface/accessibility ***'  })       
        sys.exit(77)
   
    # Make sure that Frysk is not still running or in a funny state after previous tests
    # This is a brutal way to do this - is there a better way?
    try:
        killFrysk()
        logWriter.writeResult({'WARNING' :  'Frysk was still running at start of test- killed it - ok to start test'  })
    except:
        logWriter.writeResult({'INFO' :  'Frysk not running at start of test - ok to start test'  })
   
    # Start up Frysk 
    run ( FryskBinary, appName=FRYSK_APP_NAME )
    fryskObject = tree.root.application ( FRYSK_APP_NAME )
    return fryskObject

# ---------------------
def endFrysk( fryskObject ):
    """ Close the Frysk GUI 
    """
    # Exit Frysk GUI
    closeItem = fryskObject.menuItem( 'Close' )
    closeItem.click()
    killFrysk()
     
    # ---------------------
def killFrysk( ):
    """ Kill the process - cleanup the persistent object dir
    """
    # The Frysk object in the panel cannot be accessed as it does not have any
    # AT/SPI information - just kill the process instead
    subprocess.Popen( [r'killall', '-KILL', FRYSK_PROCESS_NAME] ).wait()
    
    # Cleanup all frysk config files created during the test - save them for test
    # failure analysis - need to add a means to tie test run info to dir name
    strTime = str( time.time() )
    newDir = '/tmp/' + strTime
    os.mkdir( newDir )
    oldDir = os.getenv( 'HOME' ) + '/.frysk'
    os.rename( oldDir, newDir )
      
 # ---------------------
def skipDruid( fryskObject ):
    """ Skip the intial session setup Druid - this function is probably 
        temporary and will be used only during test development
    """
    theDruid = fryskObject.dialog('Debug Session Druid')
    cancelButton = theDruid.button( 'Cancel' )
    cancelButton.click()
    finishButton = theDruid.button( 'Finish' )
    finishButton.click()

# ---------------------
def getEventType ( eventClassName ):
    """ Based on the eventClass name, return the string used in the GUI
        for that event type
    """
    returnString = str( FRYSK_OBSERVER_TYPES.get( eventClassName ) )
    #print 'DEBUG - class=:' + eventClassName + ' name=' + returnString
    return returnString

# ---------------------
def createMinimalSession (fryskObject, sessionObject, quitBoolean):
    """ This function is used to create a minimal session object - this
        is needed as all tests that access the FryskGui must either create
        a session or access an existing session.
    """

    theProcessGroups = sessionObject.getProcesses()
    userSelectedProcessGroups = []
    for x in theProcessGroups:
        userSelectedProcessGroups.append(x.getName())

    userSelectedObserverDict = {}
    for x in theProcessGroups:
        tempProcess = x.getName().encode('ascii')
        userSelectedObserverDict[x.getName()] = x.getObservers()
    
    # ---------------------
    # Access the Druid GUI
    theDruid = fryskObject.child(name='Frysk Startup Manager')
    newButton = theDruid.child (name='New', roleName = 'push button')
    newButton.click()
    
    theSessionManager = fryskObject.dialog('Create a Frysk Session Dialog')
    quitButton = theDruid.button('Quit')
    openButton = theDruid.button('Open')

    # And the GUI's 'notebook' of pages
    vbox1 = theSessionManager.child('dialog-vbox1')
    sessionDruid_sessionNoteBook = vbox1.child('sessionDruid_sessionNoteBook')
    vbox43_tab2_processGroups = sessionDruid_sessionNoteBook.child('vbox43_tab2_processGroups')
    sessionNameText = vbox43_tab2_processGroups.child(name='sessionNameText')
    sessionNameText.text = sessionObject.getName()
    
    hbox62_tab2_groupLists = vbox43_tab2_processGroups.child('hbox62_tab2_groupLists')
        
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

    # ---------------------
    # page #2 - vbox43_tab2_processGroups - Select process groups to monitor

    vbox43_tab2_processGroups = sessionDruid_sessionNoteBook.child('vbox43_tab2_processGroups')

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

    #hbox83_tab4_tagSets = sessionDruid_sessionNoteBook.child('hbox83_tab4_tagSets')
    #forwardButton.click()

    # ---------------------
    # page 5 - hbox77_tab5_observers - Select process groups and observers

    hbox77 = sessionDruid_sessionNoteBook.child('hbox77')
    vbox52 = hbox77.child('vbox52')
    SessionDruid_processObserverTreeView = vbox52.child('SessionDruid_processObserverTreeView')
    theProcessList = SessionDruid_processObserverTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)

    for processName in theProcessList:
        resolvedName = extractString (str(processName), 'name')
        tempProc = SessionDruid_processObserverTreeView.child(resolvedName)
        tempProc.grabFocus()
        tempProc.actions['activate'].do()

        vbox54 = hbox77.child('vbox54')
        SessionDruid_observerTreeView = vbox54.child('SessionDruid_observerTreeView')
        theList = SessionDruid_observerTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)
        theDictionary = createProcessDict (theList)
        userSelectedObservers = userSelectedObserverDict[resolvedName]
    
        for x in userSelectedObservers:
            tempObserver = theDictionary[x]
            tempObserver.actions['toggle'].do()

    forwardButton.click()

    # Close the Druid
    finishButton.click()

    # Either quit FryskGui, or close the SessionManager and go to the 
    # Monitor window
    if quitBoolean:
        quitButton.click()
    else:
        # Need to select just-created-session, then, select the 'Open' button
        theSessionTable = theDruid.child(name='SessionManager_previousSessionsListView')
        theNewlyCreatedSession = theSessionTable.child(name=sessionObject.getName(), roleName='table cell' )
        theNewlyCreatedSession.actions['activate'].do()
        theNewlyCreatedSession.grabFocus()
        openButton.click()
        
# ---------------------
def createBigSession (fryskObject, sessionObject, quitBoolean):
    """ This function is used to create a minimal session object - this
        is needed as all tests that access the FryskGui must either create
        a session or access an existing session.
    """

    # ---------------------
    # Access the Druid GUI
    theDruid = fryskObject.child(name='Frysk Startup Manager')
    newButton = theDruid.child (name='New', roleName = 'push button')
    newButton.click()
    
    theSessionManager = fryskObject.dialog('Create a Frysk Session Dialog')
    quitButton = theDruid.button('Quit')
    openButton = theDruid.button('Open')

    # And the GUI's 'notebook' of pages
    vbox1 = theSessionManager.child('dialog-vbox1')
    sessionDruid_sessionNoteBook = vbox1.child('sessionDruid_sessionNoteBook')
    vbox43_tab2_processGroups = sessionDruid_sessionNoteBook.child('vbox43_tab2_processGroups')
    sessionNameText = vbox43_tab2_processGroups.child(name='sessionNameText')
    sessionNameText.text = "all processes and observers"
    
    hbox62_tab2_groupLists = vbox43_tab2_processGroups.child('hbox62_tab2_groupLists')
        
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
    # page #2 - vbox43_tab2_processGroups - Select process groups to monitor

    vbox43_tab2_processGroups = sessionDruid_sessionNoteBook.child('vbox43_tab2_processGroups')

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

    import re
    
    processesToMonitor = []
    for FryskProcessName in theProcessList:
       strFryskProcessName = str( FryskProcessName )
       targetName = extractString ( strFryskProcessName, 'name' )
       tempName = removeAfterTab( targetName )
       # print "tempName = [[[" + tempName + "]]]"
      
       # Only names that start with a-zA-Z 
       if  re.match('[a-zA-Z]', tempName):
           # print "include = " + tempName + "  ---  " + targetName
           processesToMonitor.append( targetName )
       #else:
           # print "skip = " + tempName
    
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

    #import sys
    #sys.exit(1)

    import time
    time.sleep(30)

    hbox83_tab4_tagSets = sessionDruid_sessionNoteBook.child('hbox83_tab4_tagSets')
    
    import sys
    sys.exit(1)

    forwardButton.click()


    # ---------------------
    # page 5 - hbox77_tab5_observers - Select process groups and observers

    hbox77 = sessionDruid_sessionNoteBook.child('hbox77')
    vbox52 = hbox77.child('vbox52')
    SessionDruid_processObserverTreeView = vbox52.child('SessionDruid_processObserverTreeView')
    
    theProcessList = SessionDruid_processObserverTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)

    for processName in theProcessList:
        resolvedName = extractString (str(processName), 'name')
        #print "DEBUG - name = " + resolvedName
        tempProc = SessionDruid_processObserverTreeView.child(resolvedName)
        tempProc.grabFocus()
        tempProc.actions['activate'].do()

        vbox54 = hbox77.child('vbox54')
        SessionDruid_observerTreeView = vbox54.child('SessionDruid_observerTreeView')

        theList = SessionDruid_observerTreeView.findChildren(predicate.GenericPredicate(roleName='table cell'), False)

        # Just the odd numbered ones get toggled - the even numbered ones are
        # the process names
        oddFlag = False
        for x in theList:
           print x
           if oddFlag:
               #x.actions['toggle'].do()
               oddFlag = False
           else:
               oddFlag = True
               x.actions['toggle'].do()

    forwardButton.click()

    # Close the Druid
    finishButton.click()

    # Either quit FryskGui, or close the SessionManager and go to the 
    # Monitor window
    if quitBoolean:
        quitButton.click()
    else:
        # Need to select just-created-session, then, select the 'Open' button
        theSessionTable = theDruid.child(name='SessionManager_previousSessionsListView')
        theNewlyCreatedSession = theSessionTable.child(name=sessionObject.getName(), roleName='table cell' )
        theNewlyCreatedSession.actions['activate'].do()
        theNewlyCreatedSession.grabFocus()       
        openButton.click()

