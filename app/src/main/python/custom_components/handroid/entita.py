"""Base comune: un dispositivo HA per ogni add-on, entità aggiunte quando compaiono nel catalogo."""

from homeassistant.helpers.device_registry import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

DOMAIN = "handroid"


class EntitaAddon(CoordinatorEntity):
    _attr_has_entity_name = True

    def __init__(self, sup, slug, tipo):
        super().__init__(sup)
        self.slug = slug
        self._attr_unique_id = f"addon_{slug}_{tipo}"
        a = sup.addons()[slug]
        self._attr_device_info = DeviceInfo(
            identifiers={(DOMAIN, slug)}, name=a["name"], manufacturer="HAndroid",
            model="Add-on " + a["type"], sw_version=a.get("version"))

    @property
    def addon(self):
        return self.coordinator.addons().get(self.slug, {})

    @property
    def available(self):
        return super().available and bool(self.addon)


def aggiungi_nuovi(sup, async_add_entities, crea, filtro=lambda a: True):
    """Aggiunge le entità degli add-on nuovi a ogni aggiornamento del supervisore."""
    visti = set()

    def controlla():
        nuovi = [s for s, a in sup.addons().items() if s not in visti and filtro(a)]
        visti.update(nuovi)
        if nuovi:
            async_add_entities([crea(sup, s) for s in nuovi])

    controlla()
    return sup.async_add_listener(controlla)
