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
