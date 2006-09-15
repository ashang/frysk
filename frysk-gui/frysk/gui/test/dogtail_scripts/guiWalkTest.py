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

        # Create a Frysk session - True = quit the FryskGui after
        # creating the session
        # createMinimalSession (self.frysk, self.theSession, False)
        
    def tearDown( self ):    
        # Exit Frysk
        endFrysk ( self.startObject )
        self.theLogWriter.writeResult( {'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  } )
        
              ###############################################
    # Don't perform the 'click' action on nodes in the skipList
    def showChildren ( self, theNode, skipList ):
        if theNode.showing: 
            theNode.blink()
            theActions = theNode.actions
            for x in theActions:
                if ( skipList[0] != 'all' ):
                    if ( ( theNode.name in skipList ) and ( x == 'click' ) ):
                        print "Got one to not click!" + theNode.name
                    else:
                        try:
                            theNode.actions[x].do()
                            print str(theNode.actions[x])
                        except:
                            self.fail( 'Clicking on: ' + str( theNode.actions[x] ) + ' in GUI: ' + theNode.name + ' failed' )
        # Recursively call the function to walk thru the GUI nodes
        theList = theNode.children
        for x in theList:
            self.showChildren( x, skipList )    
 
    # Don't perform the 'click' action on nodes in the skipList
    def showChildren_new ( self, theNode, theType, theAction ):
        if theNode.showing: 
            theNode.blink()             
            theActions = theNode.actions
            for x in theActions:
                if ((theNode.roleName == 'push button') and (x == 'click'))  or ((theNode.roleName == theType) and (x == theAction)):
                        print "Got one to not click!" + theNode.name
                else:
                    try:
                        theNode.actions[x].do()
                        theNode.actions[x].do()
                        print str(theNode.actions[x])
                    except:
                        self.fail( 'Clicking on: ' + str( theNode.actions[x] ) + ' in GUI: ' + theNode.name + ' failed' )
        # Recursively call the function to walk thru the GUI nodes
        theList = theNode.children
        for x in theList:
            self.showChildren_new ( x, theType, theAction )    
 
    def testall( self ):      
        """Check that the GUI elements can be accessed and acted upon"""   
                
        #######################################################
        # open frysk startup manager
        #   1 open debug existing process window
        #       a open debug process list window
        #       b open frysk source window
        #       c     open preferences window
        #       d         open source window
        #       e           open pick a color window
        #       f         open syntax highlights window
        #       g     open register window
        #       h        open edit columns window
        #       i     open memory window
        #       j         open edit columns window
        #   2 open blank session with terminal window
        #        open frysk monitor window
        #   3 open run/manage session window
        #        open create a frysk debug session window
        #        open frysk monitor window
        #            open manage custom observers window
        #                open observer details window
        #            open program obserers window
        #            open help window
        #                open about window

       

        # 1 open debug existing process window
        skipList = ['all']
        self.showChildren(self.frysk, skipList)
        debugRadioButton = self.frysk.child ( roleName='radio button', name='Debug an Existing Process' )
        debugRadioButton.click()
        debugButton = self.frysk.child ( roleName='push button' )
        debugButton.click()

        # a open debug process list window
        skipList = ['all']
        processDialog = self.frysk.dialog( 'Debug Process List' )
        self.showChildren(processDialog, skipList)
        #self.showChildren_new ( processDialog, "table cell", "activate" )
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
        sourceDialog = theList[1]   # App.dialog('Frysk Source Window for: ahello Task 18952')
        skipList = ['all']
        self.showChildren(sourceDialog, skipList)
        #self.showChildren_new ( sourceDialog, "push button", "click" )
        edit = sourceDialog.child( name='Edit', roleName='menu' )
        edit.click()

        # c open preferences window
        preferences = edit.menuItem( 'Frysk Preferences' )
        preferences.click()
        prefDialog = theApp.child( 'prefWin_preferencesWindow' )  #'Preferences') #prefWin_preferencesWindow

        # Skip any GUI nodes that cause another GUI window to be opened
        skipList = ['linenumColor_colorOfLineNumbers', 
                    'execColor_executableMarkColor', 
                    'classColor_classSyntaxHighlightingColor', 
                    'funcColor_FunctionSyntaxHighlightingColor', 
                    'keyColor_syntaxKeywordColor', 
                    'localColor_localVariableSytnaxHighlightingColor', 
                    'globalColor_globalVariabelsSyntaxHighlightingColor', 
                    'oosColor_OutOfScopeVariablesSyntaxHighlightingColor', 
                    'optColor_optimizedVariablesSyntaxHighlightingColor', 
                    'commentColor_commentSyntaxHighlightingColor', 
                    'namespaceColor_namespaceNameSyntaxHighlightingColor', 
                    'includeColor_preprocessorIncludeSyntaxHighlightingColor', 
                    'macorColor_preprocessorMacroSyntaxHighlightingColor', 
                    'templateColor_templateSyntaxHighlightingColor', 
                    'textColor_textForegroundColor', 
                    'backgroundColor_textBackgroundColor', 
                    'marginColor_sideMarginColor', 
                    'searchColor_searchResultsColor', 
                    'currentlineColor_currentlineColor', 
                    'Cancel', 
                    'Apply' ]       
        
        prefTable = prefDialog.child( 'preferenceTree_listOfPreferenceGroups' )     
        sourceWindow = prefTable.child( 'Source Window' )   
        self.showChildren( prefDialog, skipList )
        #self.showChildren_new ( prefDialog, "push button", "click" )
         
        #sourceWindow.actions['expand or contract'].do() 
        lookAndFeel = prefTable.child( 'Look and Feel' ) 
        lookAndFeel.actions['activate'].do()
        lookAndFeel.grabFocus()
        self.showChildren( prefDialog, skipList )
        #self.showChildren_new ( prefDialog, "push button", "click" )
        
        syntaxHighlighting = prefTable.child( 'Syntax Highlighting' ) 
        syntaxHighlighting.actions['activate'].do()
        syntaxHighlighting.grabFocus()
        self.showChildren( prefDialog, skipList )
        #self.showChildren_new ( prefDialog, "push button", "click" )
        
        # The same color dialog is used for all colors - only ues once
        colorButton = prefDialog.child( 'classColor_classSyntaxHighlightingColor' )
        colorButton.click()
        pickColor = theApp.child( 'Pick a Color' )
        closeButton = pickColor.button( 'OK' )
        cancelButton = pickColor.button( 'Cancel' )
        skipList = ['OK', 'Cancel']
        #self.showChildren( pickColor, skipList )
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

        #       g     open register window
        #       h        open edit columns window
        #       i     open memory window
        #       j         open edit columns window

       


def suite():
    suite = unittest.TestSuite()
    suite.addTest( guiWalktest ( 'testall' ) )
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=2 ).run( suite() )
