// Eseguibile "python" per i processi figli (pip, build dei pacchetti, add-on Python).
// Android esegue programmi solo dalla cartella delle librerie native dell'APK: per questo viene
// compilato come libhpython.so da prepara-python-android.sh e messo in jniLibs.
// PYTHONHOME e PYTHONPATH li imposta handroid_boot.py nell'ambiente dei figli.
#include <Python.h>
#include <dlfcn.h>
#include <libgen.h>
#include <limits.h>
#include <unistd.h>

int main(int argc, char **argv) {
    // I moduli della stdlib (_ssl, _hashlib, _sqlite3) dipendono da queste librerie, che stanno accanto
    // all'eseguibile. Il linker non le cerca lì (LD_LIBRARY_PATH non basta): si caricano prima, globali.
    char exe[PATH_MAX];
    ssize_t n = readlink("/proc/self/exe", exe, sizeof exe - 1);
    if (n > 0) {
        exe[n] = 0;
        const char *dir = dirname(exe);
        const char *libs[] = {"libcrypto_python.so", "libssl_python.so", "libsqlite3_python.so", NULL};
        for (int i = 0; libs[i]; i++) {
            char path[PATH_MAX];
            snprintf(path, sizeof path, "%s/%s", dir, libs[i]);
            dlopen(path, RTLD_NOW | RTLD_GLOBAL);
        }
    }
    return Py_BytesMain(argc, argv);
}
