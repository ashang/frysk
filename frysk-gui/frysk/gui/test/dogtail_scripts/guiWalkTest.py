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

"""
Script name:    walkGuiTest.py
Creation date:  Sept 2006
Purpose:        Verify all elements in GUI work
Summary:        Simple, demo/prototype dogtail test script for Frysk

Goals:

The goal of this test is to 'walk' the Frysk GUIs and press every button, menu item, 
radio button, etc. The 'trick' is that in order for this test to not require a significant
amount of maintenance as the GUI evolves, we want this test to be able to discover the 
current state/design of the GUI at run-time and include as little hard-coded understanding 
of the GUI as possible. This is accomplished with the 'showChildren' function. Calling
this function results in a recursive loop that accesses/invokes every element in the
GUI.

Dynamic discovery .vs. hardcoding GUI understanding:

The one area in which the test script inlcudes some understanding of the GUI is in 
moving from one GUI window to another. (The 'showChildren' function intentionally
stays within one GUI window.) In the Frysk GUI, moving from one window to another is
always caused by pressing a 'push button' or 'menu item' GUI node. Accordingly, the
'showChildren' function always avoids pressing 'push button' GUI nodes and can 
optionally avoid pressing another other type of GUI node based on an input parameter.
At one point in the development of the script, we thought about having each GUI window
have an associated list of GUI nodes to not be invoked (a 'skip list'), but we dropped
this idea in favor of passing a GUI node type to the 'showChildren' function as the
skip list would (1) sometimes have to be dynamically altered and (2) sometimes have to
include a lengthy, hard-coded list of GUI nodes.

Validation:

This script is not a complete test for the Frysk GUIs as it does not replicate/simulate
users' actions in realistic use cases. It's really a test of verifying that all the GUI
elements 'are there.' The validations for this this script consist of try/exception blocks
around the accessing of each GUI node (thru calling node.blink() function) and the 
invocation of actions on each GUI node (thru calling the node.actions[x].do() function
on each GUI node.

Paths thru the GUIs:

There are an infinite number of paths thru the GUI - this script will cover these
(top-down paths):

          Starting point - open frysk startup manager
            path 1 open debug existing process window
                a open debug process list window
                b open frysk source window
                c     open preferences window
                d         open source window
                e           open pick a color window
                f         open syntax highlights window
                g     open register window
                h        open edit columns window
                i     open memory window
                j         open edit columns window
            path 2 open blank session with terminal window
                 open frysk monitor window
            path 3 open run/manage session window
                 open create a frysk debug session window
                 open frysk monitor window
                     open manage custom observers window
                         open observer details window
                     open program obserers window
                     open help window
                         open about window

The same numbering scheme is used in the comments in the code.

"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

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

class guiWalktest ( unittest.TestCase ):

    def setUp( self ):
        
        # Set up for logging
        self.TestString=dogtail.tc.TCString()
        self.theLogWriter = self.TestString.writer
        self.theLogWriter.writeResult( {'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' starting'  } )

        # Start up Frysk
        self.FryskBinary = os.getenv( 'fryskBinary' )
        self.funitChildBinary = os.getenv( 'funitChild' )

        self.startObject = startFrysk( self.FryskBinary, self.funitChildBinary, self.theLogWriter )
        self.frysk = self.startObject.getFryskObject()

        # Load up Session object
        self.parser = xml.sax.make_parser()
        self.handler = FryskHandler.FryskHandler()
        self.parser.setContentHandler( self.handler )

        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. 
        self.parser.parse( os.getenv( 'TestDruid_FILE' ) )
        self.theSession = self.handler.theDebugSession
        
    def tearDown( self ):    
        # Exit Frysk
        endFrysk ( self.startObject )
        self.theLogWriter.writeResult( {'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  } )
 
    def showChildren_new ( self, theNode, theType, theAction ):
        # Always skip the click action on push buttons, optionally
        # skip other GUI node types and actions

        if theNode.showing: 
            try:
                theNode.blink()
            except:
                self.fail( 'Blinking on: ' + theNode.name + ' failed' )
               
            theActions = theNode.actions
            for x in theActions:
                if not (((theNode.roleName == 'push button') and (x == 'click')) or \
                        ((theNode.roleName == theType) and (x == theAction))):
                        #print "Got one to not click!" + theNode.name
                #else:
                    try:
                        # Perform the action twice - resets radio buttons to their original state
                        theNode.actions[x].do()
                        theNode.actions[x].do()
                        print str(theNode.actions[x])
                    except:
                        self.fail( 'Clicking on: ' + str( theNode.actions[x] ) + ' in GUI: ' + theNode.name + ' failed' )
        
        # Recursively call the function to walk thru the GUI nodes
        theList = theNode.children
        for x in theList:
            self.showChildren_new ( x, theType, theAction )    
 
    def testPath_1( self ):      
        """Check that the GUI elements can be accessed and acted upon"""   
 
        # 1 open debug existing process window
        self.showChildren_new ( self.frysk, "push button", "click" )
        debugRadioButton = self.frysk.child ( roleName='radio button', name='Debug an Existing Process' )
        debugRadioButton.click()
        debugButton = self.frysk.child ( roleName='push button' )
        debugButton.click()

        # a open debug process list window
        processDialog = self.frysk.dialog( 'Debug Process List' )
        self.showChildren_new ( processDialog, "table cell", "activate" )
        theTable = processDialog.child ( roleName='tree table' ) 
        hello = theTable.child ( name='ahello' )
        hello.grabFocus()
        OKbutton = processDialog.child( name='Open', roleName='push button' )
        OKbutton.click()

        # b open frysk source window - need to wait for it to be displayed
        import time
        time.sleep ( 5 )
        theApp = tree.root.application( 'Frysk' )
        theList = theApp.children
        sourceDialog = theList[1]  
        
        # Open up the 'find' panel on the open frysk source window
        findPanel = sourceDialog.child( name='Find', roleName='check menu item' )
        findPanel.click()
        self.showChildren_new ( sourceDialog, "menu", "click" )
        
        # And close the 'find panel'
        gtkcancel = sourceDialog.child (name='gtk-cancel')
        gtkcancel.click()      
         
        # Access the preferences window thru the edit menu item 
        edit = sourceDialog.child( name='Edit', roleName='menu' )
        edit.click()

        # c open preferences window
        preferences = edit.menuItem( 'Frysk Preferences' )
        preferences.click()
        prefDialog = theApp.child( 'prefWin_preferencesWindow' )       
        prefTable = prefDialog.child( 'preferenceTree_listOfPreferenceGroups' )     
        sourceWindow = prefTable.child( 'Source Window' )   
        self.showChildren_new ( prefDialog, "push button", "click" )
         
        sourceWindow.actions['expand or contract'].do() 
        lookAndFeel = prefTable.child( 'Look and Feel' ) 
        lookAndFeel.actions['activate'].do()
        lookAndFeel.grabFocus()
        self.showChildren_new ( prefDialog, "push button", "click" )
        
        syntaxHighlighting = prefTable.child( 'Syntax Highlighting' ) 
        syntaxHighlighting.actions['activate'].do()
        syntaxHighlighting.grabFocus()
        self.showChildren_new ( prefDialog, "push button", "click" )
        
        # The same color dialog is used for all colors - only ues once
        colorButton = prefDialog.child( 'classColor_classSyntaxHighlightingColor' )
        colorButton.click()
        pickColor = theApp.child( 'Pick a Color' )
        closeButton = pickColor.button( 'OK' )
        cancelButton = pickColor.button( 'Cancel' )
        #skipList = ['OK', 'Cancel']
        self.showChildren_new ( pickColor, "push button", "click" )
        
        # close pick a color window
        closeButton = pickColor.button( 'OK' )
        closeButton.click()
        colorButton.click()
        pickColor = theApp.child( 'Pick a Color' )
        cancelButton.click()       
        
        # close preferences window
        cancelButton = prefDialog.button( 'Cancel' )
        cancelButton.click()

        # g open register window
        view = sourceDialog.child( name='View', roleName='menu' )
        view.click()                
        registerWindow = view.menuItem( 'Register Window' )
        registerWindow.click()
        
        theApp = tree.root.application( 'Frysk' )
        theList = theApp.children
        registerDialog = theList[2]  
        self.showChildren_new ( registerDialog, "push button", "click" )
        
        # h open edit columns window
        editColumns = registerDialog.button('Edit Columns...')
        editColumns.click()
        editColumnsDialog = theApp.dialog('Frysk / Register Formats')
        self.showChildren_new ( editColumnsDialog, "push button", "click" )
        closeButton = editColumnsDialog.button ('Close')
        closeButton.click()
        
        closeButton = registerDialog.button( 'Close' )
        closeButton.click()
        
        # i open memory window
        view = sourceDialog.child( name='View', roleName='menu' )
        view.click()                
        memoryWindow = view.menuItem( 'Memory Window' )
        memoryWindow.click()
        
        theApp = tree.root.application( 'Frysk' )
        theList = theApp.children
        memoryDialog = theList[2]  
        self.showChildren_new ( memoryDialog, "push button", "click" )
        
        # j open edit columns window
        editColumns = memoryDialog.button('Edit Columns...')
        editColumns.click()
        editColumnsDialog = theApp.dialog('Frysk / Memory Formats')
        self.showChildren_new ( editColumnsDialog, "push button", "click" )
        closeButton = editColumnsDialog.button ('Close')
        closeButton.click()
        
        closeButton = memoryDialog.button( 'Close' )
        closeButton.click()
        
        stack = sourceDialog.child( name='Stack', roleName='menu' )
        stack.click()                
        stackList = stack.children
        for x in stackList:
            x.click()
            
        program = sourceDialog.child( name='Program', roleName='menu' )
        program.click()                
        programList = program.children
        for x in programList:
            x.click()            
                
        #sourceDialog
        #self.frysk.dialog( 'Frysk Startup Manager' )

   
def suite():
    suite = unittest.TestSuite()
    suite.addTest( guiWalktest ( 'testPath_1' ) )
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=1 ).run( suite() )
