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
Script name:    test3380.py
Creation date:  Oct 2006
Purpose:        Test to recreate this bug: http://sourceware.org/bugzilla/show_bug.cgi?id=3380
Summary:        Once the bug is fixed - this test should run cleanly - until then it will fail
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

from fryskTestCase import fryskTestCase

class test3380 ( fryskTestCase ):

    def setUp( self ):
        
        fryskTestCase.setUp(self);
        
        # Load up Session object
        self.parser = xml.sax.make_parser()
        self.handler = FryskHandler.FryskHandler()
        self.parser.setContentHandler( self.handler )

        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. 
        self.parser.parse( os.getenv( 'TestDruid_FILE' ) )
        self.theSession = self.handler.theDebugSession
               
    
    def runTheTest ( self, thePID ):        
        
        # Start at the top level Frysk gui
        topFryskDialog = AbstractGuiClass()
        topFryskDialog.setCurrentGui(self.frysk)
                
        # open debug existing process window
        theDebugProcessDialog = DebugExistingProcessDialog(topFryskDialog.getCurrentGui())
        processDialog = theDebugProcessDialog.getCurrentGui()
        
        #######################continue work here on generalizing code to walk GUI ############
             
        # TODO - replace this with a program built with debug enabled so we can see the source     
        theTable = processDialog.child ( roleName='tree table' ) 
        theList = theTable.children
        targetPID = theList[thePID]
        targetProcessName = theList[thePID + 1]
        targetProcessLocation = theList[thePID + 2]
        targetPID.grabFocus()
        
        OKbutton = processDialog.child( name='Open', roleName='push button' )
        OKbutton.click()

        # b open frysk source window - need to wait for it to be displayed
        time.sleep ( 15 )
        sourceDialogName = 'Frysk Source Window for: ' + targetProcessName.name[0:15] + ' - process ' + targetPID.name
        sourceDialog = self.frysk.child (sourceDialogName)        
        
        programClose = sourceDialog.child( name='File', roleName='menu' )
        programClose.click()
        closeItem = programClose.child (name='Close', roleName = 'menu item')
        closeItem.click()
   
    def testFailure_1( self ):      
        """test = test3380.testFailure_1 - This test fails with this exception: 
        java.lang.Exception: gtk_widget_show_all: assertion `GTK_IS_WIDGET (widget)' failed
        at org.gnu.glib.GObject.printStackTrace(libgtkjava-2.8.so)
        at org.gnu.gtk.Widget.gtk_widget_show_all(libgtkjava-2.8.so)
        at org.gnu.gtk.Widget.showAll(libgtkjava-2.8.so)
        at frysk.gui.srcwin.SourceWindowFactory.createSourceWindow(FryskGui)
        at frysk.gui.SessionManagerGui$2.buttonEvent(FryskGui)
        at org.gnu.gtk.Button.fireButtonEvent(libgtkjava-2.8.so)
        at org.gnu.gtk.Button.handleClick(libgtkjava-2.8.so)
        at org.gnu.gtk.Gtk.gtk_main(libgtkjava-2.8.so)
        at org.gnu.gtk.Gtk.main(libgtkjava-2.8.so)
        at frysk.gui.Gui.gui(FryskGui)
        at frysk.gui.FryskGui.main(FryskGui) """ 
        
        # Why #12? To avoid process at-spi-registryd - trying to debug this process hangs the GNOME desktop
        self.runTheTest (12)
        self.runTheTest (12)
    
    def testFailure_2( self ):      
        """test = test3380.testFailure_2 - This test fails with an infinite loop of this exceptions:
        org.gnu.gtk.ObjectDestroyedException
        at org.gnu.gtk.Window.checkState(libgtkjava-2.8.so)
        at org.gnu.gtk.Widget.setSensitive(libgtkjava-2.8.so)
        at frysk.gui.srcwin.SourceWindow.procReblocked(FryskGui)
        at frysk.gui.srcwin.SourceWindowFactory.handleTask(FryskGui)
        at frysk.gui.srcwin.SourceWindowFactory$SourceWinBlocker$1.run(FryskGui)
        at org.gnu.glib.CustomEvents.runEvents(libgtkjava-2.8.so)
        at org.gnu.gtk.Gtk.gtk_main(libgtkjava-2.8.so)
        at org.gnu.gtk.Gtk.main(libgtkjava-2.8.so)
        at frysk.gui.Gui.gui(FryskGui)
        at frysk.gui.FryskGui.main(FryskGui)"""   
        
         # Why #24? To avoid process bash - which is sometimes listed as /bin/sh - the test
         # script needs only one name for a process 
        self.runTheTest (24)
        self.runTheTest (27)
        
def suite():
    suite = unittest.TestSuite()
    suite.addTest( test3380 ( 'testFailure_1' ) )
    suite.addTest( test3380 ( 'testFailure_2' ) )
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=2 ).run( suite() )
