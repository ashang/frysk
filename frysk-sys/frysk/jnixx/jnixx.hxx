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

struct jnixx::env {

  JNIEnv* _jni;

  env(JNIEnv* _jni) {
    this->_jni = _jni;
  }

  jclass findClass(const char signature[]) {
    jclass klass = _jni->FindClass(signature);
    if (klass == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.FindClass(\"%s\") failed\n",
	      signature);
      throw jnixx::exception();
    }
    return klass;
  }

  java::lang::String newStringUTF(const char string[]) {
    jstring utf = _jni->NewStringUTF(string);
    if (utf == NULL) {
      fprintf(stderr,
	      "frysk: JNIEnv.NewStringUTF(\"%s\") failed\n",
	      string);
      throw jnixx::exception();
    }
    return java::lang::String(utf);
  }

  const char* getStringUTFChars(java::lang::String string, jboolean* isCopy) {
    const char* chars = _jni->GetStringUTFChars((jstring) string._object,
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
    _jni->ReleaseStringUTFChars((jstring) string._object,
				  chars);
    if (_jni->ExceptionCheck()) {
      fprintf(stderr,
	      "frysk: JNIEnv.ReleaseStringUTFChars(%p,%s) failed\n",
	      string._object, chars);
      throw jnixx::exception();
    }
  }

  jint getStringUTFLength(java::lang::String string) {
    jint length = _jni->GetStringUTFLength((jstring) (string._object));
    // Cannot fail?
    return length;
  }

  void getStringUTFRegion(java::lang::String string, jsize start,
			  jsize len, char *buf) {
    _jni->GetStringUTFRegion((jstring) (string._object), start, len, buf);
    if (_jni->ExceptionCheck()) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetStringUTFRegion(%p,%d,%d,%p) failed\n",
	      string._object, (int) start, (int) len, buf);
      throw jnixx::exception();
    }
  }

  jint getStringLength(java::lang::String string) {
    jint length = _jni->GetStringLength((jstring) (string._object));
    // Cannot fail?
    return length;
  }

  void getStringRegion(java::lang::String string, jsize start,
			  jsize len, jchar *buf) {
    _jni->GetStringRegion((jstring) (string._object), start, len, buf);
    if (_jni->ExceptionCheck()) {
      fprintf(stderr,
	      "frysk: JNIEnv.GetStringRegion(%p,%d,%d,%p) failed\n",
	      string._object, (int) start, (int) len, buf);
      throw jnixx::exception();
    }
  }

  jmethodID getMethodID(jclass klass, const char name[],
			const char signature[]) {
    jmethodID methodID = _jni->GetMethodID(klass, name, signature);
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
    jmethodID methodID = _jni->GetStaticMethodID(klass, name, signature);
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
    jfieldID fieldID = _jni->GetFieldID(klass, name, signature);
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
    jfieldID fieldID = _jni->GetStaticFieldID(klass, name, signature);
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
    jobject object = _jni->NewObjectV(klass, id, ap);
    va_end(ap);
    if (object == NULL) {
      throw jnixx::exception();
    }
    return object;
  }

  jobject getStaticObjectField(jclass klass, jfieldID id) {
    jobject tmp = _jni->GetStaticObjectField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean getStaticBooleanField(jclass klass, jfieldID id) {
    jboolean tmp = _jni->GetStaticBooleanField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte getStaticByteField(jclass klass, jfieldID id) {
    jbyte tmp = _jni->GetStaticByteField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar getStaticCharField(jclass klass, jfieldID id) {
    jchar tmp = _jni->GetStaticCharField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort getStaticShortField(jclass klass, jfieldID id) {
    jshort tmp = _jni->GetStaticShortField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint getStaticIntField(jclass klass, jfieldID id) {
    jint tmp = _jni->GetStaticIntField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong getStaticLongField(jclass klass, jfieldID id) {
    jlong tmp = _jni->GetStaticLongField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat getStaticFloatField(jclass klass, jfieldID id) {
    jfloat tmp = _jni->GetStaticFloatField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble getStaticDoubleField(jclass klass, jfieldID id) {
    jdouble tmp = _jni->GetStaticDoubleField(klass, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void setStaticObjectField(jclass klass, jfieldID id, jobject value) {
    _jni->SetStaticObjectField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticBooleanField(jclass klass, jfieldID id, jboolean value) {
    _jni->SetStaticBooleanField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticByteField(jclass klass, jfieldID id, jbyte value) {
    _jni->SetStaticByteField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticCharField(jclass klass, jfieldID id, jchar value) {
    _jni->SetStaticCharField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticShortField(jclass klass, jfieldID id, jshort value) {
    _jni->SetStaticShortField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticIntField(jclass klass, jfieldID id, jint value) {
    _jni->SetStaticIntField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticLongField(jclass klass, jfieldID id, jlong value) {
    _jni->SetStaticLongField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticFloatField(jclass klass, jfieldID id, jfloat value) {
    _jni->SetStaticFloatField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setStaticDoubleField(jclass klass, jfieldID id, jdouble value) {
    _jni->SetStaticDoubleField(klass, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }

  jobject getObjectField(jobject object, jfieldID id) {
    jobject tmp = _jni->GetObjectField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean getBooleanField(jobject object, jfieldID id) {
    jboolean tmp = _jni->GetBooleanField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte getByteField(jobject object, jfieldID id) {
    jbyte tmp = _jni->GetByteField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar getCharField(jobject object, jfieldID id) {
    jchar tmp = _jni->GetCharField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort getShortField(jobject object, jfieldID id) {
    jshort tmp = _jni->GetShortField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint getIntField(jobject object, jfieldID id) {
    jint tmp = _jni->GetIntField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong getLongField(jobject object, jfieldID id) {
    jlong tmp = _jni->GetLongField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat getFloatField(jobject object, jfieldID id) {
    jfloat tmp = _jni->GetFloatField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble getDoubleField(jobject object, jfieldID id) {
    jdouble tmp = _jni->GetDoubleField(object, id);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void setObjectField(jobject object, jfieldID id, jobject value) {
    _jni->SetObjectField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setBooleanField(jobject object, jfieldID id, jboolean value) {
    _jni->SetBooleanField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setByteField(jobject object, jfieldID id, jbyte value) {
    _jni->SetByteField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setCharField(jobject object, jfieldID id, jchar value) {
    _jni->SetCharField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setShortField(jobject object, jfieldID id, jshort value) {
    _jni->SetShortField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setIntField(jobject object, jfieldID id, jint value) {
    _jni->SetIntField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setLongField(jobject object, jfieldID id, jlong value) {
    _jni->SetLongField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setFloatField(jobject object, jfieldID id, jfloat value) {
    _jni->SetFloatField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  void setDoubleField(jobject object, jfieldID id, jdouble value) {
    _jni->SetDoubleField(object, id, value);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }

  jobjectArray newObjectArray(jsize length, jclass elementType,
			 jobject initialElement) {
    jobjectArray tmp = _jni->NewObjectArray(length, elementType, initialElement);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbooleanArray newBooleanArray(jsize length) {
    jbooleanArray tmp = _jni->NewBooleanArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbyteArray newByteArray(jsize length) {
    jbyteArray tmp = _jni->NewByteArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jcharArray newCharArray(jsize length) {
    jcharArray tmp = _jni->NewCharArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jshortArray newShortArray(jsize length) {
    jshortArray tmp = _jni->NewShortArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jintArray newIntArray(jsize length) {
    jintArray tmp = _jni->NewIntArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jlongArray newLongArray(jsize length) {
    jlongArray tmp = _jni->NewLongArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jfloatArray newFloatArray(jsize length) {
    jfloatArray tmp = _jni->NewFloatArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jdoubleArray newDoubleArray(jsize length) {
    jdoubleArray tmp = _jni->NewDoubleArray(length);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }

  jsize getArrayLength(jarray array) {
    return _jni->GetArrayLength(array);
  }

  jboolean* getBooleanArrayElements(jbooleanArray array, jboolean *isCopy) {
    jboolean* tmp = _jni->GetBooleanArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jbyte* getByteArrayElements(jbyteArray array, jboolean *isCopy) {
    jbyte* tmp = _jni->GetByteArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jchar* getCharArrayElements(jcharArray array, jboolean *isCopy) {
    jchar* tmp = _jni->GetCharArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jshort* getShortArrayElements(jshortArray array, jboolean *isCopy) {
    jshort* tmp = _jni->GetShortArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jint* getIntArrayElements(jintArray array, jboolean *isCopy) {
    jint* tmp = _jni->GetIntArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jlong* getLongArrayElements(jlongArray array, jboolean *isCopy) {
    jlong* tmp = _jni->GetLongArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jfloat* getFloatArrayElements(jfloatArray array, jboolean *isCopy) {
    jfloat* tmp = _jni->GetFloatArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }
  jdouble* getDoubleArrayElements(jdoubleArray array, jboolean *isCopy) {
    jdouble* tmp = _jni->GetDoubleArrayElements(array, isCopy);
    if (tmp == NULL)
      throw jnixx::exception();
    return tmp;
  }

  void releaseBooleanArrayElements(jbooleanArray array, jboolean* elements, jint mode) {
    _jni->ReleaseBooleanArrayElements(array, elements, mode);
  }
  void releaseByteArrayElements(jbyteArray array, jbyte* elements, jint mode) {
    _jni->ReleaseByteArrayElements(array, elements, mode);
  }
  void releaseCharArrayElements(jcharArray array, jchar* elements, jint mode) {
    _jni->ReleaseCharArrayElements(array, elements, mode);
  }
  void releaseShortArrayElements(jshortArray array, jshort* elements, jint mode) {
    _jni->ReleaseShortArrayElements(array, elements, mode);
  }
  void releaseIntArrayElements(jintArray array, jint* elements, jint mode) {
    _jni->ReleaseIntArrayElements(array, elements, mode);
  }
  void releaseLongArrayElements(jlongArray array, jlong* elements, jint mode) {
    _jni->ReleaseLongArrayElements(array, elements, mode);
  }
  void releaseFloatArrayElements(jfloatArray array, jfloat* elements, jint mode) {
    _jni->ReleaseFloatArrayElements(array, elements, mode);
  }
  void releaseDoubleArrayElements(jdoubleArray array, jdouble* elements, jint mode) {
    _jni->ReleaseDoubleArrayElements(array, elements, mode);
  }

  void callVoidMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    _jni->CallObjectMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  jobject callObjectMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jobject tmp = _jni->CallObjectMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean callBooleanMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jboolean tmp = _jni->CallBooleanMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte callByteMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jbyte tmp = _jni->CallByteMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar callCharMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jchar tmp = _jni->CallCharMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort callShortMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jshort tmp = _jni->CallShortMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint callIntMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jint tmp = _jni->CallIntMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong callLongMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jlong tmp = _jni->CallLongMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat callFloatMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jfloat tmp = _jni->CallFloatMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble callDoubleMethod(jobject object, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jdouble tmp = _jni->CallDoubleMethodV(object, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void callStaticVoidMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    _jni->CallStaticObjectMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
  }
  jobject callStaticObjectMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jobject tmp = _jni->CallStaticObjectMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jboolean callStaticBooleanMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jboolean tmp = _jni->CallStaticBooleanMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jbyte callStaticByteMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jbyte tmp = _jni->CallStaticByteMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jchar callStaticCharMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jchar tmp = _jni->CallStaticCharMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jshort callStaticShortMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jshort tmp = _jni->CallStaticShortMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jint callStaticIntMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jint tmp = _jni->CallStaticIntMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jlong callStaticLongMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jlong tmp = _jni->CallStaticLongMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jfloat callStaticFloatMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jfloat tmp = _jni->CallStaticFloatMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }
  jdouble callStaticDoubleMethod(jclass klass, jmethodID id, ...) {
    va_list ap;
    va_start(ap, id);
    jdouble tmp = _jni->CallStaticDoubleMethodV(klass, id, ap);
    va_end(ap);
    if (_jni->ExceptionCheck())
      throw jnixx::exception();
    return tmp;
  }

  void throwNew(jclass klass, const char msg[]) __attribute__((noreturn)) {
    _jni->ThrowNew(klass, msg);
    throw jnixx::exception();
  }

};

#endif
