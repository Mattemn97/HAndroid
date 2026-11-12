// Avvia CPython ufficiale dentro il processo dell'app ed esegue un modulo.
// Py_RunMain blocca finché il modulo non termina; poi l'interprete è finalizzato
// e non si può riavviare: il servizio chiude il processo e Android lo riapre.
#include <jni.h>
#include <Python.h>
#include <android/log.h>

#define TAG "HAndroid"

static int check(PyStatus st, const char *step) {
    if (PyStatus_Exception(st)) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "%s: %s", step, st.err_msg ? st.err_msg : "?");
        return 0;
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_it_handroid_PythonRuntime_runModule(JNIEnv *env, jobject thiz, jstring jhome,
                                         jobjectArray jpaths, jstring jmodule) {
    PyConfig config;
    PyConfig_InitPythonConfig(&config);
    config.install_signal_handlers = 0;
    config.module_search_paths_set = 1;

    const char *home = (*env)->GetStringUTFChars(env, jhome, NULL);
    const char *module = (*env)->GetStringUTFChars(env, jmodule, NULL);
    int ok = check(PyConfig_SetBytesString(&config, &config.home, home), "home")
          && check(PyConfig_SetBytesString(&config, &config.run_module, module), "run_module");

    jsize n = (*env)->GetArrayLength(env, jpaths);
    for (jsize i = 0; ok && i < n; i++) {
        jstring jp = (jstring) (*env)->GetObjectArrayElement(env, jpaths, i);
        const char *p = (*env)->GetStringUTFChars(env, jp, NULL);
        wchar_t *w = Py_DecodeLocale(p, NULL);
        ok = w && check(PyWideStringList_Append(&config.module_search_paths, w), "path");
        PyMem_RawFree(w);
        (*env)->ReleaseStringUTFChars(env, jp, p);
    }
    (*env)->ReleaseStringUTFChars(env, jhome, home);
    (*env)->ReleaseStringUTFChars(env, jmodule, module);

    if (ok) ok = check(Py_InitializeFromConfig(&config), "init");
    PyConfig_Clear(&config);
    if (!ok) return -1;
    return Py_RunMain();
}
