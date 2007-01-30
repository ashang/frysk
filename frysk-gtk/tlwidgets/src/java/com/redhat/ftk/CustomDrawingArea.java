package com.redhat.ftk;

import org.gnu.atk.AtkObject;
import org.gnu.glib.Handle;
import org.gnu.gtk.DrawingArea;

public class CustomDrawingArea extends DrawingArea
{
    
  static {
	System.loadLibrary ("ftk");
	System.loadLibrary ("ftkjni");
  }

  public CustomDrawingArea ()
  {
    super(ftk_custom_drawing_area_new());                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
  }
  
  public void setAcessible(AtkObject accessible){
    ftk_custom_drawing_area_set_accessible(getHandle(), accessible.getHandle());
  }
  
  native static final protected Handle ftk_custom_drawing_area_new ();
  native static final protected void   ftk_custom_drawing_area_set_accessible(Handle drawingArea, Handle atkObject);
}
