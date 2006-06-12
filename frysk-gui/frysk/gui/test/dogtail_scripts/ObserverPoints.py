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
Script name:    ObserverElement.py
Creation date:  May 2006
Purpose:        Support datamodel of Frysk custom observer objects
Summary:        Simple, demo/prototype dogtail test script for Fryske
"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

from ObserverElement import ObserverElement

class ObserverPoints:
    type = 'default type'
    name = 'default name'
    elements = []
    elementsDict = {}

    # constructor - not sure if we want this
    # def __init__(self, value):
    #   self.name = value

    # getters and setters methods
    def setType ( self, value ):
        self.type = value
  
    def setName ( self, value ):
        self.name = value
        
    def setElements (self, value):
        self.elements = value
        
    def setElementsDict ( self, theElements ):
    # use the process names as the keys, and the processes as the values
      theElementNames = []
      theElementObjects = []
      for x in theElements:
        #print x.getName()
        theElementNames.append( x.getName() )
        theElementObjects.append( x )
      self.elementsDict = dict( zip ( theElementNames, theElementObjects ) )        

    def getType( self ):
        return self.type
 
    def getName( self ):
        return self.name
    
    def getElements (self):
        return self.elements

    def dump( self ):
        print 'in ObserverPoints'
        print 'name=' + self.name
        print 'type=' + self.type
        for x in self.elements:
            x.dump()
      
    # ------------------------
    # Function to determine if two ObserverPoints objects are equal
    def isequal ( self, theOtherPoints ):
      returnFlag = True
  
      if self.getType() != theOtherElement.getType():
          returnFlag = False
          
      theElements = dict( self.getElementsDict() )
      theOtherElements = dict( theOtherPoints.getElementsDict() )
    
      theKeys = theElements.keys()
      theOtherKeys = theOtherElements.keys()

      if len( theKeys ) == len( theOtherKeys ):
        
        theKeys.sort()
        theOtherKeys.sort()

        for x in theKeys:
          theElement = theElements.get( x )
          theOtherElement = theOtherElements.get( x )

          print "DEBUG = " + theElement.getName()
          print "DEBUG = " + theOtherElement.getName()
        
          if not theElement.isequal( theOtherElement ):
            returnFlag = False

      return returnFlag
