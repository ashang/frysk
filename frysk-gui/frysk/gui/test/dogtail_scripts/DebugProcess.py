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
Script name:    DebugProcess.py
Creation date:  April 2006
Purpose:        Support datamodel of Frysk Debug session objects
Summary:        Simple, demo/prototype dogtail test script for Frysk
"""
__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

from Observer import Observer

class DebugProcess:
  type = 'default type'
  name = 'default name'
  execPath = 'default path'
  observers = []

  # constructor - not sure if we want this
  # def __init__(self, value):
  #   self.name = value

  # getters and setters methods
  def setName(self, value):
    self.name = value
  
  def setType (self, value):
    self.type = value

  def setPath (self, value):
    self.execPath = value

  def setObservers (self, value):
    self.observers = value
  
  def getName(self):
    return self.name
  
  def getType(self):
    return self.type

  def getPath(self):
    return self.execPath

  def getObservers(self):
    return self.observers

  def dump(self):
    print 'process name=' + self.name + ' ' + 'type=' + self.type
    for x in self.observers:
      print 'process observer = ' + x

  # -------------------------------------
  # Compare two DebugProcess objects
  
  def isequal(self, theOtherDebugProcess):
    returnFlag = True

    if self.getName() != theOtherDebugProcess.getName():
      returnFlag = False
  
    if self.getType() != theOtherDebugProcess.getType():
      returnFlag = False

    theOtherProcessObservers = theOtherDebugProcess.getObservers()
    thisProcessObservers = self.getObservers()

    if len(theOtherProcessObservers) == len (thisProcessObservers):

      print len(theOtherProcessObservers)
      print len (thisProcessObservers)

      theOtherProcessObservers.sort()
      thisProcessObservers.sort()

      i = 0
      while i < len(thisProcessObservers):
        print theOtherProcessObservers[i] + "  " + thisProcessObservers[i]

        if theOtherProcessObservers[i] != thisProcessObservers[i]:
          returnFlag = False
        i = i + 1

    else:
      returnFlag = False
      
    return returnFlag
