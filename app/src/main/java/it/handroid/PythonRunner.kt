package it.handroid

object PythonRunner {
    init {
        System.loadLibrary("handroid")
    }

    /** Esegue `python -m module` con sys.argv = args. Blocca fino alla fine; si chiama una volta per processo. */
    @JvmStatic
    external fun runModule(home: String, sitePackages: String, module: String, args: Array<String>): Int
}
