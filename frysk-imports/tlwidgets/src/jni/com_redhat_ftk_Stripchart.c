/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */

#include <jni.h>
#include "gtk/gtk.h"
#include "ftkstripchart.h"
#include "jg_jnu.h"
#include "gtk_java.h"

#ifdef __cplusplus
extern "C" 
{
#endif

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_get_type
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1get_1type (JNIEnv *env, 
							jclass cls) 
{
    return (jint)ftk_stripchart_get_type ();
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_new
 */
JNIEXPORT jobject JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1new (JNIEnv *env, 
						  jclass cls) 
{
    return getGObjectHandle(env, (GObject *) ftk_stripchart_new ());
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_resize
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1resize (JNIEnv *env, 
						     jclass cls,
						     jobject sc,
						     jint    wd,
						     jint    hg)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  int width  = (int) wd;
  int height = (int) hg;
  ftk_stripchart_resize (stripchart, width, height);
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_set_event_rgb
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1set_1event_1rgb (JNIEnv *env, 
							      jclass cls,
							      jobject sc,
							      jint    ty,
							      jint rj,
							      jint gj,
							      jint bj)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  FtkStripchartTypeEnum type = (FtkStripchartTypeEnum) ty;
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  ftk_stripchart_set_event_rgb (stripchart, type, red, green, blue);
}


/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_set_event_title
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1set_1event_1title (JNIEnv *env, 
								jclass cls,
								jobject sc,
								jint    ty,
								jstring ti)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  FtkStripchartTypeEnum type = (FtkStripchartTypeEnum) ty;
  const gchar * title =
     (const gchar *)(*env)->GetStringUTFChars(env, ti, NULL);
  ftk_stripchart_set_event_title (stripchart, type, title);
  (*env)->ReleaseStringUTFChars(env, ti, title);
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_set_update
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1set_1update (JNIEnv *env, 
							  jclass cls,
							  jobject sc,
							  jint    ud)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  int update  = (int) ud;
  ftk_stripchart_set_update (stripchart, update);
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_set_range
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1set_1range (JNIEnv *env, 
							 jclass cls,
							 jobject sc,
							 jint    rg)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  int range  = (int) rg;
  ftk_stripchart_set_range (stripchart, range);
}

/*
 * Class:     com.redhat.ftk.Stripchart
 * Method:    ftk_stripchart_append_event
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_Stripchart_ftk_1stripchart_1append_1event (JNIEnv *env, 
							    jclass cls,
							    jobject sc,
							    jint    ty)
{
  FtkStripchart * stripchart =
    (FtkStripchart *)getPointerFromHandle(env, sc);
  FtkStripchartTypeEnum type = (FtkStripchartTypeEnum) ty;
  ftk_stripchart_append_event (stripchart, type);
}



#ifdef __cplusplus
}
#endif
