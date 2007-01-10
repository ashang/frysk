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

package frysk.gui.monitor.eventviewer;

import org.gnu.gtk.HBox;

public class Box
    extends HBox
{
  
  Box ()
  {
    super(false,6);
//    this.packStart(new Label(label), false, true, 0);
//    this.selected = false;
  }

//  public void draw (GdkCairo cairo)
//  {
//
//    cairo.setSourceColor(Color.BLUE);
//    cairo.newPath();
//    cairo.moveTo(this.getX()+2,this.getY()+12);
//    cairo.showText(this.getName());
//    cairo.stroke();
//    
////    cairo.restore(); 
////    cairo.setSourceColor(Color.BLUE);
//    
//    //cairo.moveTo(x,y);
//    cairo.setSourceRGBA(0,0,1, 0.1);
//    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+this.getWidth(), this.getY()+this.getHeight()));
//    cairo.fill();
//    
//    if(this.selected){
//      cairo.setLineWidth(1);
//    }else{
//      cairo.setLineWidth(0.2);
//    }
//    
//    cairo.setSourceColor(Color.BLUE);
//    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+this.getWidth(), this.getY()+this.getHeight()));
//    cairo.stroke();
//    
//    this.drawChildren(cairo);
//    //cairo.restore();
//  }

//  public boolean mouseEvent (MouseEvent event)
//  {
//  
//    if(event.isOfType(MouseEvent.Type.BUTTON_PRESS)){
//      if(isInside((int)event.getX(), (int)event.getY())){
//        this.selected = true;
//      }else{
//        this.selected = false;
//      }
//    }
//    
//    return false;
//  }
}
