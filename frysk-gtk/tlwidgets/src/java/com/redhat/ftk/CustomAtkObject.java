package com.redhat.ftk;

import java.util.LinkedList;

import org.gnu.atk.AtkObject;
import org.gnu.glib.Handle;
import org.gnu.glib.Type;

public class CustomAtkObject extends AtkObject
{
 
  LinkedList atkObjects;
  
  static {
    System.loadLibrary ("ftk");
    System.loadLibrary ("ftkjni");
  }

  public CustomAtkObject (CustomDrawingArea area)
  {
    super(ftk_custom_atk_object_new(area.getHandle()));
    
    this.atkObjects = new LinkedList();
  }
  
  public void setStartIndex(int startIndex){
    ftk_custom_atk_object_set_start_index(getHandle(), startIndex);
  }
  
  public void setNumberOfChildren(int n){
    ftk_custom_atk_object_set_n_children(getHandle(), n);
  }
  
  public static Type getType(){
    return new Type(ftk_custom_atk_object_get_type());
  }
  
  public void addChild(CustomAtkObject atkObject){
    atkObject.setParent(this);
    this.atkObjects.add(atkObject);
    this.setNumberOfChildren(this.atkObjects.size());
  }
  
  public static CustomAtkObject getEventAccessible(Handle handle,int index){
    CustomAtkObject atkObject = (CustomAtkObject)CustomAtkObject.getAtkObjectFromHandle(handle);
    return (CustomAtkObject)atkObject.atkObjects.get(index);
  }
  
  native static final protected Handle ftk_custom_atk_object_new (Handle widget);
  native static final protected int ftk_custom_atk_object_get_type();
  native static final protected void ftk_custom_atk_object_set_n_children(Handle handle, int n);
  native static final protected void ftk_custom_atk_object_set_start_index(Handle handle, int i);
}
