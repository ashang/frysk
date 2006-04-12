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
#include "ftkstripchartx.h"
#include "jg_jnu.h"
#include "gtk_java.h"

#ifdef __cplusplus
extern "C" 
{
#endif

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchart_get_type
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1get_1type (JNIEnv *env, 
							jclass cls) 
{
    return (jint)ftk_stripchartx_get_type ();
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_new
 */
JNIEXPORT jobject JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1new (JNIEnv *env, 
						  jclass cls) 
{
    return getGObjectHandle(env, (GObject *) ftk_stripchartx_new ());
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_resize
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1resize (JNIEnv *env, 
						     jclass cls,
						     jobject sc,
						     jint    wd,
						     jint    hg)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int width  = (int) wd;
  int height = (int) hg;
  ftk_stripchartx_resize (stripchartx, width, height);
}


/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_bg_rgb
 */

JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1bg_1rgb (JNIEnv *env, 
							      jclass cls,
							      jobject sc,
							      jint rj,
							      jint gj,
							      jint bj)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  ftk_stripchartx_set_bg_rgb (stripchartx,red, green, blue);
}


/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_readout_rgb
 */

JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1readout_1rgb (JNIEnv *env, 
								   jclass cls,
								   jobject sc,
								   jint rj,
								   jint gj,
								   jint bj)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  ftk_stripchartx_set_readout_rgb (stripchartx, red, green, blue);
}


/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_chart_rgb
 */

JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1chart_1rgb (JNIEnv *env, 
								 jclass cls,
								 jobject sc,
								 jint rj,
								 jint gj,
								 jint bj)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  ftk_stripchartx_set_chart_rgb (stripchartx, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_event_rgb
 */
/**************** removed pro-tem **************
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1event_1rgb (JNIEnv *env, 
							      jclass cls,
							      jobject sc,
							      jint    ty,
							      jint rj,
							      jint gj,
							      jint bj)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  FtkStripchartTypeEnum type = (FtkStripchartTypeEnum) ty;
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  ftk_stripchartx_set_event_rgb (stripchartx, type, red, green, blue);
}
***********************/


/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_event_title
 */
/**************** removed pro-tem **************
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1event_1title (JNIEnv *env, 
								jclass cls,
								jobject sc,
								jint    ty,
								jstring ti)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  FtkStripchartTypeEnum type = (FtkStripchartTypeEnum) ty;
  const gchar * title =
     (const gchar *)(*env)->GetStringUTFChars(env, ti, NULL);
  ftk_stripchartx_set_event_title (stripchartx, type, title);
  (*env)->ReleaseStringUTFChars(env, ti, title);
}
***********************/

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_new_event
 */
JNIEXPORT int JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1new_1event (JNIEnv *env, 
                                                            jclass cls,
							    jobject sc,
							    jstring ti,
							    jint rj,
							    jint gj,
							    jint bj)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  const gchar * title =
     (const gchar *)(*env)->GetStringUTFChars(env, ti, NULL);
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  return ftk_stripchartx_new_event (stripchartx, title, red, green, blue);
  (*env)->ReleaseStringUTFChars(env, ti, title);
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_update
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1update (JNIEnv *env, 
							  jclass cls,
							  jobject sc,
							  jint    ud)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int update  = (int) ud;
  ftk_stripchartx_set_update (stripchartx, update);
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_set_range
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1set_1range (JNIEnv *env, 
							 jclass cls,
							 jobject sc,
							 jint    rg)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int range  = (int) rg;
  ftk_stripchartx_set_range (stripchartx, range);
}

/*
 * Class:     com.redhat.ftk.StripchartX
 * Method:    ftk_stripchartx_append_event
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_StripchartX_ftk_1stripchartx_1append_1event (JNIEnv *env, 
							    jclass cls,
							    jobject sc,
							    jint    ty)
{
  FtkStripchartX * stripchartx =
    (FtkStripchartX *)getPointerFromHandle(env, sc);
  int type = (int) ty;
  ftk_stripchartx_append_event (stripchartx, type);
}



#ifdef __cplusplus
}
#endif
