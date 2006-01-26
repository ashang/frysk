
#ifndef __org_gnu_gtk_frysk_EggTrayIcon__
#define __org_gnu_gtk_frysk_EggTrayIcon__

#include <jni.h>
#include <jg_jnu.h>
#include "eggtrayicon.h"
#include <gtk_java.h>

JNIEXPORT jint JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1get_1type (JNIEnv *env, jclass c){
  return (jint)egg_tray_icon_get_type();
}

JNIEXPORT jobject JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1new_1for_1screen (JNIEnv *env, jclass c, jobject screen, jstring name){
  return getGObjectHandle(env, G_OBJECT (egg_tray_icon_new_for_screen(screen,name)));
}

JNIEXPORT jobject JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1new (JNIEnv *env, jclass c, jstring name){
  return getGObjectHandle(env,G_OBJECT (egg_tray_icon_new(name)));
}

JNIEXPORT jint JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1send_1message (JNIEnv *env, jclass c, jobject icon, jint timeout, jstring message, jint len){
  return (jint)egg_tray_icon_send_message(icon,timeout,message,len);
}

JNIEXPORT void JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1cancel_1message (JNIEnv *env, jclass c, jobject icon, jint id){
  egg_tray_icon_cancel_message (icon,id);
  
}

JNIEXPORT jint JNICALL Java_org_gnu_gtk_frysk_EggTrayIcon_egg_1tray_1icon_1get_1orientation (JNIEnv *env, jclass c, jobject icon){
  return (jint)egg_tray_icon_get_orientation (icon);
}

#endif /* __org_gnu_gtk_frysk_EggTrayIcon__ */
