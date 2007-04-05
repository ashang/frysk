// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.


package frysk.gui.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Element;

/**
 * The job of a LiaisonPoint is to execute all its liaison Items with
 * its specialized argument. For example a TaskFilterPoint is a
 * LiaisonPoint that is specialized in calling all its filters with
 * the task its deticated to as an argument. If for instance the
 * update function of an observer looks like this: updateForked(Task
 * parent, Task child) then that observer will create two
 * TaskFilterPoints; deticating one to the parent task and the other
 * to the child task.  Thereby allowing the user to add TaskFilter(s)
 * to either TaskFilterPoint and filter on the properties of either
 * task. A LiaisonPoint can either be a FilterPoint or an ActionPoint.
 */
public abstract class LiaisonPoint
    extends GuiObject
    implements SaveableXXX
{
  protected ObservableLinkedList items;

  Logger logger = WindowManager.logger;

  public LiaisonPoint ()
  {
    super();
    this.items = new ObservableLinkedList();
  }

  public LiaisonPoint (String name, String toolTip)
  {
    super(name, toolTip);
    this.items = new ObservableLinkedList();
  }

  public LiaisonPoint (LiaisonPoint other)
  {
    super(other);
    this.items = new ObservableLinkedList(other.items, true); // Do copy items
  }

  /**
   * Retrieves a list of applicable items from the appropriate Manager.
   */
  public abstract ObservableLinkedList getApplicableItems ();

  public void addItem (LiaisonItem item)
  {
    logger.log(Level.FINE, "{0} addItem {1}\n", new Object[] { this, item });
    this.items.add(item);
  }

  public void removeItem (LiaisonItem item)
  {
    logger.log(Level.FINE, "{0} removeItem {1}\n", new Object[] { this, item });
    if (! this.items.remove(item))
      {
        throw new IllegalArgumentException(
                                           "the passed item ["
                                               + item
                                               + "] is not a member of this Liason point");
      }
  }

  public ObservableLinkedList getItems ()
  {
    return this.items;
  }

  public void save (Element node)
  {
    super.save(node);

    Element filtersXML = new Element("items");
    this.items.save(filtersXML);
    node.addContent(filtersXML);
  }

  public void load (Element node)
  {
    super.load(node);
    Element elemetnsXML = node.getChild("items");
    this.items.load(elemetnsXML);
  }

}
