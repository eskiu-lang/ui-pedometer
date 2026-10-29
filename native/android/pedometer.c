// <pedometer> on Android: the externs Eskiu calls, into EskiuPedometer (Java) through JNI.
#include <jni.h>
#include "ui_native.h"

static jclass pedometer_class(JNIEnv *env) { return ui_find_class("org.eskiu.pedometer.EskiuPedometer"); }

static void call0(const char *method) {
    JNIEnv *env = ui_jni_env();
    jclass type = pedometer_class(env);
    if (!type) return;
    (*env)->CallStaticVoidMethod(env, type, (*env)->GetStaticMethodID(env, type, method, "()V"));
    (*env)->DeleteLocalRef(env, type);
}

void pedometer_start(void) { call0("start"); }

void pedometer_stop(void) { call0("stop"); }

void pedometer_sample(void) { call0("sample"); }

void pedometer_query(int id, double from_ms, double to_ms) {
    JNIEnv *env = ui_jni_env();
    jclass type = pedometer_class(env);
    if (!type) return;
    jmethodID method = (*env)->GetStaticMethodID(env, type, "query", "(IJJ)V");
    (*env)->CallStaticVoidMethod(env, type, method, (jint)id, (jlong)from_ms, (jlong)to_ms);
    (*env)->DeleteLocalRef(env, type);
}
