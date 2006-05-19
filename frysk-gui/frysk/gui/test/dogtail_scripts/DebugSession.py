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
Script name:    DebugSession.py
Creation date:  April 2006
Purpose:        Support datamodel of Frysk Debug session objects
Summary:        Simple, demo/prototype dogtail test script for Fryske
"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

import DebugProcess

class DebugSession:
  type = 'default type'
  name = 'default name'
  processes = []
  processesDict = {}

  # constructor - not sure if we want this
  # def __init__(self, value):
  #   self.name = value

  # getters and setters methods
  def setName(self, value):
    self.name = value
  
  def setType (self, value):
    self.type = value

  def setProcesses (self, value):
    self.processes = value

  def setProcessesDict (self, theProcesses):
    # use the process names as the keys, and the processes as the values
    theProcessNames = []
    theProcessObjects = []
    for x in theProcesses:
      print x.getName()
      theProcessNames.append(x.getName())
      theProcessObjects.append(x)
    self.processesDict = dict(zip (theProcessNames, theProcessObjects))

  def getProcessesDict(self):
    return self.processesDict
      
  def getProcessByName (self, theProcessName):
    return self.processesDict.get(theProcessName)
      
  def getName(self):
    return self.name
  
  def getType(self):
    return self.type

  def getProcesses(self):
    return self.processes

  def dump(self):
    print 'name=' + self.name + ' ' + 'type=' + self.type
    for x in self.processes:
      x.dump()

  # ------------------------
  # Function to determine if two DebugSession objects are equal
  def isequal (self, theOtherDebugSession):
    returnFlag = True

    print self.getName() + theOtherDebugSession.getName()

    if self.getName() != theOtherDebugSession.getName():
      returnFlag = False

      theSessionProcesses = dict(self.getProcessesDict())
      theOtherSessionProcesses = dict(theOtherDebugSession.getProcessesDict())

      print theSessionProcesses
      print theOtherSessionProcesses
      
      theKeys = theSessionProcesses.keys()
      theOtherKeys = theOtherSessionProcesses.keys()

      if len(theKeys) == len(theOtherKeys):
        
        theKeys.sort()
        theOtherKeys.sort()

        for x in theKeys:
          theProcess = theSessionProcesses.get(x)
          theOtherProcess = theOtherSessionProcesses.get(x)

          print "DEBUG = " + theProcess.getName()
          print "DEBUG = " + theOtherProcess.getName()
        
          if not theProcess.isequal(theOtherProcess):
            returnFlag = False

      else:
        returnFlag = False      

    return returnFlag
