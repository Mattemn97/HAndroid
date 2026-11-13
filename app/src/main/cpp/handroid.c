// Avvia un modulo Python con l'interprete CPython ufficiale per Android.
// Py_RunMain blocca finché il programma non termina, quindi va chiamato da un thread dedicato.
#include <jni.h>
#include <Python.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>

#define TAG "HAndroid"

static int failed(const char *step, PyStatus status) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s: %s %s", step,
                        status.func ? status.func : "", status.err_msg ? status.err_msg : "");
    return 1;
}

JNIEXPORT jint JNICALL
Java_it_handroid_PythonRunner_runModule(JNIEnv *env, jclass cls, jstring home, jstring site, jstring module,
                                        jobjectArray args) {
    PyConfig config;
    PyStatus status;
    PyConfig_InitPythonConfig(&config);
    // stdout e stderr finiscono già in logcat (tag python.stdout e python.stderr): lo fa CPython su Android
    config.parse_argv = 0;        // gli argomenti sono di Home Assistant, non di Python

    const char *c_home = (*env)->GetStringUTFChars(env, home, NULL);
    status = PyConfig_SetBytesString(&config, &config.home, c_home);
    (*env)->ReleaseStringUTFChars(env, home, c_home);
    if (PyStatus_Exception(status)) return failed("home", status);

    // Percorsi espliciti: stdlib, moduli nativi della stdlib e site-packages.
    // (Le variabili impostate da Kotlin con Os.setenv non arrivano a getenv qui.)
    const char *c_paths_home = (*env)->GetStringUTFChars(env, home, NULL);
    const char *subdirs[] = {"/lib/python3.14", "/lib/python3.14/lib-dynload", NULL};
    config.module_search_paths_set = 1;
    for (int i = 0; subdirs[i]; i++) {
        char path[1024];
        snprintf(path, sizeof path, "%s%s", c_paths_home, subdirs[i]);
        wchar_t *wpath = Py_DecodeLocale(path, NULL);
        status = PyWideStringList_Append(&config.module_search_paths, wpath);
        PyMem_RawFree(wpath);
        if (PyStatus_Exception(status)) return failed("module_search_paths", status);
    }
    (*env)->ReleaseStringUTFChars(env, home, c_paths_home);
    const char *c_site = (*env)->GetStringUTFChars(env, site, NULL);
    wchar_t *wsite = Py_DecodeLocale(c_site, NULL);
    (*env)->ReleaseStringUTFChars(env, site, c_site);
    status = PyWideStringList_Append(&config.module_search_paths, wsite);
    PyMem_RawFree(wsite);
    if (PyStatus_Exception(status)) return failed("site-packages", status);

    const char *c_module = (*env)->GetStringUTFChars(env, module, NULL);
    status = PyConfig_SetBytesString(&config, &config.run_module, c_module);
    (*env)->ReleaseStringUTFChars(env, module, c_module);
    if (PyStatus_Exception(status)) return failed("run_module", status);

    jsize argc = (*env)->GetArrayLength(env, args);
    char **argv = calloc(argc, sizeof(char *));
    for (jsize i = 0; i < argc; i++) {
        jstring arg = (*env)->GetObjectArrayElement(env, args, i);
        const char *c_arg = (*env)->GetStringUTFChars(env, arg, NULL);
        argv[i] = strdup(c_arg);
        (*env)->ReleaseStringUTFChars(env, arg, c_arg);
    }
    status = PyConfig_SetBytesArgv(&config, argc, argv);
    for (jsize i = 0; i < argc; i++) free(argv[i]);
    free(argv);
    if (PyStatus_Exception(status)) return failed("argv", status);

    status = Py_InitializeFromConfig(&config);
    PyConfig_Clear(&config);
    if (PyStatus_Exception(status)) return failed("init", status);

    return Py_RunMain();
}
