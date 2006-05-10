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
#include "ftkeventviewer.h"
#include "jg_jnu.h"
#include "gtk_java.h"

#ifdef __cplusplus
extern "C" 
{
#endif

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_get_type
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1get_1type (JNIEnv *env, 
							     jclass cls) 
{
    return (jint)ftk_eventviewer_get_type ();
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_new
 */
JNIEXPORT jobject JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1new (JNIEnv *env, 
						       jclass cls) 
{
    return getGObjectHandle(env, (GObject *) ftk_eventviewer_new ());
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_resize
 */
/****************** not yet implemented 
JNIEXPORT void JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1resize (JNIEnv *env, 
				                     jclass cls,
						     jobject sc,
						     jint    wd,
						     jint    hg)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int width  = (int) wd;
  int height = (int) hg;
  ftk_eventviewer_resize (eventviewer, width, height);
}
*******************/

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_bg_rgb
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1bg_1rgb (JNIEnv *env, 
								jclass cls,
								jobject sc,
								jint rj,
								jint gj,
								jint bj)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  return (jboolean) ftk_eventviewer_set_bg_rgb (eventviewer, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_timebase
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1timebase (JNIEnv *env, 
								 jclass cls,
								 jobject sc,
								 jdouble sp)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  double span   = (double) sp;
  return (jboolean) ftk_eventviewer_set_timebase (eventviewer, span);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_add_trace
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1add_1trace (JNIEnv *env, 
							      jclass cls,
							      jobject sc,
							      jstring lb)
{
  jint rc;
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gchar * label =
     (gchar *)(*env)->GetStringUTFChars(env, lb, NULL);
  rc = (jint)ftk_eventviewer_add_trace (eventviewer, label);
  (*env)->ReleaseStringUTFChars(env, lb, label);

  return rc;
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_trace_rgb
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1trace_1rgb (JNIEnv *env, 
								   jclass cls,
								   jobject sc,
								   jint tr,
								   jint rj,
								   jint gj,
								   jint bj)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int trace = (int) tr;
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  return (jboolean)ftk_eventviewer_set_trace_rgb (eventviewer,
						  trace, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_trace_label
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1trace_1label (JNIEnv *env, 
								     jclass cls,
								     jobject sc,
								     jint tr,
								     jstring lb)
{
  jboolean rc;
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int trace = (int) tr;
  gchar * label =
     (gchar *)(*env)->GetStringUTFChars(env, lb, NULL);
  rc = (jboolean)ftk_eventviewer_set_trace_label (eventviewer, trace, label);
  (*env)->ReleaseStringUTFChars(env, lb, label);

  return rc;
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_trace_linestyle
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1trace_1linestyle (JNIEnv *env, 
									 jclass cls,
									 jobject sc,
									 jint tr,
									 jint lw,
									 jint ls)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int trace = (int) tr;
  int linewidth   = (int) lw;
  GdkLineStyle linestyle = (GdkLineStyle) ls;
  return (jboolean)ftk_eventviewer_set_trace_linestyle (eventviewer,trace,
							linewidth, linestyle);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_marker_new
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1marker_1new (JNIEnv *env, 
							       jclass cls,
							       jobject sc,
							       int gl,
							       jstring lb)
{
  jint rc;
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  
  FtkGlyph glyph = (FtkGlyph)gl;
  gchar * label =
     (gchar *)(*env)->GetStringUTFChars(env, lb, NULL);
  rc = (jint)ftk_eventviewer_marker_new (eventviewer, glyph, label);
  (*env)->ReleaseStringUTFChars(env, lb, label);

  return rc;
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_marker_rgb
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1marker_1rgb (JNIEnv *env, 
								    jclass cls,
								    jobject sc,
								    jint mk,
								    jint rj,
								    jint gj,
								    jint bj)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int marker = (int) mk;
  int red   = (int) rj;
  int green = (int) gj;
  int blue  = (int) bj;
  return (jboolean)ftk_eventviewer_set_marker_rgb (eventviewer,
						   marker, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewr
 * Method:    ftk_eventviewer_append_event
 */
JNIEXPORT void JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1append_1event (JNIEnv *env, 
								 jclass cls,
								 jobject sc,
								 jint    tr,
								 jint    mk)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  int trace = (int) tr;
  int marker = (int) mk;
  ftk_eventviewer_append_event (eventviewer, trace, marker);
}



#ifdef __cplusplus
}
#endif
