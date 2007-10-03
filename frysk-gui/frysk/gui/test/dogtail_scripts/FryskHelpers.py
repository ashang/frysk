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

# Other imports
import os
import time

# Constants
FRYSK_PROCESS_NAME = 'FryskGui'
FRYSK_BINARY_NAME = '/home/ldimaggi/sandbox/build/frysk-gui/frysk/gui/FryskGui'

# Frysk app name - note 'java-gnome' (sourceware.org/bugzilla #2591) 
FRYSK_APP_NAME = 'java-gnome'

# Used as a lookup table to match the key/type in XML files to value/GUI string
FRYSK_OBSERVER_TYPES = {'frysk.gui.monitor.observers.TaskForkedObserver':'Fork Observer', 
                        'frysk.gui.monitor.observers.TaskExecObserver':'Exec Observer', 
                        'frysk.gui.monitor.observers.TaskTerminatingObserver':'Task Terminating Observer', 
                        'frysk-gui.frysk.gui.monitor.observers.TaskSyscallObserver':'Syscall Observer', 
                        'frysk-gui.frysk.gui.monitor.observers.TaskCloneObserver':'TaskCloneObserver' }

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
def startFrysk ():
    """ Start up the Frysk GUI
        Function returns an object that points to the Frysk GUI
    """
    # Start up Frysk 
    run ( FRYSK_BINARY_NAME, appName=FRYSK_APP_NAME )
    fryskObject = tree.root.application ( FRYSK_APP_NAME )
    return fryskObject

# ---------------------
def endFrysk( fryskObject ):
    """ Close the Frysk GUI and kill the process
    """
    # Exit Frysk GUI
    closeItem = fryskObject.menuItem( 'Close' )
    closeItem.click()
    
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
    # ---------------------
    # Access the Druid GUI
    theDruid = fryskObject.dialog( 'Debug Session Druid' )

    # And the GUI's 'notebook' of (6) pages
    vbox1 = theDruid.child( 'dialog-vbox1' )
        
    # ---------------------
    # The action buttons are displayed on the bottom of all pages - the
    # specific buttons (Back, Forward, Finish) that are visible or enabled
    # varies with the page - and the current state of the page
    dialogActionArea1 = vbox1.child( 'dialog-action_area1' )
    finishButton = dialogActionArea1.button( 'Finish' )
    cancelButton = dialogActionArea1.button( 'Cancel' ) 
    cancelButton.click()
    finishButton.click()
        
# ---------------------
def getEventType ( eventClassName ):
    """ Based on the eventClass name, return the string used in the GUI
        for that event type
    """
    returnString = str( FRYSK_OBSERVER_TYPES.get( eventClassName ) )
    #print 'DEBUG - class=:' + eventClassName + ' name=' + returnString
    return returnString
