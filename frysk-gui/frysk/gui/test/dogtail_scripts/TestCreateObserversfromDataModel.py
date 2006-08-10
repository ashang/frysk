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
from FryskHelpers import *

# Setup to parse test input data (XML file)
import xml.sax
import ObserverHandler
import FryskHandler
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
        self.theLogWriter = self.TestString.writer
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' starting'  })

        # Start up Frysk 
        self.FryskBinary = sys.argv[1]
        self.frysk = startFrysk(self.FryskBinary, self.theLogWriter)
                
         # Load up Session object
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

        # Create a Frysk session - True = quit the FryskGui after
        # creating the session
        createMinimalSession (self.frysk, self.theSession, False) 
        

        # Load up some sample Observer objects         
        self.parser = xml.sax.make_parser(  )
        self.handler = ObserverHandler.ObserverHandler(  )
        self.parser.setContentHandler(self.handler)    
              
        # Mechanism to allow multiple tests to be assembled into test suite,
        # and have the test input data files be specified in the suite defiition,
        # not the test script. As of June 8, 2006, there's a problem with 
        # the test suite - either Frysk or Dogtail gets confused and attempts
        # to run tests before other tests have completed - short-term workaround
        # is to comment out these lines, run the tests separately, and read
        # the datafiles from the CLI       
        self.parser.parse(sys.argv[3])
        #inputFile = os.environ.get('TestCreateObserversfromDataModel_FILE')
        #parser.parse(inputFile)

        self.theObserver = self.handler.theObserver
        #self.theObserver.dump()
        theName = self.theObserver.getName()
        theType = self.theObserver.getType()
        theDescription = self.theObserver.getDescription()

        # June 12 - Assume one ActionPoint with name == "Generic Names" - the elements
        # under this ActionPoint - "theActions" are where the actions are defined
        # This is very incomplete! The test script needs to be updated to match 
        # the new state of the code.
        topLevelActionPoints = self.theObserver.getActionPoints()
        genericAction = topLevelActionPoints[0]

        x = Observer()
        x.setName ( theName )
        # June 12 - setLoggingAction is a temporary method - there should be a list of actions
        #x.setLoggingAction ( theActionName )
        x.setType (theType)
        x.setDescription (theDescription)
                
        # Create a List object to hold the Observer objects - for now, the GUI persists only
        # one Observer per Observer file - but this may change, so we'll retain the matrix
        self.theMatrix = [x]
        self.matrixLength = len( self.theMatrix )

        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( OBSERVERS )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( MANAGE_CUSTOM_OBSERVERS )
        observersSelection.click()

        # Create a new custom observer
        customObservers = self.frysk.dialog( CUSTOM_OBSERVERS )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )
        temp = self.theMatrix[0]
 
        # Start loop to create the new Observers - for now, Frysk only supports (1)
        # observer object in a persistent file - so the loop counter has a limit of
        # (1)
        for i in range( self.matrixLength ):
  
            observerToCreate = self.theMatrix[i]
            #observerToCreate.dump()
  
            # Press 'New'
            newObserverButton = customObservers.button( 'New' )
            newObserverButton.click()
            observerDetails = self.frysk.dialog( CUSTOM_OBSERVER_DIALOG )
  
            # Find the panel on the frame
            observerPanel = observerDetails.child( name='table1', roleName='panel' )
            #combo =  observerPanel.child( roleName='combo box' )
            #comboMenu = combo.child( roleName='menu' )
               
            #try:
            # Set the new observer name
            newObserverName = observerToCreate.getName()
            observerName = observerPanel.child( roleName='text', name = 'observerNameEntry') 
            observerName.text = newObserverName
                
            # and description
            newObserverDescription = observerToCreate.getDescription() 
            observerDescription = observerPanel.child( roleName='text', name = 'observerDescriptionTextView') 
            observerDescription.text = newObserverDescription
                
            observerTypeComboBox = observerPanel.child( roleName='combo box', name = 'observerTypeComboBox') 
            comboMenu = observerTypeComboBox.child( roleName='menu' )    
            selectedItem=comboMenu.child( name = observerToCreate.getType() )
            selectedItem.click()              

            # At this point, we want to access the multiple actions (ActionPoints)
            # defined in the Observer. The GUI elements to support the definition of
            # more than one ActionPoint are generated at runtime, and do not have
            # AT data names, however, so we cannot access them by name. We have to 
            # deal with their order in the list[] of objects seen by Dogtail.
            #
            # In the code below, we always want the first comboBox and the 2nd button
            # (this button is the '+' button) under the 'observerActionsTable' GUI
            # element. This will be a real problem if the order of the elements
            # as generated/displayed changes.

            #theActions = genericAction.getElements()
            theActions = self.theObserver.getActionPoints()
            #actionListLength = len(theActions)
            #print "DEBUG - the actions length = " + str(actionListLength)
            #actionListCounter = 0
            for tempAction in theActions:
                #actionListCounter = actionListCounter + 1 
                
                theElements = tempAction.getElements()
                actionListLength = len(theElements)
                #print "DEBUG - the actions length = " + str(actionListLength)
                actionListCounter = 0
                for tempElement in theElements:                 
                    actionListCounter = actionListCounter + 1 

                    theActionName = tempElement.getName()
                    observerActionsTable = observerPanel.child (name = 'observerActionsTable')
                    # comboBox that lists the action types - the first one in the list[]
                    theComboBoxes = observerActionsTable.findChildren(predicate.GenericPredicate(roleName='combo box'), False)
                    comboBox = theComboBoxes[0]
                    # Select the action
                    actionItem = comboBox.menuItem (theActionName)
                    actionItem.click()
                    
                    actionText = observerActionsTable.child(roleName = 'text')
                    actionText.text = tempElement.getArgument()

                    #print "Press the + button"
                    # The '+' and '-' buttons to enable adding/substracting action points
                    theButtons = observerActionsTable.findChildren(predicate.GenericPredicate(roleName='push button'), False)
                    # button [1] is the '+' button
                    if actionListCounter < actionListLength:
                        theButtons[1].click()
                        #print "pressed the button"

            # And - there is a similar situation for the multiple FilterPoints defined
            # in the Observer

            theFilterPoints = self.theObserver.getFilterPoints()
            filterPointListLength = len(theFilterPoints)
            #print "DEBUG - filterPoint  len = " + str(filterPointListLength)
            filterPointListCounter = 0
 
            for tempFilterPoint in theFilterPoints:
                filterPointListCounter = filterPointListCounter + 1 
                theElements = tempFilterPoint.getElements()
                elementListLength = len(theElements)
                #print "DEBUG - the element length = " + str(elementListLength)
                elementListCounter = 0
                for tempElement in theElements:                 
                    elementListCounter = elementListCounter + 1 

                    theFilterName = tempElement.getName() + ' ' + tempFilterPoint.getName()
                    theFilterArgument = tempElement.getArgument()
                   
                    observerFiltersTable = observerPanel.child (name = 'observerFiltersTable')
                    # comboBox that lists the filter types - the first 2nd in the list[]
                    theComboBoxes = observerFiltersTable.findChildren(predicate.GenericPredicate(roleName='combo box'), False)
                    comboBox = theComboBoxes[1]
                    # Select the action
                    filterItem = comboBox.menuItem (theFilterName)
                    filterItem.click()
                    
                    filterText = observerFiltersTable.child(roleName = 'text')
                    filterText.text = tempElement.getArgument()

                    #print "Press the + button"
                    # The '+' and '-' buttons to enable adding/substracting action points
                    theButtons = observerFiltersTable.findChildren(predicate.GenericPredicate(roleName='push button'), False)
                    # button [1] is the '+' button
                    if elementListCounter < elementListLength:
                        theButtons[1].click()
                        #print "pressed the button"
                
                if filterPointListCounter < filterPointListLength:
                    theButtons[1].click()      
                

            try:
     
               # Save the new Observer
                okButton = observerDetails.button( 'OK' )
                okButton.click()

                # This button showed up on 20060609
                applyButton = customObservers.button ('Apply')
                applyButton.click()


            except:
                self.fail ( 'Error - unable to create new Observer with name = ' + newObserverName )
            else:
                #print 'Successfully created new Observer with name = ' + newObserverName
                self.theLogWriter.writeResult({'INFO' :  'Successfully created new Observer with name = ' + newObserverName})
                self.TestString.compare(self.theLogWriter.scriptName + '.setUp()', newObserverName, newObserverName)
                self.assertEqual(newObserverName, newObserverName)
  
        # end loop --------
        
        """Verify that the session object just created and presisted under $HOME/.frysk/Observers
           matches the test input"""   
  
        # ---------------------
        # Verify that the observer object just created and presisted under $HOME/.frysk/Observers
        # matches the test input - it's not ideal to put this into the setup method - but it has to
        # be run before the update test 
        newlyCreatedObserverFile =  FRYSK_OBSERVER_FILES + self.theObserver.getName()
        self.parser.parse(newlyCreatedObserverFile)
        newlyCreatedObserver = self.handler.theObserver

        #print '---------newlyCreatedObserver.dump()-------'
        #newlyCreatedObserver.dump()
        #print '---------self.theObserver.dump()--------'
        #self.theObserver.dump()
        
        newlyCreatedFilterPoints = newlyCreatedObserver.getFilterPoints()
        newlyCreatedObserver.setFilterPointsDict(newlyCreatedFilterPoints)
        self.theObserverFilterPoints = self.theObserver.getFilterPoints()
        self.theObserver.setFilterPointsDict(self.theObserverFilterPoints)

        newlyCreatedActionPoints = newlyCreatedObserver.getActionPoints()
        newlyCreatedObserver.setActionPointsDict(newlyCreatedActionPoints)
        self.theObserverActionPoints = self.theObserver.getActionPoints()
        self.theObserver.setActionPointsDict(self.theObserverActionPoints)

        if self.theObserver.isequal (newlyCreatedObserver):
            # print 'PASS - the observer objects match'
            self.TestString.compare(self.theLogWriter.scriptName + '.setUp()', newlyCreatedObserver.getName(), self.theObserver.getName() )
            self.assertEqual(newlyCreatedObserver.getName(), self.theObserver.getName() )
        else:
            self.fail ('FAIL - the observer objects do not match: ' + newlyCreatedObserver.getName() + ', ' + self.theObserver.getName() )

        # Return to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()

        ### Need to add new test - compare Observer in file to Observer as displayed in GUI ###
        # Populate new Observer Object from data displayed in GUI

        observerFromGUI = Observer()
        
        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( OBSERVERS )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( MANAGE_CUSTOM_OBSERVERS )
        observersSelection.click()

        customObservers = self.frysk.dialog( CUSTOM_OBSERVERS )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        for i in range( self.matrixLength ):
            observerToVerify = self.theMatrix[i]
            observerNameToVerify = observerToVerify.getName()
  
            try:
                observerInGui = customTable.child( name = observerNameToVerify, roleName='table cell' )
                # Correct, but not optimal: observerInGui.actions['activate'].do()
                observerInGui.doAction('activate')
                observerInGui.grabFocus()
            except dogtail.tree.SearchError:
                self.fail ( 'Error - unable to locate Observer with name = ' + observerNameToVerify )
            else:
                self.TestString.compare(self.theLogWriter.scriptName + '.testUpdateObservers()', observerNameToVerify, observerNameToVerify)
                self.assertEqual(observerNameToVerify, observerNameToVerify)                
                
            editButton = customObservers.button( 'Edit' )
            editButton.click()
            
            observerDetails = self.frysk.dialog( CUSTOM_OBSERVER_DIALOG )
            observerName = observerDetails.child( name = 'observerNameEntry', roleName = 'text' )

            # Set observer name
            observerFromGUI.setName(observerName.text)
            
            # and description
            observerPanel = observerDetails.child( name='table1', roleName='panel' )
            observerDescription = observerPanel.child( roleName='text', name = 'observerDescriptionTextView') 
            observerFromGUI.setDescription(observerDescription.text)
               
            # and type? This is blocked by http://bugzilla.gnome.org/show_bug.cgi?id=345667           
            #observerTypeComboBox = observerPanel.child( roleName='combo box', name = 'observerTypeComboBox') 
            #comboMenu = observerTypeComboBox.child( roleName='menu' ) 
            #comboMenu.isSelected()
            
            #theTypes = comboMenu.findChildren(predicate.GenericPredicate(roleName='menu item'), False)
            #for x in theTypes:
            #    x.dump()
            #    print x.isSelected()
            
            observerFromGUI.setType(self.theObserver.getType())
            
            # and the action Points
                
            # and the filter Points            

            okButton = observerDetails.button( 'OK' )
            okButton.click()
 
        # Resturn to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()  

    def tearDown( self ):    
 
        # Add test to delete the observers
        
        """Check that the newly created Observers can be queried and deleted"""   
    
        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( OBSERVERS )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( MANAGE_CUSTOM_OBSERVERS )
        observersSelection.click()

        customObservers = self.frysk.dialog( CUSTOM_OBSERVERS )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        for i in range( self.matrixLength ):
            observerToVerify = self.theMatrix[i]
            observerNameToVerify = observerToVerify.getName()
  
            try:
                observerInGui = customTable.child( name = observerNameToVerify, roleName='table cell' )
               # Correct, but not optimal: observerInGui.actions['activate'].do()
                observerInGui.doAction('activate')
                observerInGui.grabFocus()
                deleteButton = customObservers.button( 'Delete' )
                deleteButton.click()
            except dogtail.tree.SearchError:
                self.fail ( 'Error - unable to locate Observer with name = ' + observerNameToVerify )
            else:  
                self.theLogWriter.writeResult({'INFO' :  'No error - successfully found ' + observerNameToVerify })
                self.TestString.compare(self.theLogWriter.scriptName + '.teardown()', observerNameToVerify, observerNameToVerify)
                self.assertEqual(observerNameToVerify, observerNameToVerify)
                
        # Resturn to the Frysk main menu
        okButton = customObservers.button( 'OK' )
        okButton.click()
       
        # Exit Frysk
        endFrysk( self.frysk )
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  })
 
    def testUpdateObservers( self ):      
        """Check that the newly created Observers can be queried and updated"""   
    
        # Select the 'Observers' menu item
        observersItem = self.frysk.menuItem( OBSERVERS )
        observersItem.click()

        # And the menu pick to access Observers
        observersSelection = observersItem.menuItem( MANAGE_CUSTOM_OBSERVERS )
        observersSelection.click()

        customObservers = self.frysk.dialog( CUSTOM_OBSERVERS )
        customScrollPane = customObservers.child( roleName='scroll pane' )
        customTable = customScrollPane.child( roleName = 'table' )

        for i in range( self.matrixLength ):
            observerToVerify = self.theMatrix[i]
            observerNameToVerify = observerToVerify.getName()
  
            try:
                observerInGui = customTable.child( name = observerNameToVerify, roleName='table cell' )
                # Correct, but not optimal: observerInGui.actions['activate'].do()
                observerInGui.doAction('activate')
                observerInGui.grabFocus()
            except dogtail.tree.SearchError:
                self.fail ( 'Error - unable to locate Observer with name = ' + observerNameToVerify )
            else:
                self.TestString.compare(self.theLogWriter.scriptName + '.testUpdateObservers()', observerNameToVerify, observerNameToVerify)
                self.assertEqual(observerNameToVerify, observerNameToVerify)                
                
            editButton = customObservers.button( 'Edit' )
            editButton.click()
            
            observerDetails = self.frysk.dialog( CUSTOM_OBSERVER_DIALOG )
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
