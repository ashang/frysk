#include <jni.h>
#include "openpty_frysk.h"
#include "jg_jnu.h"

#ifdef __cplusplus
extern "C" 
{
#endif

/*
 * Class:     com.redhat.ftk.OpenPty
 * Method:    ftk_eventviewer_get_type
 */
JNIEXPORT jint JNICALL
Java_com_redhat_ftk_OpenPty_openpty_1frysk (JNIEnv *env, 
					    jclass cls) 
{
    return (jint)openpty_frysk ();
}

/*
 * Class:     com.redhat.ftk.OpenPty
 * Method:    ftk_eventviewer_get_type
 */
JNIEXPORT jstring JNICALL
Java_com_redhat_ftk_OpenPty_ptsname_1frysk (JNIEnv *env, 
					    jclass cls,
					    jint master) 
{
    char * name = ptsname_frysk ((int)master);
    return (*env)->NewStringUTF(env, name);
}


#ifdef __cplusplus
}
#endif
