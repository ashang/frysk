#! /usr/bin/env python

'''
Script name:    TestCredits.py
Script author:  ldimaggi
Creation date:  April 2006
Purpose:        Verify correct credits text is displayed by Frysk gui
Summary:        Simple, demo/prototype dogtail test script for Fryske
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# Imports
from dogtail import tree
from dogtail.utils import run
from dogtail import predicate

# Set up for logging
import dogtail.tc

# Set up for unit test framework
import unittest

class TestCredits (unittest.TestCase):

    def setUp(self):
        # Set up for logging
        self.TestString=dogtail.tc.TCString()
   
        # Start up Frysk 
        run ('/opt/Frysk/build/frysk-gui/frysk/gui/FryskGui')
   
        # Locate the Frysk application - note the application name of 
        # 'java-gnome' (sourceware.org/bugzilla #2591)
        self.frysk = tree.root.application ('java-gnome')

    def tearDown(self):    
        # Exit Frysk
        closeItem = self.frysk.findChild(predicate.IsAMenuItemNamed('Close'))
        closeItem.actions['click'].do()

    def testCredits(self):      
        """Check that the credits text is correct"""   
      
        # Define the expected credits string
        expectedCreditsString = 'TBD\n'

        # Select the 'Help' menu item
        helpItem = self.frysk.findChild(predicate.IsAMenuItemNamed('Help'))
        helpItem.actions['click'].do()
   
        # Select the 'About Frysk' Help menu item
        aboutItem = helpItem.findChild(predicate.IsAMenuItemNamed('About'))
        aboutItem.actions['click'].do()
   
        # Open the 'About' dialog and its child filler dialog
        aboutFrame = self.frysk.dialog('About Frysk - Technology Preview')
        aboutFiller = aboutFrame.child(roleName='filler')
   
        # Open the 'Credits' dialog
        creditsButton = aboutFiller.button('Credits')
        creditsButton.actions['click'].do()
   
        # Select the 'Credits' menu pick to view the credit text
        creditsFrame = self.frysk.dialog('Credits')
        creditsFiller = creditsFrame.child(roleName='filler')
   
        # As of 2006/04/26, the text is blank - so just exit for now
        pass
        closeButton = creditsFiller.button('Close')
        closeButton.actions['press'].do()
   
        # Close the 'about Frysk' filler dialog
        closebutton = aboutFiller.button('Close')
        closebutton.actions['click'].do()

def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(TestCredits))
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner(verbosity=2).run(suite())

