"""Un sensore di stato per ogni add-on del catalogo: non_installato, fermo, avviato, errore."""

from homeassistant.components.sensor import SensorDeviceClass, SensorEntity

from .entita import DOMAIN, EntitaAddon, aggiungi_nuovi

STATI = ["non_installato", "fermo", "avviato", "errore"]


async def async_setup_entry(hass, entry, async_add_entities):
    sup = hass.data[DOMAIN][entry.entry_id]
    entry.async_on_unload(aggiungi_nuovi(sup, async_add_entities, StatoAddon))


class StatoAddon(EntitaAddon, SensorEntity):
    _attr_device_class = SensorDeviceClass.ENUM
    _attr_options = STATI
    _attr_name = "Stato"
    _attr_icon = "mdi:puzzle"

    def __init__(self, sup, slug):
        super().__init__(sup, slug, "stato")

    @property
    def native_value(self):
        return self.addon.get("stato")

    @property
    def extra_state_attributes(self):
        a = self.addon
        return {k: a.get(k) for k in ("slug", "version", "disponibile", "description", "ports", "processo",
                                      "opzioni_attuali", "avvio_auto", "repository", "codice_uscita")}
