/* DO NOT EDIT THIS FILE - it is machine generated */

#ifndef __com_redhat_ftk_CustomAtkObject__
#define __com_redhat_ftk_CustomAtkObject__

#include <jni.h>
#include "ftkcustomatkobject.h"

#include "gtk/gtk.h"
#include "jg_jnu.h"
#include "gtk_java.h"
#include <stdlib.h>

#ifdef __cplusplus
extern "C"
{
#endif

  JNIEnv *ENV = NULL;
  jclass CLS = NULL;

  JNIEXPORT jobject JNICALL Java_com_redhat_ftk_CustomAtkObject_ftk_1custom_1atk_1object_1new (JNIEnv *env, jclass cls, jobject widget){
    ENV = env;
    CLS = cls;

    GtkDrawingArea* drawingArea = (GtkDrawingArea*)getPointerFromHandle(env,widget);                                                               
    return getGObjectHandle(env, (GObject *) ftk_custom_atk_object_new (drawingArea));
  }

  JNIEXPORT jint JNICALL Java_com_redhat_ftk_CustomAtkObject_ftk_1custom_1atk_1object_1get_1type (JNIEnv *env, jclass cls){
    return (jint)ftk_custom_atk_object_get_type();      
  }
  
  JNIEXPORT void JNICALL Java_com_redhat_ftk_CustomAtkObject_ftk_1custom_1atk_1object_1set_1n_1children (JNIEnv *env, jclass cls, jobject obj, jint n){
    
    FtkCustomAtkObject* ftk_object = (FtkCustomAtkObject*)getPointerFromHandle(env, obj);
    ftk_custom_atk_object_set_n_children(ftk_object,n);
    
  }
  
  JNIEXPORT void JNICALL Java_com_redhat_ftk_CustomAtkObject_ftk_1custom_1atk_1object_1set_1start_1index (JNIEnv *env, jclass cls, jobject obj, jint i){
    
    FtkCustomAtkObject* ftk_object = (FtkCustomAtkObject*)getPointerFromHandle(env, obj);
    ftk_custom_atk_object_set_start_index(ftk_object,i);

  }
  

  gint ftk_custom_atk_object_get_n_children (AtkObject *accessible){
    return ((FtkCustomAtkObject*)accessible)->n_children;
  }
  
  AtkObject* ftk_custom_atk_object_ref_child      (AtkObject *accessible, gint     index){
    JNIEnv *env = ENV; 

    if (env == 0) {
      printf("env lookup faild\n");
      exit(0);
      return NULL;
    }
    
    jclass cls = CLS;
    if (cls == 0) {
      printf("cls lookup faild\n");
      exit(0);
      return NULL;
    }
    
    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "getEventAccessible", "(Lorg/gnu/glib/Handle;I)Lcom/redhat/ftk/CustomAtkObject;");
    if (mid == 0) {
      printf("method lookup failed\n");
      exit(0);
      return 0;
    }

    jobject handle = getGObjectHandle(env, (GObject*)accessible);
    jobject obj2 = (*env)->CallStaticObjectMethod(env,cls,mid,handle,index);
    if (obj2 == 0){
      printf("method call failed \n");
      exit(0);
      return 0;
    }

    return getPointerFromJavaGObject(env, obj2);
  }

#ifdef __cplusplus
}
#endif

#endif /* __com_redhat_ftk_CustomAtkObject__ */
