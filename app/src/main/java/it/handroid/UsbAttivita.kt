package it.handroid

import android.app.Activity
import android.os.Bundle

/**
 * Attività invisibile aperta da Android quando si collega una chiavetta del filtro
 * (`usb_device_filter.xml`). Con "Usa sempre per questo dispositivo" il permesso resta;
 * qui basta svegliare il servizio, che nel processo ":ha" ricontrolla le chiavette.
 */
class UsbAttivita : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HaService.avvia(this, ricontrollaUsb = true)
        finish()
    }
}
