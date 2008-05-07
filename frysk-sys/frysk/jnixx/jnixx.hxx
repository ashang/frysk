// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

/**
 * XXX: This header is included twice, first to get the declarations
 * and, second, to get the corresponding definitions.
 */

#if !defined frysk_jnixx_hxx_1
#define frysk_jnixx_hxx_1
/**
 * First pass, declare everything used by generated code.
 */

#include <jni.h>
#include <stdarg.h>

namespace jnixx {
  /**
   * JNIXX wrapper for the JNIEnv.
   */
  struct env;
  /**
   * An exception to throw when JNI makes an exception pending, caught
   * by the JNI wrapper stub.
   */
  struct exception {
  };
  /**
   * The JNIXX root, wraps the jobject pointer, all generated object
   * wrappers extend this.
   */
  struct object {
    jobject _object;
    object(jobject _object) {
      this->_object = _object;
    }
    inline bool operator==(jobject o) {
      return _object == o;
    }
  };
  /**
   * The JNIXX array root, any array object extends this (which
   * extends jnixx::object).
   */
  struct objectArray : public object {
    objectArray(jobject _object) : object(_object) {
    }
  };
}

#elif !defined frysk_jnixx_hxx_2
#define frysk_jnixx_hxx_2
/**
 * Second pass, define everything declared above.
 */

namespace java {
  namespace lang {
    struct String;
    struct Object;
    struct Class;
  }
};

class jnixx::env {

private:
  JNIEnv* jniEnv;

public:
  env(JNIEnv* jniEnv) {
    this->jniEnv = jniEnv;
  }

  jclass findClass(const char signature[]) {
    jclass klass = jniEnv->FindClass(signature);
    if (klass == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.FindClass(\"%s\") failed\n",
	      signature);
      throw jnixx::exception();
    }
    return klass;
  }

  java::lang::String newStringUTF(const char string[]) {
    jstring utf = jniEnv->NewStringUTF(string);
    if (utf == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.NewStringUTF(\"%s\") failed\n",
	      string);
      throw jnixx::exception();
    }
    return java::lang::String(utf);
  }

  const char* getStringUTFChars(java::lang::String string, jboolean* isCopy) {
    const char* chars = jniEnv->GetStringUTFChars((jstring) string._object,
						  isCopy);
    if (chars == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetStringUTFChars(%p,%s) failed\n",
	      string._object,
	      isCopy == NULL ? "<null>" : (*isCopy ? "<true>" : "<false>"));
      throw jnixx::exception();
    }
    return chars;
  }

  void releaseStringUTFChars(java::lang::String string, const char* chars) {
    jniEnv->ReleaseStringUTFChars((jstring) string._object,
				  chars);
    if (jniEnv->ExceptionCheck()) {
      fprintf(stderr,
	      "frysk: JNIEnv.ReleaseStringUTFChars(%p,%s) failed\n",
	      string._object, chars);
      throw jnixx::exception();
    }
  }

  jmethodID getMethodID(jclass klass, const char name[],
			const char signature[]) {
    jmethodID methodID = jniEnv->GetMethodID(klass, name, signature);
    if (methodID == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetMethodID(%p,\"%s\",\"%s\") failed\n",
	      klass, name, signature);
      throw jnixx::exception();
    }
    return methodID;
  }
  jmethodID getStaticMethodID(jclass klass, const char name[],
			      const char signature[]) {
    jmethodID methodID = jniEnv->GetStaticMethodID(klass, name, signature);
    if (methodID == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetStaticMethodID(%p,\"%s\",\"%s\") failed\n",
	      klass, name, signature);
      throw jnixx::exception();
    }
    return methodID;
  }

  jfieldID getFieldID(jclass klass, const char name[],
		      const char signature[]) {
    jfieldID fieldID = jniEnv->GetFieldID(klass, name, signature);
    if (fieldID == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetFieldID(%p,\"%s\",\"%s\") failed\n",
	      klass, name, signature);
      throw jnixx::exception();
    }
    return fieldID;
  }
  jfieldID getStaticFieldID(jclass klass, const char name[],
			    const char signature[]) {
    jfieldID fieldID = jniEnv->GetStaticFieldID(klass, name, signature);
    if (fieldID == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetStaticFieldID(%p,\"%s\",\"%s\") failed\n",
	      klass, name, signature);
      throw jnixx::exception();
    }
    return fieldID;
  }

  jobject newObject(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jobject object = jniEnv->NewObjectV(klass, id, ap);
    va_end(ap);
    if (object == NULL) {
      throw jnixx::exception();
    }
    return object;
  }

  jobject getStaticObjectField(jclass klass, jfieldID id) {
    jobject tmp = jniEnv->GetStaticObjectField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean getStaticBooleanField(jclass klass, jfieldID id) {
    jboolean tmp = jniEnv->GetStaticBooleanField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte getStaticByteField(jclass klass, jfieldID id) {
    jbyte tmp = jniEnv->GetStaticByteField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar getStaticCharField(jclass klass, jfieldID id) {
    jchar tmp = jniEnv->GetStaticCharField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort getStaticShortField(jclass klass, jfieldID id) {
    jshort tmp = jniEnv->GetStaticShortField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint getStaticIntField(jclass klass, jfieldID id) {
    jint tmp = jniEnv->GetStaticIntField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong getStaticLongField(jclass klass, jfieldID id) {
    jlong tmp = jniEnv->GetStaticLongField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat getStaticFloatField(jclass klass, jfieldID id) {
    jfloat tmp = jniEnv->GetStaticFloatField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble getStaticDoubleField(jclass klass, jfieldID id) {
    jdouble tmp = jniEnv->GetStaticDoubleField(klass, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void setStaticObjectField(jclass klass, jfieldID id, jobject value) {
    jniEnv->SetStaticObjectField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticBooleanField(jclass klass, jfieldID id, jboolean value) {
    jniEnv->SetStaticBooleanField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticByteField(jclass klass, jfieldID id, jbyte value) {
    jniEnv->SetStaticByteField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticCharField(jclass klass, jfieldID id, jchar value) {
    jniEnv->SetStaticCharField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticShortField(jclass klass, jfieldID id, jshort value) {
    jniEnv->SetStaticShortField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticIntField(jclass klass, jfieldID id, jint value) {
    jniEnv->SetStaticIntField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticLongField(jclass klass, jfieldID id, jlong value) {
    jniEnv->SetStaticLongField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticFloatField(jclass klass, jfieldID id, jfloat value) {
    jniEnv->SetStaticFloatField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticDoubleField(jclass klass, jfieldID id, jdouble value) {
    jniEnv->SetStaticDoubleField(klass, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }

  jobject getObjectField(jobject object, jfieldID id) {
    jobject tmp = jniEnv->GetObjectField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean getBooleanField(jobject object, jfieldID id) {
    jboolean tmp = jniEnv->GetBooleanField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte getByteField(jobject object, jfieldID id) {
    jbyte tmp = jniEnv->GetByteField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar getCharField(jobject object, jfieldID id) {
    jchar tmp = jniEnv->GetCharField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort getShortField(jobject object, jfieldID id) {
    jshort tmp = jniEnv->GetShortField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint getIntField(jobject object, jfieldID id) {
    jint tmp = jniEnv->GetIntField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong getLongField(jobject object, jfieldID id) {
    jlong tmp = jniEnv->GetLongField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat getFloatField(jobject object, jfieldID id) {
    jfloat tmp = jniEnv->GetFloatField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble getDoubleField(jobject object, jfieldID id) {
    jdouble tmp = jniEnv->GetDoubleField(object, id);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void setObjectField(jobject object, jfieldID id, jobject value) {
    jniEnv->SetObjectField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setBooleanField(jobject object, jfieldID id, jboolean value) {
    jniEnv->SetBooleanField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setByteField(jobject object, jfieldID id, jbyte value) {
    jniEnv->SetByteField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setCharField(jobject object, jfieldID id, jchar value) {
    jniEnv->SetCharField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setShortField(jobject object, jfieldID id, jshort value) {
    jniEnv->SetShortField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setIntField(jobject object, jfieldID id, jint value) {
    jniEnv->SetIntField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setLongField(jobject object, jfieldID id, jlong value) {
    jniEnv->SetLongField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setFloatField(jobject object, jfieldID id, jfloat value) {
    jniEnv->SetFloatField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  void setDoubleField(jobject object, jfieldID id, jdouble value) {
    jniEnv->SetDoubleField(object, id, value);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }

  jobjectArray newObjectArray(jsize length, jclass elementType,
			 jobject initialElement) {
    jobjectArray tmp = jniEnv->NewObjectArray(length, elementType, initialElement);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbooleanArray newBooleanArray(jsize length) {
    jbooleanArray tmp = jniEnv->NewBooleanArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbyteArray newByteArray(jsize length) {
    jbyteArray tmp = jniEnv->NewByteArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jcharArray newCharArray(jsize length) {
    jcharArray tmp = jniEnv->NewCharArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jshortArray newShortArray(jsize length) {
    jshortArray tmp = jniEnv->NewShortArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jintArray newIntArray(jsize length) {
    jintArray tmp = jniEnv->NewIntArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jlongArray newLongArray(jsize length) {
    jlongArray tmp = jniEnv->NewLongArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jfloatArray newFloatArray(jsize length) {
    jfloatArray tmp = jniEnv->NewFloatArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jdoubleArray newDoubleArray(jsize length) {
    jdoubleArray tmp = jniEnv->NewDoubleArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }

  jsize getArrayLength(jarray array) {
    return jniEnv->GetArrayLength(array);
  }

  jboolean* getBooleanArrayElements(jbooleanArray array, jboolean *isCopy) {
    jboolean* tmp = jniEnv->GetBooleanArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbyte* getByteArrayElements(jbyteArray array, jboolean *isCopy) {
    jbyte* tmp = jniEnv->GetByteArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jchar* getCharArrayElements(jcharArray array, jboolean *isCopy) {
    jchar* tmp = jniEnv->GetCharArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jshort* getShortArrayElements(jshortArray array, jboolean *isCopy) {
    jshort* tmp = jniEnv->GetShortArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jint* getIntArrayElements(jintArray array, jboolean *isCopy) {
    jint* tmp = jniEnv->GetIntArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jlong* getLongArrayElements(jlongArray array, jboolean *isCopy) {
    jlong* tmp = jniEnv->GetLongArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jfloat* getFloatArrayElements(jfloatArray array, jboolean *isCopy) {
    jfloat* tmp = jniEnv->GetFloatArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jdouble* getDoubleArrayElements(jdoubleArray array, jboolean *isCopy) {
    jdouble* tmp = jniEnv->GetDoubleArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }

  void releaseBooleanArrayElements(jbooleanArray array, jboolean* elements, jint mode) {
    jniEnv->ReleaseBooleanArrayElements(array, elements, mode);
  }
  void releaseByteArrayElements(jbyteArray array, jbyte* elements, jint mode) {
    jniEnv->ReleaseByteArrayElements(array, elements, mode);
  }
  void releaseCharArrayElements(jcharArray array, jchar* elements, jint mode) {
    jniEnv->ReleaseCharArrayElements(array, elements, mode);
  }
  void releaseShortArrayElements(jshortArray array, jshort* elements, jint mode) {
    jniEnv->ReleaseShortArrayElements(array, elements, mode);
  }
  void releaseIntArrayElements(jintArray array, jint* elements, jint mode) {
    jniEnv->ReleaseIntArrayElements(array, elements, mode);
  }
  void releaseLongArrayElements(jlongArray array, jlong* elements, jint mode) {
    jniEnv->ReleaseLongArrayElements(array, elements, mode);
  }
  void releaseFloatArrayElements(jfloatArray array, jfloat* elements, jint mode) {
    jniEnv->ReleaseFloatArrayElements(array, elements, mode);
  }
  void releaseDoubleArrayElements(jdoubleArray array, jdouble* elements, jint mode) {
    jniEnv->ReleaseDoubleArrayElements(array, elements, mode);
  }

  void callVoidMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jniEnv->CallObjectMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  jobject callObjectMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jobject tmp = jniEnv->CallObjectMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean callBooleanMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jboolean tmp = jniEnv->CallBooleanMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte callByteMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jbyte tmp = jniEnv->CallByteMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar callCharMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jchar tmp = jniEnv->CallCharMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort callShortMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jshort tmp = jniEnv->CallShortMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint callIntMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jint tmp = jniEnv->CallIntMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong callLongMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jlong tmp = jniEnv->CallLongMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat callFloatMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jfloat tmp = jniEnv->CallFloatMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble callDoubleMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jdouble tmp = jniEnv->CallDoubleMethodV(object, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void callStaticVoidMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jniEnv->CallStaticObjectMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
  }
  jobject callStaticObjectMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jobject tmp = jniEnv->CallStaticObjectMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean callStaticBooleanMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jboolean tmp = jniEnv->CallStaticBooleanMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte callStaticByteMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jbyte tmp = jniEnv->CallStaticByteMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar callStaticCharMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jchar tmp = jniEnv->CallStaticCharMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort callStaticShortMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jshort tmp = jniEnv->CallStaticShortMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint callStaticIntMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jint tmp = jniEnv->CallStaticIntMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong callStaticLongMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jlong tmp = jniEnv->CallStaticLongMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat callStaticFloatMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jfloat tmp = jniEnv->CallStaticFloatMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble callStaticDoubleMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jdouble tmp = jniEnv->CallStaticDoubleMethodV(klass, id, ap);
    va_end(ap);
    if (jniEnv->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void throwNew(jclass klass, const char msg[]) __attribute__((noreturn)) {
    jniEnv->ThrowNew(klass, msg);
    throw jnixx::exception();
  }

};

#endif
