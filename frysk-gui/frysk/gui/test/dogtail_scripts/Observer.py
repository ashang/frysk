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
Script name:    Observer.py
Creation date:  April 2006
Purpose:        Support datamodel of Frysk Observer objects
Summary:        Simple, demo/prototype dogtail test script for Frysk
"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

from ObserverElement import ObserverElement
from ObserverPoints import ObserverPoints

class Observer:
  
  # Set up some default values
  name = 'default Name'
  type = 'default type'
  description = 'default description'
  loggingAction = 'Log Generic Actions'
  afterAction = 'Resume thread'
  filterPoints = ObserverPoints()
  actionPoints = ObserverPoints()
  filterPointsDict = {}
  actionPointsDict = {}
  
  # constructor - not sure if we want this
  # def __init__(self, value):
  #   self.name = value
  
  # getters and setters methods
  def setName( self, value ):
    self.name = value

  def setType( self, value ):
    self.type = value

  def setLoggingAction ( self, value ):
    self.loggingAction = value
    
  def setFilterPoints( self, value ):
      self.filterPoints = value
      
  def setDescription( self, value ):
    self.description = value

  def setFilterPointsDict ( self, theFilterNames ):
    # use the names as the keys, and the objects as the values
    theFilterPointNames = []
    theFilterPointObjects = []
    for x in theFilterNames:
      #print "DEBUG - name = " + x.getName()
      theFilterPointNames.append( x.getName() )
      theFilterPointObjects.append( x )
    self.filterPointsDict = dict( zip ( theFilterPointNames, theFilterPointObjects ) )      
            
  def setActionPointsDict ( self, theActionNames ):
    # use the names as the keys, and the objects as the values
    theActionPointNames = []
    theActionPointObjects = []
    for x in theActionNames:
      #print "DEBUG - name = " + x.getName()
      theActionPointNames.append( x.getName() )
      theActionPointObjects.append( x )
    self.ActionPointsDict = dict( zip ( theActionPointNames, theActionPointObjects ) )  
   
  def setActionPoints ( self, value ):    
      self.actionPoints = value
    
  def setAfterAction ( self, value ):
    self.afterActions = value 
  
  def getName( self ):
    return self.name

  def getDescription( self ):
    return self.description

  def getType( self ):
    return self.type
  
  def getLoggingAction( self ):
    return self.loggingAction

  def getFilterPoints( self ):
      return self.filterPoints
 
  def getFilterPointsDict( self ):
      return self.filterPointsDict
      
  def getActionPointsDict( self ):
      return self.filterPointsDict
      
  def getActionPoints ( self ):    
      return self.actionPoints
    
  def getAfterAction ( self ):
    return self.afterActions
  
  def dump( self ):
    print 'dump of Observer object:'
    print 'name=' + self.name
    print 'type=' + self.type
    print 'description=' + self.description
    #print 'loggingAction=' + self.loggingAction
    print 'afterAction=' + self.afterAction
    for x in self.filterPoints:
        x.dump()        
    for x in self.actionPoints:
        x.dump()
        
  # -------------------------------------
  # Compare two Observer objects
  
  def isequal( self, theOtherObserver ):
    returnFlag = True
 
    if self.getName() != theOtherObserver.getName():
      returnFlag = False

    if self.getType() != theOtherObserver.getType():
      returnFlag = False         
      
    if self.getDescription() != theOtherObserver.getDescription():
      returnFlag = False         
      
    # For the FilterPoints, we need to get the data into dictionaries
    # that can be sorted by name
    theObserverFilterPoints = dict( self.getFilterPointsDict() )
    theOtherObserverFilterPoints = dict( theOtherObserver.getFilterPointsDict() )
    theKeys = theObserverFilterPoints.keys()
    theOtherKeys = theOtherObserverFilterPoints.keys()

    if len( theKeys ) == len( theOtherKeys ):
        theKeys.sort()
        theOtherKeys.sort()
    
        for x in theKeys:
          theFilterPoint = theObserverFilterPoints.get( x ) 
          theOtherFilterPoint = theOtherObserverFilterPoints.get( x )
          #print "DEBUG = " + theFilterPoint.getName() + theFilterPoint.getName()
          #print "DEBUG = " + theOtherFilterPoint.getName() + theOtherFilterPoint.getName()
          if not theFilterPoint.isequal( theOtherFilterPoint ):
              returnFlag = False
    else:
      returnFlag = False
      
           
    # Same for For the ActionPoints, we need to get the data into dictionaries
    # that can be sorted by name
    theObserverActionPoints = dict( self.getActionPointsDict() )
    theOtherObserverActionPoints = dict( theOtherObserver.getActionPointsDict() )
    
    theKeys = theObserverActionPoints.keys()
    theOtherKeys = theOtherObserverActionPoints.keys()

    if len( theKeys ) == len( theOtherKeys ):
        theKeys.sort()
        theOtherKeys.sort()
    
        for x in theKeys:
          theActionPoint = theObserverActionPoints.get( x ) 
          theOtherActionPoint = theOtherObserverActionPoints.get( x )
          if not theActionPoint.isequal( theOtherActionPoint ):
              returnFlag = False
    else:
      returnFlag = False

    # This is not yet (20060609) implemented in the Observer data file  
    # if self.getAfterAction() != theOtherObserver.getAfterAction():
    #   returnFlag = False  
    
    if not returnFlag:
        print 'expected:'
        self.dump()
        print 'actual;'
        theOtherObserver.dump()
     
    return returnFlag

  # -------------------------------------
  # Compare two Observer objects
  
#  def compareDictionaries (self, theObserverPoints, theOtherObserverPoints ):
#    returnFlag = True
#      
#    theKeys = theObserverPoints.keys()
#    theOtherKeys = theOtherObserverPoints.keys()
#
#    if len( theKeys ) == len( theOtherKeys ):
#        theKeys.sort()
#        theOtherKeys.sort()
#    
#        for x in theKeys:
#          theObserverPoint = theObserverPoints.get( x ) 
#          theOtherObserverPoint = theOtherObserverPoints.get( x )
#          print "DEBUG = " + theObserverPoint.getName() + theOtherObserverPoint.getName()
#          theObserverPoint.dump()
#          theOtherObserverPoint.dump()
#          if not theObserverPoint.isequal( theOtherObserverPoint ):
#              returnFlag = False
#    else:
#      returnFlag = False
#      
#      return returnFlag
      
