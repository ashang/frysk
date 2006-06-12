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
Script name:    TestLicense.py
Creation date:  April 2006
Purpose:        Verify correct license text is displayed by Frysk gui
Summary:        Simple, demo/prototype dogtail test script for Frysk
"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# Imports
from dogtail import tree
from dogtail import predicate
import sys

# Set up for logging
import dogtail.tc

# Set up for unit test framework
import unittest

# Test support functions
from FryskHelpers import startFrysk
from FryskHelpers import endFrysk
from FryskHelpers import skipDruid

class TestLicense (unittest.TestCase):

    def setUp(self):
        
        # Set up for logging
        self.TestString=dogtail.tc.TCString()
        self.theLogWriter = self.TestString.writer
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' starting'  })

        # Start up Frysk 
        self.FryskBinary = sys.argv[1]
        self.frysk = startFrysk(self.FryskBinary, self.theLogWriter)
        
        # Probably temporary - during test development
        skipDruid(self.frysk)
        
    def tearDown(self):    
        # Exit Frysk
        endFrysk(self.frysk)
        self.theLogWriter.writeResult({'INFO' :  'test script: ' + self.theLogWriter.scriptName + ' ending'  })


    def testLicense(self):      
        """Check that the license text is correct"""   
       
        # Define the expected license string
        expectedLicenseString = 'http://www.gnu.org/copyleft/gpl.html\n'
    
        # Select the 'Help' menu item
        helpItem = self.frysk.menuItem('Help')
        helpItem.click()
    
        # Select the 'About Frysk' Help menu item
        aboutItem = helpItem.menuItem('About')
        aboutItem.click()    
    
        # Open the 'About' dialog and its child filler dialog
        aboutFrame = self.frysk.dialog('About Frysk - Technology Preview')
            
        # Select the 'License' menu pick and click the button to open the license frame
        licenseButton = aboutFrame.button('License')
        licenseButton.click()
    
        # In the license frame, select the license text
        licenseFrame = self.frysk.dialog('License')
        licenseText = licenseFrame.child(roleName='text')
    
        # Compare the expected license string with the actual string, log the results
        self.TestString.compare('TestLicense.py', licenseText.text, expectedLicenseString)
        self.assertEqual(licenseText.text, expectedLicenseString)
    
        # Close the license text frame
        closebutton = licenseFrame.button('Close')
        closebutton.actions['press'].do()
    
        # Close the 'about Frysk' filler dialog
        closebutton = aboutFrame.button('Close')
        closebutton.click()
 
def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(TestLicense))
    return suite

if __name__ == '__main__':
  #unittest.main()
  unittest.TextTestRunner(verbosity=2).run(suite())

