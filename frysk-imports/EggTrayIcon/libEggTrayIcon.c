
#ifndef __frysk_gui_monitor_EggTrayIcon__
#define __frysk_gui_monitor_EggTrayIcon__

#include <jni.h>
#include <jg_jnu.h>
#ifdef __cplusplus
extern "C"
{
#endif


JNIEXPORT jint JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1get_1type (JNIEnv *env, jclass c){
  return (jint)egg_tray_icon_get_type();
}

JNIEXPORT jobject JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1new_1for_1screen (JNIEnv *env, jclass c, jobject screen, jstring name){
  return getGObjectHandle(env,egg_tray_icon_new_for_screen(screen,name));
}

JNIEXPORT jobject JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1new (JNIEnv *env, jclass c, jstring name){
  return getGObjectHandle(env,egg_tray_icon_new(name));
}

JNIEXPORT jint JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1send_1message (JNIEnv *env, jclass c, jobject icon, jint timeout, jstring message, jint len){
  return (jint)egg_tray_icon_send_message(icon,timeout,message,len);
}

JNIEXPORT void JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1cancel_1message (JNIEnv *env, jclass c, jobject icon, jint id){
  egg_tray_icon_cancel_message (icon,id);
  
}

JNIEXPORT jint JNICALL Java_frysk_gui_monitor_EggTrayIcon_egg_1tray_1icon_1get_1orientation (JNIEnv *env, jclass c, jobject icon){
  return (jint)egg_tray_icon_get_orientation (icon);
}

#ifdef __cplusplus
}
#endif

#endif /* __frysk_gui_monitor_EggTrayIcon__ */
