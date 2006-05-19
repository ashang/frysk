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

class Observer:
  
  # Set up some default values
  name = 'defaultName'
  loggingAction = 'Log Generic Actions'
  event = 'Exec Observer'
  filterName = "Name Exec'ing Thread"
  filterQualifier = 'is not'
  filterParameter = 'null'
  actionsName = 'Log Generic Actions'
  actionsNameParameter = 'null'
  afterAction = 'Resume thread'

  # constructor - not sure if we want this
  # def __init__(self, value):
  #   self.name = value
  
  # getters and setters methods
  def setName(self, value):
    self.name = value

  def setLoggingAction (self, value):
    self.loggingAction = value
    
  def setEvent (self, value):
    self.event = value
     
  def setFilterName (self, value):
    self.filterName = value 
     
  def setFilterQualifier (self, value):
    self.filterQualifier = value
       
  def setFilterParameter (self, value):
    self.filterParameter = value

  def setActionsName (self, value):
    self.actionsName = value 
        
  def setActionsParameter (self, value):
    self.actionsParameter = value 
    
  def setAfterAction (self, value):
    self.afterActions = value 
  
  def getName(self):
    return self.name
  
  def getLoggingAction(self):
    return self.loggingAction

  def getEvent (self):
    return self.event
     
  def getFilterName (self):
    return self.filterName
     
  def getFilterQualifier (self):
    return self.filterQualifier
       
  def getFilterParameter (self):
    return self.filterParameter

  def getActionsName (self):
    return self.actionsName
        
  def getActionsParameter (self):
    return self.actionsParameter
    
  def getAfterAction (self):
    return self.afterActions
  
  def dump(self):
    print 'name=' + self.name
    print 'loggingAction=' + self.loggingAction
    print 'event=' + self.event
    print 'filterName=' + self.filterName
    print 'filterQualifier=' + self.filterQualifier
    print 'filterParameter=' + self.filterParameter
    print 'actionsName=' + self.actionsName
    print 'actionsNameParameter=' + self.actionsNameParameter
    print 'afterAction=' + self.afterAction
    
  # -------------------------------------
  # Compare two Observer objects
  
  def isequal(self, theOtherObserver):
    returnFlag = True
    return returnFlag


