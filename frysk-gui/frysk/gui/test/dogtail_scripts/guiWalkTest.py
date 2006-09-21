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
                a open frysk monitor window
                b   open help window
                c      open about window

            path 3 open run/manage session window
                a open create a frysk debug session window
                b open frysk monitor window
                c     open manage custom observers window
                d         open observer details window
                e     open program obserers window
                f     open help window
                g         open about window

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
from DebugExistingProcessDialog import *
import time

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
   
   
    def testPath_1( self ):      
        """Check that the GUI elements can be accessed and acted upon"""   
        
        # Start at the top level Frysk gui
        topFryskDialog = AbstractGuiClass()
        topFryskDialog.setCurrentGui(self.frysk)
        showChildren_new ( topFryskDialog.getCurrentGui(), "table cell", "activate" )
                
        # 1 open debug existing process window
        theDebugProcessDialog = DebugExistingProcessDialog(topFryskDialog.getCurrentGui())
        showChildren_new ( theDebugProcessDialog.getCurrentGui(), "table cell", "activate" )
        processDialog = theDebugProcessDialog.getCurrentGui()
        
        ##################################continue work here ########################
        
        
        
        theTable = processDialog.child ( roleName='tree table' ) 
        hello = theTable.child ( name='ahello' )
        hello.grabFocus()
        OKbutton = processDialog.child( name='Open', roleName='push button' )
        OKbutton.click()

        # b open frysk source window - need to wait for it to be displayed
        time.sleep ( 5 )
        theApp = tree.root.application( 'Frysk' )
        theList = theApp.children
        sourceDialog = theList[1]  
        
        # Open up the 'find' panel on the open frysk source window
        findPanel = sourceDialog.child( name='Find', roleName='check menu item' )
        findPanel.click()
        showChildren_new ( sourceDialog, "menu", "click" )
        
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
        showChildren_new ( prefDialog, "push button", "click" )
         
        sourceWindow.actions['expand or contract'].do() 
        lookAndFeel = prefTable.child( 'Look and Feel' ) 
        lookAndFeel.actions['activate'].do()
        lookAndFeel.grabFocus()
        showChildren_new ( prefDialog, "push button", "click" )
        
        syntaxHighlighting = prefTable.child( 'Syntax Highlighting' ) 
        syntaxHighlighting.actions['activate'].do()
        syntaxHighlighting.grabFocus()
        showChildren_new ( prefDialog, "push button", "click" )
        
        # The same color dialog is used for all colors - only ues once
        colorButton = prefDialog.child( 'classColor_classSyntaxHighlightingColor' )
        colorButton.click()
        pickColor = theApp.child( 'Pick a Color' )
        closeButton = pickColor.button( 'OK' )
        cancelButton = pickColor.button( 'Cancel' )
        showChildren_new ( pickColor, "push button", "click" )
        
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
        showChildren_new ( registerDialog, "push button", "click" )
        
        # h open edit columns window
        editColumns = registerDialog.button('Edit Columns...')
        editColumns.click()
        editColumnsDialog = theApp.dialog('Frysk / Register Formats')
        showChildren_new ( editColumnsDialog, "push button", "click" )
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
        for x in theList:
            print str(x)
        memoryDialog = theList[2]  
        showChildren_new ( memoryDialog, "push button", "click" )
        
        # j open edit columns window
        editColumns = memoryDialog.button('Edit Columns...')
        editColumns.click()
        editColumnsDialog = theApp.dialog('Frysk / Memory Formats')
        showChildren_new ( editColumnsDialog, "push button", "click" )
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
        
        programClose = sourceDialog.child( name='File', roleName='menu' )
        programClose.click()
        closeItem = programClose.child (name='Close', roleName = 'menu item')
        closeItem.click()



    #def testPath_2( self ):
        """Check that the GUI elements can be accessed and acted upon"""

        # Start at the top level Frysk gui
        topFryskDialog = AbstractGuiClass()
        topFryskDialog.setCurrentGui(self.frysk)
        showChildren_new ( topFryskDialog.getCurrentGui(), "table cell", "activate" )

        # path 2 open blank session with terminal window
        # a. open frysk monitor window
        fryskStartupManager = self.frysk.child ( 'Frysk Startup Manager' )

        terminalRadioButton = fryskStartupManager.child ( roleName='radio button', name='Open Blank Session with a Terminal' )
        terminalRadioButton.click()
        openButton = fryskStartupManager.child (roleName='push button', name = 'Open')
        openButton.click()

        # Need to handle the delay in getting the Frysk Monitor frame to appear
        time.sleep ( 5 )

        fryskMonitor = self.frysk.child ('Frysk Monitor')
        showChildren_new ( fryskMonitor, "menu item", "click" )

        # Select the 'Help' menu item
        helpItem = fryskMonitor.menuItem('Help')
        helpItem.click()

        # Select the 'About Frysk' Help menu item
        aboutItem = helpItem.menuItem('About')
        aboutItem.click() 
        aboutFrame = self.frysk.child(ABOUT_FRYSK)
        showChildren_new ( aboutFrame, "push button", "click" )

        # Select the 'License' menu pick and click the button to open the license frame
        licenseButton = aboutFrame.button(LICENSE)
        licenseButton.click()
        licenseFrame = self.frysk.dialog(LICENSE)
        showChildren_new ( licenseFrame, "push button", "click" )

        # Close the license text frame
        closeButton = licenseFrame.button('Close')
        closeButton.click()

        # Select the 'Credits' menu pick and click the button to open the credits frame
        creditsButton = aboutFrame.button(CREDITS)
        creditsButton.click()
        creditsFrame = self.frysk.dialog(CREDITS)
        showChildren_new ( creditsFrame, "push button", "click" )

        # Close the license text frame
        closeButton = creditsFrame.button('Close')
        closeButton.click()

        #self.frysk.dump()

   
def suite():
    suite = unittest.TestSuite()
    suite.addTest( guiWalktest ( 'testPath_1' ) )
#    suite.addTest( guiWalktest ( 'testPath_2' ) )
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=1 ).run( suite() )


