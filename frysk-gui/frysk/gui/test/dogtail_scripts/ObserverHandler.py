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
Script name:    ObserverHandler.py
Creation date:  May 2006
Purpose:        Support tests for creation of Frysk Debug Session Custom Observers
Summary:        Simple parser for Frysk Custom Observer debug session data
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

# Import XML/SAX and regular expression modules
import xml.sax.handler
import re

# Import Frysk test objects
from Observer import Observer
from DebugProcess import DebugProcess
from DebugSession import DebugSession
from ObserverElement import ObserverElement
from ObserverPoints import ObserverPoints


# The input data to this parser takes this form:
#
#<Observer type="frysk.gui.monitor.observers.TaskForkedObserver" name="NewForkingCustomObserver" tooltip="Fires when a proc forks">
#  <actionPoints>
#    <actionPoint name="Generic Actions" tooltip="Actions that dont take any arguments">
#      <items>
#        <elements>
#          <element type="frysk.gui.monitor.actions.LogAction" name="Log" tooltip="logs what is going on with this observer plus a user set comment" argument="bedrock" />
#          <element type="frysk.gui.monitor.actions.Resume" name="Resume" tooltip="Resume execution of the current process" argument="null" />
#        </elements>
#      </items>
#    </actionPoint>
#    <actionPoint name="forking thread" tooltip="Thread that performed the fork">
#      <items>
#        <elements />
#      </items>
#    </actionPoint>
#    <actionPoint name="forked thread" tooltip="Main thread of newly forked process">
#      <items>
#        <elements />
#      </items>
#    </actionPoint>
#  </actionPoints>
#  <filterPoints>
#    <filterPoint name="forking thread" tooltip="Thread that performed the fork">
#      <items>
#        <elements>
#          <element type="frysk.gui.monitor.filters.TaskProcNameFilter" name="Name" tooltip="name of the process" argument="fred" />
#        </elements>
#      </items>
#    </filterPoint>
#    <filterPoint name="forked thread" tooltip="Main thread of newly forked process">
#      <items>
#        <elements>
#          <element type="frysk.gui.monitor.filters.TaskProcNameFilter" name="Name" tooltip="name of the process" argument="barney" />
#        </elements>
#      </items>
#    </filterPoint>
#  </filterPoints>
#</Observer>
#
 
class ObserverHandler(xml.sax.handler.ContentHandler):
  def __init__(self):
    self.mapping = {}
    self.currentTag = 'document'
    self.parentTag = 'document'
    self.actionPointFlag = False
    self.filterPointFlag = False
    self.theObserver = Observer()
    self.emptyActionPointFlag = False
    #self.buffer = ''
 
  #-------------------------------
  # def startDocument(self):
  
  #-------------------------------
  # def endDocument(self):

  #-------------------------------
  def startElement(self, name, attributes):
    self.parentTag = self.currentTag
    self.currentTag = name
    
    if self.currentTag == 'Observer':
      self.buffer = ''
      self.theObserver = Observer()
      self.theObserver.setType (attributes['type'])
      self.theObserver.setName (attributes['name'])
      self.theObserver.setDescription (attributes['tooltip'])

    elif self.currentTag == 'actionPoints':
      self.theActionPoints = []
      #print 'Entering actionPoints'
 
    elif self.currentTag == 'actionPoint':
      self.actionPointFlag = True
      #print 'Entering actionPoint'
      self.tempActionPoint = ObserverPoints()
      self.tempActionPointElements = []
      self.tempActionPoint.setName(attributes['name'])
       
    elif self.currentTag == 'filterPoints':
      self.theFilterPoints = []
      #print 'Entering filterPoints'

    elif self.currentTag == 'filterPoint':
      self.filterPointFlag = True
      #print 'Entering filterPoint'
      self.tempFilterPoint = ObserverPoints()
      self.tempFilterPointElements = []
      self.tempFilterPoint.setName(attributes['name'])
      #print 'DEBUG - found a filterPoint - argument = ' + attributes['argument']
      if attributes['name'] == 'forking thread':
          self.tempFilterPoint.setName('Name forking thread')
      if attributes['name'] == 'forked thread':
          self.tempFilterPoint.setName('Name forked thread')
 
#   elif self.currentTag == 'elements':
      
    elif self.currentTag == 'element':

      # Used to look for Observer Action data      
      p = re.compile('frysk.gui.monitor.actions*')
        
      try:
        if attributes['type'] == 'frysk.gui.monitor.filters.TaskProcNameFilter':
            self.tempObserverElement = ObserverElement()
            self.tempObserverElement.setType(attributes['type'])
            self.tempObserverElement.setName(attributes['name'])
            self.tempObserverElement.setArgument(attributes['argument'])
            #print 'DEBUG - found a filterPoint - arg = ' + attributes['argument']
            #self.tempObserverElement.dump()
        
        #elif attributes['type'] == 'frysk.gui.monitor.actions.LogAction':
        # If we found an Action that matches the regexp defined above
        elif p.search (attributes['type']) != 'None':
          #print p.search (attributes['type'])      
          self.tempObserverElement = ObserverElement()
          self.tempObserverElement.setType(attributes['type'])
          self.tempObserverElement.setName(attributes['name'])
          
          # The name attribute in the input file does not equal the
          # GUI name - so, make the change here.
          if attributes['name'] == 'Log':
              self.tempObserverElement.setName('Log Generic Actions')
          if attributes['name'] == 'Resume':
              self.tempObserverElement.setName('Resume Generic Actions')
          if attributes['name'] == 'Stop':
              self.tempObserverElement.setName('Stop Generic Actions')

          self.tempObserverElement.setArgument(attributes['argument'])         
 
          #print 'DEBUG - found an ActionPoint - arg = ' + attributes['argument']
          #self.tempObserverElement.dump()
          
      except:
        self.observerFlag = True
#        self.theObservers.append(attributes['name']) 
       
 #   elif self.currentTag == 'observers':
 #     self.theObservers = []
    
#   elif self.currentTag == 'tagsets':


  #-------------------------------
  def characters(self, data):
    self.buffer += data
 
  #-------------------------------
  def endElement(self, name):
    if name == 'Observer':
        print 'END of Observer'
              
    elif name == 'actionPoints':
        self.theObserver.setActionPoints(self.theActionPoints)
#        print 'END of actionPoints'

    elif name == 'actionPoint':
         #print 'END of actionPoint'
         self.tempActionPoint.setElements(self.tempActionPointElements)
         if self.emptyActionPointFlag != True:
             self.theActionPoints.append(self.tempActionPoint)
         self.actionPointFlag = False
         self.emptyActionPointFlag = True

    elif name == 'filterPoints':
        self.theObserver.setFilterPoints(self.theFilterPoints)
#        print 'END of filterPoints'

    elif name == 'filterPoint':
#        print 'END of filterPoint'
         self.tempFilterPoint.setElements(self.tempFilterPointElements)
         self.theFilterPoints.append(self.tempFilterPoint)         
         self.filterPointFlag = False        

#    elif name == 'elements':
#        print 'END of elements'
      
    elif name == 'element':     
        if self.actionPointFlag == True:
            self.tempActionPointElements.append(self.tempObserverElement)
            if self.tempObserverElement.getType() == 'default type':
                self.emptyActionPointFlag = True
        elif self.filterPointFlag == True:
            #self.tempObserverElement.dump()
            self.tempFilterPointElements.append(self.tempObserverElement)

#        print 'END of element'

