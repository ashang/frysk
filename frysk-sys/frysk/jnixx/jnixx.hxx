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

#if !defined frysk_jnixx_hxx

#include <jni.h>
#include <stdarg.h>

namespace jnixx {

  /**
   * An exception to throw when JNI makes an exception pending, caught
   * by the JNI wrapper stub.
   */
  class exception {
  };

  /**
   * JNIXX wrapper for the JNIEnv; just like JNIEnv except it throws
   * an exception for any error or exception check.
   */
  class env {
  public:

    JNIEnv* _jni;
    env(JNIEnv* _jni) {
      this->_jni = _jni;
    }

    // Version Information

    jint GetVersion() {
      return _jni->GetVersion();
    }

    // Class Operations

    jclass DefineClass(const char *name, jobject loader,
			      const jbyte *buf, jsize bufLen) {
      jclass klass = _jni->DefineClass(name, loader, buf, bufLen);
      if (klass == NULL) {
	throw new exception();
      }
      return klass;
    }
    jclass FindClass(const char *signature) {
      jclass klass = _jni->FindClass(signature);
      if (klass == NULL) {
	fprintf(stderr, "%s(\"%s\") failed\n",
		__func__, signature);
	throw new exception();
      }
      return klass;
    }
    jclass GetSuperclass(jclass klass) {
      return _jni->GetSuperclass(klass);
    }
    jboolean IsAssignableFrom(jclass klass1, jclass klass2) {
      return _jni->IsAssignableFrom(klass1, klass2);
    }

    // Exceptions

    void Throw(jthrowable obj) __attribute__((noreturn)) {
      _jni->Throw(obj);
      throw exception();
    }
    void ThrowNew(jclass klass, const char* message) __attribute__((noreturn)) {
      _jni->ThrowNew(klass, message);
      throw exception();
    }
    jthrowable ExceptionOccurred() {
      return _jni->ExceptionOccurred();
    }
    void ExceptionDescribe() {
      _jni->ExceptionDescribe();
    }
    void ExceptionClear() {
      _jni->ExceptionClear();
    }
    void FatalError(const char *msg) {
      _jni->FatalError(msg);
    }
    jboolean ExceptionCheck() {
      return _jni->ExceptionCheck();
    }

    // Global and Local References

    // GLobal References

    jobject NewGlobalRef(jobject obj) {
      jobject glob = _jni->NewGlobalRef(obj);
      if (glob == NULL) {
	throw exception();
      }
      return glob;
    }
    void DeleteGlobalRef(jobject obj) {
      _jni->DeleteGlobalRef(obj);
    }

    // Local References

    void DeleteLocalRef(jobject obj) {
      _jni->DeleteLocalRef(obj);
    }
    void EnsureLocalCapacity(jint capacity) {
      if (_jni->EnsureLocalCapacity(capacity) < 0) {
	throw exception();
      }
    }
    void PushLocalFrame(jint capacity) {
      if (_jni->PushLocalFrame(capacity) < 0) {
	throw exception();
      }
    }
    jobject PopLocalFrame(jobject result) {
      return _jni->PopLocalFrame(result);
    }
    jobject NewLocalRef(jobject obj) {
      return _jni->NewLocalRef(obj);
    }

    // Weak Global References

    jweak NewWeakGlobalRef(jobject ref) {
      jweak weak = _jni->NewWeakGlobalRef(ref);
      if (weak == NULL) {
	throw exception();
      }
      return weak;
    }
    void DeleteWeakGlobalRef(jweak weak) {
      _jni->DeleteWeakGlobalRef(weak);
    }

    // Object Operations

    jobject AllocObject(jclass klass) {
      jobject object = _jni->AllocObject(klass);
      if (object == NULL) {
	throw exception();
      }
    }
    jobject NewObject(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jobject object = _jni->NewObjectV(klass, id, ap);
      va_end(ap);
      if (object == NULL) {
	throw exception();
      }
      return object;
    }
    jobject NewObjectA(jclass klass, jmethodID id, jvalue* args) {
      jobject object = _jni->NewObjectA(klass, id, args);
      if (object == NULL) {
	throw exception();
      }
    }
    jobject NewObjectV(jclass klass, jmethodID id, va_list args) {
      jobject object = _jni->NewObjectV(klass, id, args);
      if (object == NULL) {
	throw exception();
      }
    }
    jclass GetObjectClass(jobject object) {
      return _jni->GetObjectClass(object);
    }
    jboolean IsInstanceOf(jobject object, jclass klass) {
      return _jni->IsInstanceOf(object, klass);
    }
    jboolean IsSameObject(jobject ref1, jobject ref2) {
      return _jni->IsSameObject(ref1, ref2);
    }

    // Accessing Fields of Objects

    jfieldID GetFieldID(jclass klass, const char name[],
			const char signature[]) {
      jfieldID fieldID = _jni->GetFieldID(klass, name, signature);
      if (fieldID == NULL) {
	fprintf(stderr, "%s(%p,\"%s\",\"%s\") failed\n",
		__func__, klass, name, signature);
	throw exception();
      }
      return fieldID;
    }
    jobject GetObjectField(jobject object, jfieldID id) {
      jobject tmp = _jni->GetObjectField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean GetBooleanField(jobject object, jfieldID id) {
      jboolean tmp = _jni->GetBooleanField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte GetByteField(jobject object, jfieldID id) {
      jbyte tmp = _jni->GetByteField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar GetCharField(jobject object, jfieldID id) {
      jchar tmp = _jni->GetCharField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort GetShortField(jobject object, jfieldID id) {
      jshort tmp = _jni->GetShortField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint GetIntField(jobject object, jfieldID id) {
      jint tmp = _jni->GetIntField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong GetLongField(jobject object, jfieldID id) {
      jlong tmp = _jni->GetLongField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat GetFloatField(jobject object, jfieldID id) {
      jfloat tmp = _jni->GetFloatField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble GetDoubleField(jobject object, jfieldID id) {
      jdouble tmp = _jni->GetDoubleField(object, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void SetObjectField(jobject object, jfieldID id, jobject value) {
      _jni->SetObjectField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetBooleanField(jobject object, jfieldID id, jboolean value) {
      _jni->SetBooleanField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetByteField(jobject object, jfieldID id, jbyte value) {
      _jni->SetByteField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetCharField(jobject object, jfieldID id, jchar value) {
      _jni->SetCharField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetShortField(jobject object, jfieldID id, jshort value) {
      _jni->SetShortField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetIntField(jobject object, jfieldID id, jint value) {
      _jni->SetIntField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetLongField(jobject object, jfieldID id, jlong value) {
      _jni->SetLongField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetFloatField(jobject object, jfieldID id, jfloat value) {
      _jni->SetFloatField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetDoubleField(jobject object, jfieldID id, jdouble value) {
      _jni->SetDoubleField(object, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }

    // Calling Instance Methods

    jmethodID GetMethodID(jclass klass, const char name[],
			  const char signature[]) {
      jmethodID methodID = _jni->GetMethodID(klass, name, signature);
      if (methodID == NULL) {
	fprintf(stderr, "%s(%p,\"%s\",\"%s\") failed\n",
		__func__, klass, name, signature);
	throw exception();
      }
      return methodID;
    }
    void CallVoidMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      _jni->CallObjectMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallObjectMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jobject tmp = _jni->CallObjectMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallBooleanMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jboolean tmp = _jni->CallBooleanMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallByteMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jbyte tmp = _jni->CallByteMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallCharMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jchar tmp = _jni->CallCharMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallShortMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jshort tmp = _jni->CallShortMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallIntMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jint tmp = _jni->CallIntMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallLongMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jlong tmp = _jni->CallLongMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallFloatMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jfloat tmp = _jni->CallFloatMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallDoubleMethod(jobject object, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jdouble tmp = _jni->CallDoubleMethodV(object, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallVoidMethodA(jobject object, jmethodID id, jvalue* args) {
      _jni->CallObjectMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallObjectMethodA(jobject object, jmethodID id, jvalue* args) {
      jobject tmp = _jni->CallObjectMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallBooleanMethodA(jobject object, jmethodID id, jvalue* args) {
      jboolean tmp = _jni->CallBooleanMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallByteMethodA(jobject object, jmethodID id, jvalue* args) {
      jbyte tmp = _jni->CallByteMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallCharMethodA(jobject object, jmethodID id, jvalue* args) {
      jchar tmp = _jni->CallCharMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallShortMethodA(jobject object, jmethodID id, jvalue* args) {
      jshort tmp = _jni->CallShortMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallIntMethodA(jobject object, jmethodID id, jvalue* args) {
      jint tmp = _jni->CallIntMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallLongMethodA(jobject object, jmethodID id, jvalue* args) {
      jlong tmp = _jni->CallLongMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallFloatMethodA(jobject object, jmethodID id, jvalue* args) {
      jfloat tmp = _jni->CallFloatMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallDoubleMethodA(jobject object, jmethodID id, jvalue* args) {
      jdouble tmp = _jni->CallDoubleMethodA(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallVoidMethodV(jobject object, jmethodID id, va_list args) {
      _jni->CallObjectMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallObjectMethodV(jobject object, jmethodID id, va_list args) {
      jobject tmp = _jni->CallObjectMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallBooleanMethodV(jobject object, jmethodID id, va_list args) {
      jboolean tmp = _jni->CallBooleanMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallByteMethodV(jobject object, jmethodID id, va_list args) {
      jbyte tmp = _jni->CallByteMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallCharMethodV(jobject object, jmethodID id, va_list args) {
      jchar tmp = _jni->CallCharMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallShortMethodV(jobject object, jmethodID id, va_list args) {
      jshort tmp = _jni->CallShortMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallIntMethodV(jobject object, jmethodID id, va_list args) {
      jint tmp = _jni->CallIntMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallLongMethodV(jobject object, jmethodID id, va_list args) {
      jlong tmp = _jni->CallLongMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallFloatMethodV(jobject object, jmethodID id, va_list args) {
      jfloat tmp = _jni->CallFloatMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallDoubleMethodV(jobject object, jmethodID id, va_list args) {
      jdouble tmp = _jni->CallDoubleMethodV(object, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallNonvirtualVoidMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      _jni->CallNonvirtualObjectMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallNonvirtualObjectMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jobject tmp = _jni->CallNonvirtualObjectMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallNonvirtualBooleanMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jboolean tmp = _jni->CallNonvirtualBooleanMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallNonvirtualByteMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jbyte tmp = _jni->CallNonvirtualByteMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallNonvirtualCharMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jchar tmp = _jni->CallNonvirtualCharMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallNonvirtualShortMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jshort tmp = _jni->CallNonvirtualShortMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallNonvirtualIntMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jint tmp = _jni->CallNonvirtualIntMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallNonvirtualLongMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jlong tmp = _jni->CallNonvirtualLongMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallNonvirtualFloatMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jfloat tmp = _jni->CallNonvirtualFloatMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallNonvirtualDoubleMethod(jobject object, jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jdouble tmp = _jni->CallNonvirtualDoubleMethodV(object, klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallNonvirtualVoidMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      _jni->CallNonvirtualObjectMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallNonvirtualObjectMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jobject tmp = _jni->CallNonvirtualObjectMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallNonvirtualBooleanMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jboolean tmp = _jni->CallNonvirtualBooleanMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallNonvirtualByteMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jbyte tmp = _jni->CallNonvirtualByteMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallNonvirtualCharMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jchar tmp = _jni->CallNonvirtualCharMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallNonvirtualShortMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jshort tmp = _jni->CallNonvirtualShortMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallNonvirtualIntMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jint tmp = _jni->CallNonvirtualIntMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallNonvirtualLongMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jlong tmp = _jni->CallNonvirtualLongMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallNonvirtualFloatMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jfloat tmp = _jni->CallNonvirtualFloatMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallNonvirtualDoubleMethodA(jobject object, jclass klass, jmethodID id, jvalue* args) {
      jdouble tmp = _jni->CallNonvirtualDoubleMethodA(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallNonvirtualVoidMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      _jni->CallNonvirtualObjectMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallNonvirtualObjectMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jobject tmp = _jni->CallNonvirtualObjectMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallNonvirtualBooleanMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jboolean tmp = _jni->CallNonvirtualBooleanMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallNonvirtualByteMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jbyte tmp = _jni->CallNonvirtualByteMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallNonvirtualCharMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jchar tmp = _jni->CallNonvirtualCharMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallNonvirtualShortMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jshort tmp = _jni->CallNonvirtualShortMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallNonvirtualIntMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jint tmp = _jni->CallNonvirtualIntMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallNonvirtualLongMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jlong tmp = _jni->CallNonvirtualLongMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallNonvirtualFloatMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jfloat tmp = _jni->CallNonvirtualFloatMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallNonvirtualDoubleMethodV(jobject object, jclass klass, jmethodID id, va_list args) {
      jdouble tmp = _jni->CallNonvirtualDoubleMethodV(object, klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }

    // Accessing Static Fields

    jfieldID GetStaticFieldID(jclass klass, const char name[],
			      const char signature[]) {
      jfieldID fieldID = _jni->GetStaticFieldID(klass, name, signature);
      if (fieldID == NULL) {
	fprintf(stderr, "%s(%p,\"%s\",\"%s\") failed\n",
		__func__, klass, name, signature);
	throw exception();
      }
      return fieldID;
    }
    jobject GetStaticObjectField(jclass klass, jfieldID id) {
      jobject tmp = _jni->GetStaticObjectField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean GetStaticBooleanField(jclass klass, jfieldID id) {
      jboolean tmp = _jni->GetStaticBooleanField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte GetStaticByteField(jclass klass, jfieldID id) {
      jbyte tmp = _jni->GetStaticByteField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar GetStaticCharField(jclass klass, jfieldID id) {
      jchar tmp = _jni->GetStaticCharField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort GetStaticShortField(jclass klass, jfieldID id) {
      jshort tmp = _jni->GetStaticShortField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint GetStaticIntField(jclass klass, jfieldID id) {
      jint tmp = _jni->GetStaticIntField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong GetStaticLongField(jclass klass, jfieldID id) {
      jlong tmp = _jni->GetStaticLongField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat GetStaticFloatField(jclass klass, jfieldID id) {
      jfloat tmp = _jni->GetStaticFloatField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble GetStaticDoubleField(jclass klass, jfieldID id) {
      jdouble tmp = _jni->GetStaticDoubleField(klass, id);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void SetStaticObjectField(jclass klass, jfieldID id, jobject value) {
      _jni->SetStaticObjectField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticBooleanField(jclass klass, jfieldID id, jboolean value) {
      _jni->SetStaticBooleanField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticByteField(jclass klass, jfieldID id, jbyte value) {
      _jni->SetStaticByteField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticCharField(jclass klass, jfieldID id, jchar value) {
      _jni->SetStaticCharField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticShortField(jclass klass, jfieldID id, jshort value) {
      _jni->SetStaticShortField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticIntField(jclass klass, jfieldID id, jint value) {
      _jni->SetStaticIntField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticLongField(jclass klass, jfieldID id, jlong value) {
      _jni->SetStaticLongField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticFloatField(jclass klass, jfieldID id, jfloat value) {
      _jni->SetStaticFloatField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    void SetStaticDoubleField(jclass klass, jfieldID id, jdouble value) {
      _jni->SetStaticDoubleField(klass, id, value);
      if (_jni->ExceptionCheck())
	throw exception();
    }

    // Calling Static Methods

    jmethodID GetStaticMethodID(jclass klass, const char name[],
				const char signature[]) {
      jmethodID methodID = _jni->GetStaticMethodID(klass, name, signature);
      if (methodID == NULL) {
	fprintf(stderr, "%s(%p,\"%s\",\"%s\") failed\n",
		__func__, klass, name, signature);
	throw exception();
      }
      return methodID;
    }
    void CallStaticVoidMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      _jni->CallStaticObjectMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallStaticObjectMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jobject tmp = _jni->CallStaticObjectMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallStaticBooleanMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jboolean tmp = _jni->CallStaticBooleanMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallStaticByteMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jbyte tmp = _jni->CallStaticByteMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallStaticCharMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jchar tmp = _jni->CallStaticCharMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallStaticShortMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jshort tmp = _jni->CallStaticShortMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallStaticIntMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jint tmp = _jni->CallStaticIntMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallStaticLongMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jlong tmp = _jni->CallStaticLongMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallStaticFloatMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jfloat tmp = _jni->CallStaticFloatMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallStaticDoubleMethod(jclass klass, jmethodID id, ...) {
      va_list ap;
      va_start(ap, id);
      jdouble tmp = _jni->CallStaticDoubleMethodV(klass, id, ap);
      va_end(ap);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallStaticVoidMethodA(jclass klass, jmethodID id, jvalue* args) {
      _jni->CallStaticObjectMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallStaticObjectMethodA(jclass klass, jmethodID id, jvalue* args) {
      jobject tmp = _jni->CallStaticObjectMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallStaticBooleanMethodA(jclass klass, jmethodID id, jvalue* args) {
      jboolean tmp = _jni->CallStaticBooleanMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallStaticByteMethodA(jclass klass, jmethodID id, jvalue* args) {
      jbyte tmp = _jni->CallStaticByteMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallStaticCharMethodA(jclass klass, jmethodID id, jvalue* args) {
      jchar tmp = _jni->CallStaticCharMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallStaticShortMethodA(jclass klass, jmethodID id, jvalue* args) {
      jshort tmp = _jni->CallStaticShortMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallStaticIntMethodA(jclass klass, jmethodID id, jvalue* args) {
      jint tmp = _jni->CallStaticIntMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallStaticLongMethodA(jclass klass, jmethodID id, jvalue* args) {
      jlong tmp = _jni->CallStaticLongMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallStaticFloatMethodA(jclass klass, jmethodID id, jvalue* args) {
      jfloat tmp = _jni->CallStaticFloatMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallStaticDoubleMethodA(jclass klass, jmethodID id, jvalue* args) {
      jdouble tmp = _jni->CallStaticDoubleMethodA(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    void CallStaticVoidMethodV(jclass klass, jmethodID id, va_list args) {
      _jni->CallStaticObjectMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
    }
    jobject CallStaticObjectMethodV(jclass klass, jmethodID id, va_list args) {
      jobject tmp = _jni->CallStaticObjectMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jboolean CallStaticBooleanMethodV(jclass klass, jmethodID id, va_list args) {
      jboolean tmp = _jni->CallStaticBooleanMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jbyte CallStaticByteMethodV(jclass klass, jmethodID id, va_list args) {
      jbyte tmp = _jni->CallStaticByteMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jchar CallStaticCharMethodV(jclass klass, jmethodID id, va_list args) {
      jchar tmp = _jni->CallStaticCharMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jshort CallStaticShortMethodV(jclass klass, jmethodID id, va_list args) {
      jshort tmp = _jni->CallStaticShortMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jint CallStaticIntMethodV(jclass klass, jmethodID id, va_list args) {
      jint tmp = _jni->CallStaticIntMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jlong CallStaticLongMethodV(jclass klass, jmethodID id, va_list args) {
      jlong tmp = _jni->CallStaticLongMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jfloat CallStaticFloatMethodV(jclass klass, jmethodID id, va_list args) {
      jfloat tmp = _jni->CallStaticFloatMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }
    jdouble CallStaticDoubleMethodV(jclass klass, jmethodID id, va_list args) {
      jdouble tmp = _jni->CallStaticDoubleMethodV(klass, id, args);
      if (_jni->ExceptionCheck())
	throw exception();
      return tmp;
    }

    // String Operations

    jstring NewString(const jchar *unicodeChars, jsize size) {
      jstring string = _jni->NewString(unicodeChars, size);
      if (string == NULL) {
	throw exception();
      }
      return string;
    }
    jint GetStringLength(jstring string) {
      return _jni->GetStringLength(string);
    }
    const jchar* GetStringChars(jstring string) {
      return _jni->GetStringChars(string, NULL);
    }
    void ReleaseStringChars(jstring string, jchar* chars) {
      _jni->ReleaseStringChars(string, chars);
    }
    jstring NewStringUTF(const char string[]) {
      jstring utf = _jni->NewStringUTF(string);
      if (utf == NULL) {
	throw exception();
      }
      return utf;
    }
    jint GetStringUTFLength(jstring string) {
      return _jni->GetStringUTFLength(string);
    }
    const char* GetStringUTFChars(jstring string, jboolean* isCopy) {
      const char* chars = _jni->GetStringUTFChars(string, isCopy);
      if (chars == NULL) {
	throw exception();
      }
      return chars;
    }
    void ReleaseStringUTFChars(jstring string, const char* chars) {
      _jni->ReleaseStringUTFChars(string, chars);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetStringRegion(jstring string, jsize start, jsize len, jchar *buf) {
      _jni->GetStringRegion(string, start, len, buf);
      if (_jni->ExceptionCheck()) {
	fprintf(stderr, "%s(%p,%d,%d,%p) failed\n",
		__func__, string, (int) start, (int) len, buf);
	throw exception();
      }
    }
    void GetStringUTFRegion(jstring string, jsize start, jsize len, char *buf) {
      _jni->GetStringUTFRegion(string, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    const jchar* GetStringCritical(jstring string) {
      return _jni->GetStringCritical(string, NULL);
    }
    void ReleaseStringCritical(jstring string, jchar* chars) {
      return _jni->ReleaseStringCritical(string, chars);
    }

    // Array Operations

    jsize GetArrayLength(jarray array) {
      return _jni->GetArrayLength(array);
    }
    jobjectArray NewObjectArray(jsize length, jclass elementType,
				jobject initialElement) {
      jobjectArray tmp = _jni->NewObjectArray(length, elementType, initialElement);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jobject GetObjectArrayElement(jobjectArray array, jsize index) {
      jobject element = _jni->GetObjectArrayElement(array, index);
      if (element == NULL) {
	throw exception();
      }
      return element;
    }
    void SetObjectArrayElement(jobjectArray array, jsize index, jobject value) {
      _jni->SetObjectArrayElement(array, index, value);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    jbooleanArray NewBooleanArray(jsize length) {
      jbooleanArray tmp = _jni->NewBooleanArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jbyteArray NewByteArray(jsize length) {
      jbyteArray tmp = _jni->NewByteArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jcharArray NewCharArray(jsize length) {
      jcharArray tmp = _jni->NewCharArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jshortArray NewShortArray(jsize length) {
      jshortArray tmp = _jni->NewShortArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jintArray NewIntArray(jsize length) {
      jintArray tmp = _jni->NewIntArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jlongArray NewLongArray(jsize length) {
      jlongArray tmp = _jni->NewLongArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jfloatArray NewFloatArray(jsize length) {
      jfloatArray tmp = _jni->NewFloatArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jdoubleArray NewDoubleArray(jsize length) {
      jdoubleArray tmp = _jni->NewDoubleArray(length);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jboolean* GetBooleanArrayElements(jbooleanArray array, jboolean *isCopy) {
      jboolean* tmp = _jni->GetBooleanArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jbyte* GetByteArrayElements(jbyteArray array, jboolean *isCopy) {
      jbyte* tmp = _jni->GetByteArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jchar* GetCharArrayElements(jcharArray array, jboolean *isCopy) {
      jchar* tmp = _jni->GetCharArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jshort* GetShortArrayElements(jshortArray array, jboolean *isCopy) {
      jshort* tmp = _jni->GetShortArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jint* GetIntArrayElements(jintArray array, jboolean *isCopy) {
      jint* tmp = _jni->GetIntArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jlong* GetLongArrayElements(jlongArray array, jboolean *isCopy) {
      jlong* tmp = _jni->GetLongArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jfloat* GetFloatArrayElements(jfloatArray array, jboolean *isCopy) {
      jfloat* tmp = _jni->GetFloatArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    jdouble* GetDoubleArrayElements(jdoubleArray array, jboolean *isCopy) {
      jdouble* tmp = _jni->GetDoubleArrayElements(array, isCopy);
      if (tmp == NULL)
	throw exception();
      return tmp;
    }
    void ReleaseBooleanArrayElements(jbooleanArray array, jboolean* elements, jint mode) {
      _jni->ReleaseBooleanArrayElements(array, elements, mode);
    }
    void ReleaseByteArrayElements(jbyteArray array, jbyte* elements, jint mode) {
      _jni->ReleaseByteArrayElements(array, elements, mode);
    }
    void ReleaseCharArrayElements(jcharArray array, jchar* elements, jint mode) {
      _jni->ReleaseCharArrayElements(array, elements, mode);
    }
    void ReleaseShortArrayElements(jshortArray array, jshort* elements, jint mode) {
      _jni->ReleaseShortArrayElements(array, elements, mode);
    }
    void ReleaseIntArrayElements(jintArray array, jint* elements, jint mode) {
      _jni->ReleaseIntArrayElements(array, elements, mode);
    }
    void ReleaseLongArrayElements(jlongArray array, jlong* elements, jint mode) {
      _jni->ReleaseLongArrayElements(array, elements, mode);
    }
    void ReleaseFloatArrayElements(jfloatArray array, jfloat* elements, jint mode) {
      _jni->ReleaseFloatArrayElements(array, elements, mode);
    }
    void ReleaseDoubleArrayElements(jdoubleArray array, jdouble* elements, jint mode) {
      _jni->ReleaseDoubleArrayElements(array, elements, mode);
    }
    void GetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len, jboolean *buf) {
      _jni->GetBooleanArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetByteArrayRegion(jbyteArray array, jsize start, jsize len, jbyte *buf) {
      _jni->GetByteArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetCharArrayRegion(jcharArray array, jsize start, jsize len, jchar *buf) {
      _jni->GetCharArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetShortArrayRegion(jshortArray array, jsize start, jsize len, jshort *buf) {
      _jni->GetShortArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetIntArrayRegion(jintArray array, jsize start, jsize len, jint *buf) {
      _jni->GetIntArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetLongArrayRegion(jlongArray array, jsize start, jsize len, jlong *buf) {
      _jni->GetLongArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetFloatArrayRegion(jfloatArray array, jsize start, jsize len, jfloat *buf) {
      _jni->GetFloatArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void GetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len, jdouble *buf) {
      _jni->GetDoubleArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len, jboolean *buf) {
      _jni->SetBooleanArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetByteArrayRegion(jbyteArray array, jsize start, jsize len, jbyte *buf) {
      _jni->SetByteArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetCharArrayRegion(jcharArray array, jsize start, jsize len, jchar *buf) {
      _jni->SetCharArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetShortArrayRegion(jshortArray array, jsize start, jsize len, jshort *buf) {
      _jni->SetShortArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetIntArrayRegion(jintArray array, jsize start, jsize len, jint *buf) {
      _jni->SetIntArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetLongArrayRegion(jlongArray array, jsize start, jsize len, jlong *buf) {
      _jni->SetLongArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetFloatArrayRegion(jfloatArray array, jsize start, jsize len, jfloat *buf) {
      _jni->SetFloatArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void SetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len, jdouble *buf) {
      _jni->SetDoubleArrayRegion(array, start, len, buf);
      if (_jni->ExceptionCheck()) {
	throw exception();
      }
    }
    void* GetPrimitiveArrayCritical(jarray array) {
      void* elements = _jni->GetPrimitiveArrayCritical(array, NULL);
      if (elements == NULL) {
	throw exception();
      }
      return elements;
    }
    void ReleasePrimitiveArrayCritical(jarray array, void* elements, int mode) {
      _jni->ReleasePrimitiveArrayCritical(array, elements, mode);
    }

    // Registering Native Methods

    void RegisterNatives(jclass klass, JNINativeMethod* methods, jint n) {
      if (_jni->RegisterNatives(klass, methods, n) < 0) {
	throw exception();
      }
    }
    void UnregisterNatives(jclass klass) {
      if (_jni->UnregisterNatives(klass) < 0) {
	throw exception();
      }
    }

    // Monitor Operations

    void MonitorEnter(jobject monitor) {
      if (_jni->MonitorEnter(monitor) < 0) {
	throw exception();
      }
    }
    void MonitorExit(jobject monitor) {
      if (_jni->MonitorExit(monitor) < 0) {
	throw exception();
      }
    }

    // NIO Support

    jobject NewDirectByteBuffer(void* address, jlong capacity) {
      jobject buf = _jni->NewDirectByteBuffer(address, capacity);
      if (buf == NULL) {
	throw exception();
      }
    }
    void* GetDirectBufferAddress(jobject buf) {
      // Can return NULL, but not an exception?
      return _jni->GetDirectBufferAddress(buf);
    }
    jlong GetDirectBufferCapacity(jobject buf) {
      // Can return -1, but not an exception?
      return _jni->GetDirectBufferCapacity(buf);
    }

    // Reflection Support

    jmethodID FromReflectedMethod(jobject method) {
      return _jni->FromReflectedMethod(method);
    }
    jfieldID FromReflectedField(jobject field) {
      return _jni->FromReflectedField(field);
    }
    jobject ToReflectedMethod(jclass klass, jmethodID methodID) {
      // XXX: CLASSPATH takes an extra jboolean arg?
      return _jni->ToReflectedMethod(klass, methodID, 0);
    }
    jobject ToReflectedField(jclass klass, jfieldID fieldID) {
      // XXX: CLASSPATH takes an extra jboolean arg?
      return _jni->ToReflectedField(klass, fieldID, 0);
    }

    // Java VM Interface

    jint GetJavaVM(JavaVM **vm) {
      // Can return -1, but not an exception?
      return _jni->GetJavaVM(vm);
    }

  };

  /**
   * The JNIXX root, wraps the jobject pointer, all generated object
   * wrappers extend this.
   */
  class object {
  public:
    jobject _object;
    object(jobject _object) {
      this->_object = _object;
    }
    bool operator==(jobject o) {
      return _object == o;
    }
  };

  /**
   * The JNIXX array root, any array object extends this (which
   * extends jnixx::object).
   */
  template <typename Object> class array : public object {
  public:
    array(jobject _object) : object(_object) {
    }
  };
}

#endif
