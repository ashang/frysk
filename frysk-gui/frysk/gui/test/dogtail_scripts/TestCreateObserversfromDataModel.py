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
Script name:    TestCreateObservers.py
Creation date:  April 2006
Purpose:        Verify creation/reading/updating/deleting (CRUD) of Frysk Observer objects
Summary:        Simple, demo/prototype dogtail test script for Frysk
                At this point - GUI elements are 'blinked' multiple times for demo purposes
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# Imports
from dogtail import tree
from dogtail import predicate

# Set up for logging
import dogtail.tc

# Set up for unit test framework
import unittest

# Test support functions
from FryskHelpers import startFrysk
from FryskHelpers import endFrysk
from FryskHelpers import skipDruid
from FryskHelpers import getEventType

# Setup to parse test input data (XML file)
import xml.sax
import ObserverHandler
import sys
import os

# The Frysk test objects
from Observer import Observer
from ObserverElement import ObserverElement
from ObserverPoints import ObserverPoints

class TestCreateObserversfromDataModel ( unittest.TestCase ):

    def setUp( self ):

        # Set up for logging
        self.TestString=dogtail.tc.TCString()
        # Start up Frysk 
        self.frysk = startFrysk()

        # Probably temporary - during test development
        #skipDruid(self.frysk)

        # Load up some sample Observer objects         
        parser = xml.sax.make_parser(  )
        handler = ObserverHandler.ObserverHandler(  )
        parser.setContentHandler(handler)    
              
        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. As of June 8, 2006, there's a problem with 
        # the test suite - either Frysk or Dogtail gets confused and attempts
        # to run tests before other tests have completed - short-term workaround
        # is to comment out these lines, run the tests separately, and read
        # the datafiles from the CLI       
        parser.parse(sys.argv[1])
        #inputFile = os.environ.get('TestCreateObserversfromDataModel_FILE')
        #parser.parse(inputFile)

        theObserver = handler.theObserver
        theObserver.dump()
        theName = theObserver.getName()
        theType = theObserver.getType()

        theActions = theObserver.getActionPoints()
        for tempAction in theActions:
          theActionName = tempAction.getName()
  
        x = Observer()
        x.setName ( theName )
        x.setLoggingAction ( theActionName )
        x.setType (theType)
        
        # Create a List object to hold the Observer objects
        self.theMatrix = [x]
        self.matrixLength = len( self.theMatrix )

        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( 'Observers' )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( 'Custom Observers... (DEMO)' )
        observersSelection.click()

        # Create a new custom observer
        customObservers = self.frysk.dialog( 'Custom Observers' )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        # Until we can get the accessibility problem
        # (http://sourceware.org/bugzilla/show_bug.cgi?id=2614) resolved,
        # we'll just create a new Exec Observers
        execObserver = customObservers.child( name = 'Exec Observer', roleName='table cell' )
        execObserver.actions['activate'].do()

        # Start loop to create the new Observers

        for i in range( self.matrixLength ):
  
            observerToCreate = self.theMatrix[i]
            #observerToCreate.dump()
  
            # Press 'New'
            newObserverButton = customObservers.button( 'New' )
            newObserverButton.click()
            observerDetails = self.frysk.dialog( 'Observer Details' )
  
            # Find the panel on the frame
            observerPanel = observerDetails.child( roleName='panel' )
            combo =  observerPanel.child( roleName='combo box' )
            comboMenu = combo.child( roleName='menu' )

            # Find and set the logging action combo box - again - this is the only combo
            # box that we can access until Frysk displays more accessibility infomation
            newLoggingAction = observerToCreate.getLoggingAction()
            selectedItem=comboMenu.child( name=newLoggingAction )
            selectedItem.click()
            
            tempString = getEventType (observerToCreate.getType())           
              
            try:
                # Set the new observer name
                newObserverName = observerToCreate.getName()
                observerName = observerPanel.child( roleName='text', name = 'observerNameEntry')   #description='Enter a name for the observer' )
                observerName.actions['activate'].do()
                observerName.text = newObserverName
                
                observerTypeComboBox = observerPanel.child( roleName='combo box', name = 'observerTypeComboBox') 
                comboMenu = observerTypeComboBox.child( roleName='menu' )    
                tempString = getEventType (observerToCreate.getType())
                selectedItem=comboMenu.child( name = tempString )
                selectedItem.click()                      
                okButton = observerDetails.button( 'OK' )
                okButton.click()
            except Error:
                self.fail ( 'Error - unable to create new Observer with name = ' + newObserverName )
            else:
                pass
                print 'Successfully created new Observer with name = ' + newObserverName
  
        # end loop ---------

        # Return to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()

    def tearDown( self ):    
 
        # Add test to delete the observers
        
        """Check that the newly created Observers can be queried and deleted"""   
    
        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( 'Observers' )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( 'Custom Observers... (DEMO)' )
        observersSelection.click()

        customObservers = self.frysk.dialog( 'Custom Observers' )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        for i in range( self.matrixLength ):
            observerToVerify = self.theMatrix[i]
            observerNameToVerify = observerToVerify.getName()
  
            try:
                observerInGui = customTable.child( name = observerNameToVerify, roleName='table cell' )
                observerInGui.actions['activate'].do()
                observerInGui.grabFocus()
                deleteButton = customObservers.button( 'Delete' )
                deleteButton.click()
            except dogtail.tree.SearchError:
                self.fail ( 'Error - unable to locate Observer with name = ' + observerNameToVerify )
            else:
                pass        
                print 'No error - successfully found ' + observerNameToVerify

        # Resturn to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()
       
        # Exit Frysk
        endFrysk( self.frysk )
 
    def testUpdateObservers( self ):      
        """Check that the newly created Observers can be queried and updated"""   
    
        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( 'Observers' )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( 'Custom Observers... (DEMO)' )
        observersSelection.click()

        customObservers = self.frysk.dialog( 'Custom Observers' )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        for i in range( self.matrixLength ):
            observerToVerify = self.theMatrix[i]
            observerNameToVerify = observerToVerify.getName()
  
            try:
                observerInGui = customTable.child( name = observerNameToVerify, roleName='table cell' )
                observerInGui.actions['activate'].do()
                observerInGui.grabFocus()
            except dogtail.tree.SearchError:
                self.fail ( 'Error - unable to locate Observer with name = ' + observerNameToVerify )
            else:
                pass        
                print 'No error - successfully found ' + observerNameToVerify
                
            editButton = customObservers.button( 'Edit' )
            editButton.click()
            
            observerDetails = self.frysk.dialog( 'Observer Details' )
            observerName = observerDetails.child( name = 'observerNameEntry', roleName = 'text' )
                       
            observerToVerify.setName( observerToVerify.getName() + ' updated' )
            self.theMatrix[i] = observerToVerify
            
            observerName.text = observerToVerify.getName()
            okButton = observerDetails.button( 'OK' )
            okButton.click()
 
        # Resturn to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()

def suite():
        suite = unittest.TestSuite()
        suite.addTest( unittest.makeSuite( TestCreateObserversfromDataModel ) )
        return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner( verbosity=2 ).run( suite() )
