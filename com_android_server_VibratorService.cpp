/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "VibratorService"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <utils/misc.h>
#include <utils/Log.h>
#include <hardware_legacy/vibrator.h>

#include <stdio.h>
#include <android/log.h>
#include <stdlib.h>

namespace android
{

static jboolean vibratorExists(JNIEnv *env, jobject clazz)
{
    return vibrator_exists() > 0 ? JNI_TRUE : JNI_FALSE;
}

static void vibratorOn(JNIEnv *env, jobject clazz, jlong timeout_ms)
{
    // ALOGI("vibratorOn\n");
    vibrator_on(timeout_ms);
}

static void vibratorOff(JNIEnv *env, jobject clazz)
{
    // ALOGI("vibratorOff\n");
    vibrator_off();
}



static void myexecuteCommand(JNIEnv* env, jobject clazz, jstring jcmd)
{
    //ALOGD();
	__android_log_print(ANDROID_LOG_INFO, "622", "INSIDE native function");
	const char *cmd = NULL;
    cmd = jcmd ? env->GetStringUTFChars(jcmd, NULL) : NULL; 
    //mCommand(cmd);
	
	if(NULL == cmd) {
        jniThrowNullPointerException(env, "cmd");
        //ALOGD("622 - Looks like JNI string passing is not working");
        __android_log_print(ANDROID_LOG_INFO, "622", "NULL value passed!!!");
		return;
    }   
    //else
    //{
		//ALOGD("622 - Native Code, cmd passed= %s",cmd);
    //	__android_log_print(ANDROID_LOG_INFO, "622", "Command passed = %s", cmd);
	//}
	//char writeToFile[200];
    //snprintf(writeToFile, sizeof(writeToFile), "echo %s", cmd);
    int ret = system(cmd);
	//int ret1 = system("ip rule ls");
	__android_log_print(ANDROID_LOG_INFO, "622", "Command executed = %s", cmd);
	__android_log_print(ANDROID_LOG_INFO, "622", "system() return value = %d", ret);
    env->ReleaseStringUTFChars(jcmd, cmd);

}


static JNINativeMethod method_table[] = {
    { "vibratorExists", "()Z", (void*)vibratorExists },
    { "vibratorOn", "(J)V", (void*)vibratorOn },
    { "vibratorOff", "()V", (void*)vibratorOff },
	{ "myexecuteCommand", "(Ljava/lang/String;)V", (void*)myexecuteCommand },
};

int register_android_server_VibratorService(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "com/android/server/VibratorService",
            method_table, NELEM(method_table));
}

};
