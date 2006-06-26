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
Script name:    FryskHandler.py
Creation date:  April 2006
Purpose:        To support tests of Frysk Debug Session Druid
Summary:        Simple parser for Frysk Druid/Assistant debug session data
'''

__author__ = 'Len DiMaggio <ldimaggi@redhat.com>'

import xml.sax.handler
from DebugProcess import DebugProcess
from DebugSession import DebugSession

# The input data to this parser takes this form:
#
#  <Session type="frysk.gui.sessions.Session" name="another new session" tooltip="NoTootip">
#  <procs>
#    <elements>
#      <element type="frysk.gui.sessions.DebugProcess" name="init" tooltip="init" executablePath="init">
#        <observers>
#          <element name="Task Terminating Observer" />
#          <element name="Exit Notification Observer" />
#          <element name="Exec Observer" />
#        </observers>
#        <tagsets />
#      </element>
#      <element type="frysk.gui.sessions.DebugProcess" name="xterm" tooltip="xterm" executablePath="xterm">
#        <observers>
#          <element name="Task Terminating Observer" />
#          <element name="Fork Observer" />
#        </observers>
#        <tagsets />
#      </element>
#      <element type="frysk.gui.sessions.DebugProcess" name="FryskGui" tooltip="FryskGui" executablePath="FryskGui">
#        <observers>
#          <element name="Task Terminating Observer" />
#          <element name="TaskCloneObserver" />
#          <element name="Syscall Observer" />
#        </observers>
#        <tagsets />
#      </element>
#    </elements>
#  </procs>
#  </Session>
 
class FryskHandler(xml.sax.handler.ContentHandler):
  def __init__(self):
    self.mapping = {}
    self.currentTag = "document"
    self.parentTag = "document"
    self.debugProcessFlag = False
    self.observerFlag = True
    self.theDebugSession = DebugSession()

  #-------------------------------
  # def startDocument(self):
  
  #-------------------------------
  # def endDocument(self):

  #-------------------------------
  def startElement(self, name, attributes):
    self.parentTag = self.currentTag
    self.currentTag = name
    
    if self.currentTag == "Session":
      self.buffer = ""
      self.theDebugSession = DebugSession()
      self.theDebugSession.setType (attributes["type"])
      self.theDebugSession.setName (attributes["name"])

    elif self.currentTag == "procs":
      self.theDebugProcesses = []
      
#   elif self.currentTag == "elements":
      
    elif self.currentTag == "element":
      try:
        if attributes["type"] == "frysk.gui.sessions.DebugProcess":
          self.tempDebugProcess = DebugProcess()
          self.debugProcessFlag = True
          self.tempDebugProcess.setType(attributes["type"])
          self.tempDebugProcess.setName(attributes["name"])          
      except:
        self.observerFlag = True
        self.theObservers.append(attributes["name"]) 
       
    elif self.currentTag == "observers":
      self.theObservers = []
    
#   elif self.currentTag == "tagsets":


  #-------------------------------
  def characters(self, data):
    self.buffer += data
 
  #-------------------------------
  def endElement(self, name):
    if name == "Session":
      self.theDebugSession.setProcesses(self.theDebugProcesses)
      
    # elif name == "procs":

    # elif name == "elements":

    elif name == "element":
      if self.debugProcessFlag == True:
        self.theDebugProcesses.append(self.tempDebugProcess)
        self.debugProcessFlag = False

    elif name == "observers":
      self.observerFlag = False
      self.tempDebugProcess.setObservers(self.theObservers)
      
    # elif name == "tagsets":

