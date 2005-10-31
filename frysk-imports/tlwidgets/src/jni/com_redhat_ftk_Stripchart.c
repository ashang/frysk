/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */

#include <jni.h>
#include <gtk/gtk.h>
#include <jg_jnu.h>
#include "gtk_java.h"

#ifdef __cplusplus
extern "C" 
{
#endif

/*
 * Class:     org.gnu.gtk.Stripchart
 * Method:    ftk_stripchart_get_type
 */
JNIEXPORT jint JNICALL Java_org_gnu_gtk_Stripchart_ftk_1stripchart_1get_1type (JNIEnv *env, 
    jclass cls) 
{
    return (jint)ftk_stripchart_get_type ();
}

/*
 * Class:     org.gnu.gtk.Stripchart
 * Method:    ftk_stripchart_new
 */
JNIEXPORT jobject JNICALL Java_org_gnu_gtk_Stripchart_ftk_1stripchart_1new (JNIEnv *env, 
    jclass cls) 
{
    return getGObjectHandle(env, (GObject *) ftk_stripchart_new ());
}


#ifdef __cplusplus
}

#endif
