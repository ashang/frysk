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
 
JNIEXPORT void JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1resize (JNIEnv *env, 
				                     jclass cls,
						     jobject sc,
						     jint    wd,
						     jint    hg)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint width  = (gint) wd;
  gint height = (gint) hg;
  ftk_eventviewer_resize (eventviewer, width, height);
}

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
  gint red   = (gint) rj;
  gint green = (gint) gj;
  gint blue  = (gint) bj;
  return (jboolean) ftk_eventviewer_set_bg_rgb (eventviewer, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_bg_color
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1bg_1color (JNIEnv *env, 
							jclass cls,
							jobject sc,
							jobject clr)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  
  GdkColor *color = (GdkColor*)getPointerFromHandle(env, clr);
 return (jboolean) ftk_eventviewer_set_bg_color (eventviewer, color);
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
							      jstring lb,
							      jstring ds)
{
  jint rc;
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gchar * label =
     (gchar *)(*env)->GetStringUTFChars(env, lb, NULL);
  gchar * desc =
     (gchar *)(*env)->GetStringUTFChars(env, ds, NULL);
  rc = (jint)ftk_eventviewer_add_trace (eventviewer, label, desc);
  (*env)->ReleaseStringUTFChars(env, lb, label);
  (*env)->ReleaseStringUTFChars(env, ds, desc);

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
  gint trace = (gint) tr;
  gint red   = (gint) rj;
  gint green = (gint) gj;
  gint blue  = (gint) bj;
  return (jboolean)ftk_eventviewer_set_trace_rgb (eventviewer,
						  trace, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_trace_color
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1trace_1color (JNIEnv *env, 
								   jclass cls,
								   jobject sc,
								   jint tr,
								   jobject clr)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint trace = (gint) tr;
  
  GdkColor *color = (GdkColor *)getPointerFromHandle(env, clr);
  return (jboolean)ftk_eventviewer_set_trace_color (eventviewer,
						  trace, color);
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
  gint trace = (gint) tr;
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
  gint trace = (gint) tr;
  gint linewidth   = (gint) lw;
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
							       gint gl,
							       jstring lb,
							       jstring ds)
{
  jint rc;
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  
  FtkGlyph glyph = (FtkGlyph)gl;
  gchar * label =
     (gchar *)(*env)->GetStringUTFChars(env, lb, NULL);
  gchar * desc =
     (gchar *)(*env)->GetStringUTFChars(env, ds, NULL);
  rc = (jint)ftk_eventviewer_marker_new (eventviewer, glyph, label, desc);
  (*env)->ReleaseStringUTFChars(env, lb, label);
  (*env)->ReleaseStringUTFChars(env, ds, desc);

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
  gint marker = (gint) mk;
  gint red   = (gint) rj;
  gint green = (gint) gj;
  gint blue  = (gint) bj;
  return (jboolean)ftk_eventviewer_set_marker_rgb (eventviewer,
						   marker, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_marker_color
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1marker_1color (JNIEnv *env, 
								    jclass cls,
								    jobject sc,
								    jint mk,
								    jobject clr)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint marker = (gint) mk;
  
  GdkColor * color = (GdkColor *)getPointerFromHandle(env, clr);
  return (jboolean)ftk_eventviewer_set_marker_color (eventviewer,
						   marker, color);
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
								 jint    mk,
								 jstring ds)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint trace = (gint) tr;
  gint marker = (gint) mk;
  gchar * desc =
     (gchar *)(*env)->GetStringUTFChars(env, ds, NULL);
  ftk_eventviewer_append_event (eventviewer, trace, marker, desc);
  (*env)->ReleaseStringUTFChars(env, ds, desc);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_tie_new
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1tie_1new (JNIEnv *env, 
							       jclass cls,
							       jobject sc
							       )
{
  
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  return ftk_eventviewer_tie_new(eventviewer);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_tie_rgb
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1tie_1rgb (JNIEnv *env, 
								   jclass cls,
								   jobject sc,
								   jint tr,
								   jint rj,
								   jint gj,
								   jint bj)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint trace = (gint) tr;
  gint red   = (gint) rj;
  gint green = (gint) gj;
  gint blue  = (gint) bj;
  return (jboolean)ftk_eventviewer_set_tie_rgb (eventviewer,
						  trace, red, green, blue);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_tie_color
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1tie_1color (JNIEnv *env, 
								   jclass cls,
								   jobject sc,
								   jint tr,
								   jobject clr)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint trace = (gint) tr;
  
  GdkColor *color = (GdkColor *)getPointerFromHandle(env, clr);
  return (jboolean)ftk_eventviewer_set_tie_color (eventviewer,
						  trace, color);
}

/*
 * Class:     com.redhat.ftk.EventViewer
 * Method:    ftk_eventviewer_set_tie_linestyle
 */

JNIEXPORT jboolean JNICALL
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1set_1tie_1linestyle (JNIEnv *env, 
									 jclass cls,
									 jobject sc,
									 jint tr,
									 jint lw,
									 jint ls)
{
  FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
  gint trace = (gint) tr;
  gint linewidth   = (gint) lw;
  GdkLineStyle linestyle = (GdkLineStyle) ls;
  return (jboolean)ftk_eventviewer_set_tie_linestyle (eventviewer,trace,
							linewidth, linestyle);
}							

/*
 * Class:	com.redhat.ftk.EventViewer
 * Method:	ftk_eventviewer_set_tie_linestyle
 */
 
JNIEXPORT jboolean JNICALL 
Java_com_redhat_ftk_EventViewer_ftk_1eventviewer_1append_1simultaneous_1events_1array 
(JNIEnv *env, jclass cls, jobject sc, jint ti, jint size, jobjectArray jevents)
{
	FtkEventViewer * eventviewer =
    (FtkEventViewer *)getPointerFromHandle(env, sc);
      
   	jclass SimEventClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env, jevents, 0));
    
    jfieldID traceFID = (*env)->GetFieldID(env, SimEventClass, "trace", "I");
    jfieldID markerFID = (*env)->GetFieldID(env, SimEventClass, "marker", "I");
    jfieldID stringFID = (*env)->GetFieldID(env, SimEventClass, "string", "Ljava/lang/String");
    
    gint tie = (gint) ti;
    gint arrayCount = (gint) size;
    ftk_simultaneous_events_s events[arrayCount];
    
    for (jint i = 0; i < size; i++) 
    {
    jobject event = (*env)->GetObjectArrayElement(env, jevents, i);
    jint trace = (*env)->GetIntField(env, event, traceFID);
    jint marker = (*env)->GetIntField(env, event, markerFID);
    jstring string = (*env)->GetObjectField(env, event, stringFID);
    gchar *str = (gchar *) (*env)->GetStringUTFChars(env, string, NULL);
    events[i].trace = (gint) trace;
    events[i].marker = (gint) marker;
    events[i].string = str;
    (*env)->ReleaseStringUTFChars(env, string,str);
    
    }
    
    return (jboolean)ftk_eventviewer_append_simultaneous_event_array(eventviewer, 
    tie, arrayCount, events);
}
 
 
 
#ifdef __cplusplus
}
#endif
